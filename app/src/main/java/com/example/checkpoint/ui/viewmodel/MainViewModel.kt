package com.example.checkpoint.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.CheckPointApplication
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.ChatRepository
import com.example.checkpoint.data.repository.NotificationRepository
import com.example.checkpoint.data.repository.PostRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserRepository
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel principale dell'applicazione che gestisce la sessione globale, notifiche, navigazione remota e sincronizzazione.
 */
class MainViewModel(
    private val application: Application?,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository,
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
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as CheckPointApplication
                val container = application.container
                return MainViewModel(
                    application = application,
                    postRepository = container.postRepository,
                    userRepository = container.userRepository,
                    chatRepository = container.chatRepository,
                    notificationRepository = container.notificationRepository,
                    authRepository = container.authRepository,
                    preferencesRepository = container.preferencesRepository,
                ) as T
            }
        }
    }

    init {
        checkSession()
        observeTheme()
        observeOnboarding()
        observeNotifications()
    }


    // ------------------------------------------------------------------------------------------------
    // Controllo Sessione e Inizializzazione Utente
    // ------------------------------------------------------------------------------------------------

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

    @Suppress("DEPRECATION")
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


    // ------------------------------------------------------------------------------------------------
    // Gestione Notifiche e Intent di Navigazione
    // ------------------------------------------------------------------------------------------------

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

    /** Gestisce un intent di notifica push per la navigazione verso la rotta di destinazione. */
    fun handleNotificationIntent(route: String, targetId: String?, notificationId: String?) {
        viewModelScope.launch {
            if (!notificationId.isNullOrBlank()) {
                try { notificationRepository.deleteNotification(notificationId) } catch (_: Exception) {}
            }
            pendingNavigation.value = Triple(route, targetId, notificationId)
        }
    }

    /** Consuma l'azione di navigazione pendente. */
    fun consumePendingNavigation() {
        pendingNavigation.value = null
    }

    /** Gestisce la pressione su una notifica nell'elenco. */
    fun onNotificationClicked(notification: AppNotification) {
        if (notification.id.isBlank()) return
        localNotifications.update { it.filterNot { n -> n.id == notification.id } }
        notifiedNotificationIds.remove(notification.id)
        viewModelScope.launch { notificationRepository.deleteNotification(notification.id) }
    }

    /** Gestisce la pressione su una notifica tramite ID. */
    fun onNotificationClickedById(notificationId: String) {
        if (notificationId.isBlank()) return
        localNotifications.update { it.filterNot { n -> n.id == notificationId } }
        notifiedNotificationIds.remove(notificationId)
        viewModelScope.launch { notificationRepository.deleteNotification(notificationId) }
    }

    /** Elimina tutte le notifiche dell'utente. */
    fun onClearAllNotifications() {
        val currentEmail = _uiState.value.currentUserEmail ?: return
        localNotifications.value = emptyList()
        notifiedNotificationIds.clear()
        viewModelScope.launch { notificationRepository.clearAllNotifications(currentEmail) }
    }


    // ------------------------------------------------------------------------------------------------
    // Preferenze Tema e Onboarding
    // ------------------------------------------------------------------------------------------------

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

    /** Segna l'onboarding come completato nelle preferenze. */
    fun onOnboardingCompleted() {
        viewModelScope.launch { preferencesRepository.setOnboardingCompleted(true) }
    }


    // ------------------------------------------------------------------------------------------------
    // Sincronizzazione Dati, Refresh e Posizione GPS
    // ------------------------------------------------------------------------------------------------

    /** Esegue un refresh manuale di tutti i dati dal server remoto. */
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

    /** Esegue la disconnessione e la pulizia dei dati locali. */
    fun onLogout() {
        viewModelScope.launch {
            stopPeriodicSync()
            userRepository.clearLocalData()
            preferencesRepository.clearSession()
            authRepository.logout()
            _uiState.value = MainUiState(isDarkTheme = _uiState.value.isDarkTheme)
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Popup Profilo Utente
    // ------------------------------------------------------------------------------------------------

    /** Apre il popup delle informazioni rapide dell'utente selezionato. */
    fun onOpenUserPopup(user: UserEntity) {
        val currentEmail = _uiState.value.currentUserEmail
        if (user.email != currentEmail && currentEmail != null) {
            _uiState.update { it.copy(selectedUserForPopup = user) }
            observeFollowStatus(currentEmail, user.email)
        }
    }

    /** Apre il popup informazioni utente cercando per email. */
    fun onOpenUserPopupByEmail(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            val user = authRepository.getUser(email) ?: UserEntity(email = email, username = email.split("@")[0])
            onOpenUserPopup(user)
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

    /** Alterna lo stato di follow per l'utente del popup. */
    fun onToggleFollow() {
        val followedUser = _uiState.value.selectedUserForPopup ?: return
        val followerEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch { userRepository.toggleFollow(followerEmail, followedUser.email) }
    }

    /** Chiude il popup profilo utente. */
    fun onDismissUserPopup() {
        followJob?.cancel()
        _uiState.update {
            it.copy(
                selectedUserForPopup = null,
                isFollowingSelectedUser = false,
                selectedUserFollowersCount = 0,
                selectedUserFollowingCount = 0,
            )
        }
    }

    /** Aggiorna la posizione GPS dell'utente corrente se il permesso è concesso. */
    fun refreshUserLocation(isPermissionGranted: Boolean) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            val isVisible = preferencesRepository.isLocationVisible.first()
            if (isPermissionGranted && isVisible) {
                try {
                    if (application != null && ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val locationManager = application.getSystemService(LocationManager::class.java)
                        val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (location != null) userRepository.updateUserLocation(email, location.latitude, location.longitude)
                    }
                } catch (e: Exception) {
                    Log.e("CheckPointDebug", "GPS Error: ${e.message}")
                }
            } else {
                userRepository.deleteUserLocation(email)
            }
        }
    }
}
