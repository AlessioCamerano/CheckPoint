package com.example.checkpoint.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.repository.*
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainViewModel(
    private val application: Application?,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var followJob: Job? = null
    private var periodicSyncJob: Job? = null

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val notifiedNotificationIds = mutableSetOf<String>()
    private val localNotifications = MutableStateFlow<List<AppNotification>>(emptyList())

    val pendingNavigation = MutableStateFlow<Triple<String, String?, String?>?>(null)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getDatabase(app)
                val authRepo = AuthRepositoryImpl(db.userDao())
                val prefRepo = UserPreferencesRepositoryImpl(app)
                val notifRepo = NotificationRepositoryImpl()
                val userRepo = UserRepositoryImpl(db.userDao(), notifRepo, db)
                val postRepo = PostRepositoryImpl(db.postDao(), db.commentDao(), notifRepo)
                val chatRepo = ChatRepositoryImpl(db.chatDao(), notifRepo)

                return MainViewModel(app, postRepo, userRepo, chatRepo, notifRepo, authRepo, prefRepo) as T
            }
        }
    }

    init {
        checkSession()
        observeTheme()
        observeOnboarding()
        observeNotifications()
    }

    private fun checkSession() {
        viewModelScope.launch {
            preferencesRepository.isLoggedIn.collectLatest { loggedIn ->
                _isLoggedIn.value = loggedIn
                _isSessionLoaded.value = true
                if (loggedIn) {
                    val email = preferencesRepository.userEmail.first()
                    if (email != null) startPeriodicSync(email)
                    loadUserSession()
                } else {
                    stopPeriodicSync()
                }
            }
        }
    }

    private fun loadUserSession() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (!email.isNullOrBlank()) {
                    var user = authRepository.getUser(email)
                    if (user == null) {
                        val currentEmail = authRepository.getCurrentUserEmail()
                        if (currentEmail == email) {
                            user = UserEntity(email = email, username = email.split("@")[0])
                            authRepository.registerUser(user)
                        }
                    }
                    if (user != null) {
                        _uiState.update { it.copy(currentUserEmail = email) }
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                viewModelScope.launch { userRepository.saveFcmToken(email, task.result) }
                            }
                        }
                    } else {
                        onLogout()
                    }
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email.isNullOrBlank()) {
                    notifiedNotificationIds.clear()
                    localNotifications.value = emptyList()
                    _uiState.update { it.copy(notifications = emptyList()) }
                    return@collectLatest
                }

                launch {
                    localNotifications.collectLatest { list -> _uiState.update { it.copy(notifications = list) } }
                }

                launch {
                    notificationRepository.observeNotifications(email).collectLatest { remoteNotifs ->
                        localNotifications.update { current -> (remoteNotifs + current).distinctBy { n -> n.id } }
                        remoteNotifs.forEach { notif ->
                            if (notif.id.isNotBlank() && !notif.isRead && !notifiedNotificationIds.contains(notif.id)) {
                                notifiedNotificationIds.add(notif.id)
                                application?.let {
                                    NotificationHelper.showSystemNotification(it, notif.title, notif.body, notif.route, notif.targetId, notificationDocumentId = notif.id)
                                }
                                notificationRepository.markNotificationAsRead(notif.id)
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleNotificationIntent(route: String, targetId: String?, notificationId: String?) {
        viewModelScope.launch {
            if (!notificationId.isNullOrBlank()) {
                try { notificationRepository.deleteNotification(notificationId) } catch (e: Exception) {}
            }
            pendingNavigation.value = Triple(route, targetId, notificationId)
        }
    }

    fun consumePendingNavigation() { pendingNavigation.value = null }

    fun onNotificationClicked(notification: AppNotification) {
        if (notification.id.isBlank()) return
        localNotifications.update { it.filterNot { n -> n.id == notification.id } }
        notifiedNotificationIds.remove(notification.id)
        viewModelScope.launch { notificationRepository.deleteNotification(notification.id) }
    }

    fun onNotificationClickedById(notificationId: String) {
        if (notificationId.isBlank()) return
        localNotifications.update { it.filterNot { n -> n.id == notificationId } }
        notifiedNotificationIds.remove(notificationId)
        viewModelScope.launch { notificationRepository.deleteNotification(notificationId) }
    }

    fun onClearAllNotifications() {
        val currentEmail = _uiState.value.currentUserEmail ?: return
        localNotifications.value = emptyList()
        notifiedNotificationIds.clear()
        viewModelScope.launch { notificationRepository.clearAllNotifications(currentEmail) }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            preferencesRepository.isDarkTheme.collectLatest { isDark ->
                if (isDark != null) _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
    }

    private fun observeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.hasCompletedOnboarding.collectLatest { completed ->
                _hasCompletedOnboarding.value = completed
            }
        }
    }

    fun onOnboardingCompleted() {
        viewModelScope.launch { preferencesRepository.setOnboardingCompleted(true) }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val email = uiState.value.currentUserEmail
            if (email != null) {
                postRepository.syncPostsLikesAndComments()
                userRepository.syncUsersAndFollows(email)
                chatRepository.syncMessages(email)
                application?.let {
                    val isGranted = ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    refreshUserLocation(isGranted)
                }
            }
            delay(1000.milliseconds)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun startPeriodicSync(email: String) {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            postRepository.syncPostsLikesAndComments()
            userRepository.syncUsersAndFollows(email)
            chatRepository.syncMessages(email)

            while (true) {
                delay(60.seconds)
                chatRepository.syncMessages(email)
                userRepository.syncUsersAndFollows(email)
            }
        }
        postRepository.startRealtimeSync()
        userRepository.startRealtimeSync(email)
        chatRepository.startRealtimeSync(email)
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        postRepository.stopRealtimeSync()
        userRepository.stopRealtimeSync()
        chatRepository.stopRealtimeSync()
    }

    fun onLogout() {
        viewModelScope.launch {
            stopPeriodicSync()
            userRepository.clearLocalData()
            preferencesRepository.clearSession()
            authRepository.logout()
            _uiState.value = MainUiState(isDarkTheme = _uiState.value.isDarkTheme)
        }
    }

    fun onOpenUserPopup(user: UserEntity) {
        val currentEmail = _uiState.value.currentUserEmail
        if (user.email != currentEmail && currentEmail != null) {
            _uiState.update { it.copy(selectedUserForPopup = user) }
            observeFollowStatus(currentEmail, user.email)
        }
    }

    private fun observeFollowStatus(followerEmail: String, followedEmail: String) {
        followJob?.cancel()
        followJob = viewModelScope.launch {
            launch { userRepository.isFollowing(followerEmail, followedEmail).collectLatest { isF -> _uiState.update { it.copy(isFollowingSelectedUser = isF) } } }
            launch { userRepository.getFollowersCount(followedEmail).collectLatest { c -> _uiState.update { it.copy(selectedUserFollowersCount = c) } } }
            launch { userRepository.getFollowingCount(followedEmail).collectLatest { c -> _uiState.update { it.copy(selectedUserFollowingCount = c) } } }
        }
    }

    fun onToggleFollow() {
        val followedUser = _uiState.value.selectedUserForPopup ?: return
        val followerEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch { userRepository.toggleFollow(followerEmail, followedUser.email) }
    }

    fun onDismissUserPopup() {
        followJob?.cancel()
        _uiState.update { it.copy(selectedUserForPopup = null, isFollowingSelectedUser = false, selectedUserFollowersCount = 0, selectedUserFollowingCount = 0) }
    }

    fun refreshUserLocation(isPermissionGranted: Boolean) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            if (isPermissionGranted) {
                try {
                    if (application != null && ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val locationManager = application.getSystemService(android.location.LocationManager::class.java)
                        val location = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        if (location != null) userRepository.updateUserLocation(email, location.latitude, location.longitude)
                    }
                } catch (e: Exception) { Log.e("CheckPointDebug", "GPS Error: ${e.message}") }
            } else {
                userRepository.deleteUserLocation(email)
            }
        }
    }
}