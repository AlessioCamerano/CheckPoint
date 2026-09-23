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
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.AuthRepositoryImpl
import com.example.checkpoint.data.repository.ChatRepository
import com.example.checkpoint.data.repository.ChatRepositoryImpl
import com.example.checkpoint.data.repository.NotificationRepository
import com.example.checkpoint.data.repository.NotificationRepositoryImpl
import com.example.checkpoint.data.repository.PostRepository
import com.example.checkpoint.data.repository.PostRepositoryImpl
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserPreferencesRepositoryImpl
import com.example.checkpoint.data.repository.UserRepository
import com.example.checkpoint.data.repository.UserRepositoryImpl
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.UserStatus
import com.example.checkpoint.util.MediaStorageManager
import com.example.checkpoint.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

    constructor(
        postRepository: PostRepository,
        userRepository: UserRepository,
        chatRepository: ChatRepository,
        notificationRepository: NotificationRepository,
        authRepository: AuthRepository,
        preferencesRepository: UserPreferencesRepository
    ) : this(null, postRepository, userRepository, chatRepository, notificationRepository, authRepository, preferencesRepository)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sortOrder = MutableStateFlow(PostSortOrder.NEWEST)

    private var searchJob: Job? = null
    private var followJob: Job? = null
    private var chatJob: Job? = null
    private var commentJob: Job? = null
    private var periodicSyncJob: Job? = null

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val notifiedNotificationIds = mutableSetOf<String>()
    private val localNotifications = MutableStateFlow<List<AppNotification>>(emptyList())

    private var isFirstMessageLoad = true
    private var isFirstCommentLoad = true

    val pendingNavigation = MutableStateFlow<Triple<String, String?, String?>?>(null)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)

                val authRepo = AuthRepositoryImpl(database.userDao())
                val prefRepo = UserPreferencesRepositoryImpl(application)

                val notifRepo = NotificationRepositoryImpl()
                val userRepo = UserRepositoryImpl(database.userDao(), notifRepo, database)
                val postRepo = PostRepositoryImpl(database.postDao(), database.commentDao(), notifRepo)
                val chatRepo = ChatRepositoryImpl(database.chatDao(), notifRepo)

                return MainViewModel(
                    application = application,
                    postRepository = postRepo,
                    userRepository = userRepo,
                    chatRepository = chatRepo,
                    notificationRepository = notifRepo,
                    authRepository = authRepo,
                    preferencesRepository = prefRepo
                ) as T
            }
        }
    }

    init {
        checkSession()
        observeTheme()
        observeOnboarding()
        observePosts()
        observeTags()
        observeTrendingTags()
        observeSearchHistory()
        observeCurrentUserFollows()
        observeChatPartners()
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email.isNullOrBlank()) {
                    notifiedNotificationIds.clear()
                    localNotifications.value = emptyList()
                    isFirstMessageLoad = true
                    isFirstCommentLoad = true
                    _uiState.update { it.copy(notifications = emptyList()) }
                    return@collectLatest
                }

                launch {
                    localNotifications.collectLatest { list ->
                        _uiState.update { it.copy(notifications = list) }
                    }
                }

                launch {
                    chatRepository.getAllMessagesFlow().collectLatest { _ -> }
                }

                launch {
                    postRepository.getAllCommentsFlow().collectLatest { _ -> }
                }

                launch {
                    notificationRepository.observeNotifications(email).collectLatest { remoteNotifs ->
                        localNotifications.update { current ->
                            (remoteNotifs + current).distinctBy { n -> n.id }
                        }
                        remoteNotifs.forEach { notif ->
                            if (notif.id.isNotBlank() && !notif.isRead && !notifiedNotificationIds.contains(notif.id)) {
                                notifiedNotificationIds.add(notif.id)
                                if (application != null) {
                                    NotificationHelper.showSystemNotification(
                                        application,
                                        notif.title,
                                        notif.body,
                                        notif.route,
                                        notif.targetId,
                                        notificationDocumentId = notif.id
                                    )
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
                try {
                    notificationRepository.deleteNotification(notificationId)
                } catch (e: Exception) {
                    Log.e("CheckPointDebug", "Error deleting notification: ${e.message}")
                }
            }
            when (route) {
                "chat" -> {
                    if (!targetId.isNullOrBlank()) {
                        val partner = try {
                            _uiState.value.chatPartners.firstOrNull { it.email == targetId }
                                ?: authRepository.getUser(targetId)
                                ?: UserEntity(email = targetId, username = targetId.split("@")[0])
                        } catch (e: Exception) {
                            Log.e("CheckPointDebug", "Error resolving chat partner: ${e.message}")
                            UserEntity(email = targetId, username = targetId.split("@")[0])
                        }
                        onOpenChat(partner)
                    }
                }
                "post_detail" -> {
                    if (!targetId.isNullOrBlank()) {
                        val post = try {
                            _uiState.value.posts.firstOrNull { it.post.id == targetId }
                                ?: postRepository.getPostById(targetId, _uiState.value.currentUserEmail ?: "")
                        } catch (e: Exception) {
                            Log.e("CheckPointDebug", "Error resolving post detail: ${e.message}")
                            null
                        }
                        if (post != null) {
                            onOpenPostDetail(post)
                        }
                    }
                }
            }
            pendingNavigation.value = Triple(route, targetId, notificationId)
        }
    }

    fun consumePendingNavigation() {
        pendingNavigation.value = null
    }

    fun onNotificationClicked(notification: AppNotification) {
        if (notification.id.isBlank()) return
        localNotifications.update { it.filterNot { n -> n.id == notification.id } }
        notifiedNotificationIds.remove(notification.id)
        viewModelScope.launch {
            notificationRepository.deleteNotification(notification.id)
        }
    }

    fun onNotificationClickedById(notificationId: String) {
        if (notificationId.isBlank()) return
        localNotifications.update { it.filterNot { n -> n.id == notificationId } }
        notifiedNotificationIds.remove(notificationId)
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun onClearAllNotifications() {
        val currentEmail = _uiState.value.currentUserEmail ?: ""
        localNotifications.value = emptyList()
        notifiedNotificationIds.clear()
        if (currentEmail.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.clearAllNotifications(currentEmail)
            }
        }
    }

    private fun observeChatPartners() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) {
                    chatRepository.getChatPartners(email).collectLatest { partners ->
                        _uiState.update { it.copy(chatPartners = partners) }
                    }
                }
            }
        }
    }

    private fun observeCurrentUserFollows() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) {
                    launch {
                        userRepository.getFollowers(email).collectLatest { followers ->
                            _uiState.update { it.copy(followersList = followers) }
                        }
                    }
                    launch {
                        userRepository.getFollowing(email).collectLatest { following ->
                            _uiState.update { it.copy(followingList = following) }
                        }
                    }
                }
            }
        }
    }

    private fun observeSearchHistory() {
        viewModelScope.launch {
            try {
                preferencesRepository.searchHistory.collectLatest { history ->
                    _uiState.update { it.copy(searchHistory = history) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading search history: ${e.message}") }
            }
        }
    }

    private fun observeTrendingTags() {
        viewModelScope.launch {
            try {
                postRepository.getTrendingTags().collectLatest { tags ->
                    _uiState.update { it.copy(trendingTags = tags) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading trending tags: ${e.message}") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val email = preferencesRepository.userEmail.first() ?: return@launch

                launch {
                    userRepository.searchUsersByUsername(query).collectLatest { users ->
                        _uiState.update { it.copy(searchResultsUsers = users) }
                    }
                }

                launch {
                    postRepository.searchPostsByTitle(query, email).collectLatest { posts ->
                        _uiState.update { it.copy(searchResultsPosts = posts) }
                    }
                }

                launch {
                    postRepository.searchUniqueTags(query).collectLatest { tags ->
                        _uiState.update { it.copy(searchResultsTags = tags) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Search failed: ${e.message}") }
            }
        }
    }

    fun onSaveSearchQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                preferencesRepository.saveSearchQuery(query)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save search: ${e.message}") }
            }
        }
    }

    fun onSearchByTag(tag: String) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            postRepository.searchPostsByTag(tag, email).collectLatest { posts ->
                _uiState.update { it.copy(tagResults = posts) }
            }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            try {
                postRepository.getAllUniqueTags().collectLatest { tags ->
                    _uiState.update { it.copy(availableTags = tags) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error observing tags: ${e.message}") }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            try {
                preferencesRepository.isDarkTheme.collectLatest { isDark ->
                    if (isDark != null) {
                        _uiState.update { it.copy(isDarkTheme = isDark) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading theme: ${e.message}") }
            }
        }
    }

    private fun observeOnboarding() {
        viewModelScope.launch {
            try {
                preferencesRepository.hasCompletedOnboarding.collectLatest { completed ->
                    _hasCompletedOnboarding.value = completed
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading onboarding state: ${e.message}") }
            }
        }
    }

    fun onOnboardingCompleted() {
        viewModelScope.launch {
            try {
                preferencesRepository.setOnboardingCompleted(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error saving onboarding state: ${e.message}") }
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                preferencesRepository.isLoggedIn.collectLatest { loggedIn ->
                    _isLoggedIn.value = loggedIn
                    _isSessionLoaded.value = true
                    if (loggedIn) {
                        val email = preferencesRepository.userEmail.first()
                        if (email != null) {
                            startPeriodicSync(email)
                        }
                        loadUserProfile()
                    } else {
                        stopPeriodicSync()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error checking session: ${e.message}") }
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                preferencesRepository.userEmail.collectLatest { email ->
                    if (!email.isNullOrBlank()) {
                        val previousEmail = _uiState.value.currentUserEmail
                        if (previousEmail != null && previousEmail != email) {
                            Log.d("CheckPointDebug", "Cambio utente rilevato ($previousEmail -> $email): reset dati locali Room")
                            userRepository.clearLocalData()
                        }
                        var user = authRepository.getUser(email)

                        if (user == null) {
                            val currentEmail = authRepository.getCurrentUserEmail()
                            if (currentEmail == email) {
                                val newUser = UserEntity(
                                    email = email,
                                    username = email.split("@")[0]
                                )
                                authRepository.registerUser(newUser)
                                user = newUser
                            }
                        }
                        if (user != null) {
                            _uiState.update {
                                it.copy(
                                    username = user.username,
                                    bio = user.bio,
                                    profilePictureUri = user.profilePictureUri,
                                    currentUserEmail = email,
                                    data = "Welcome back, ${user.username}!"
                                )
                            }
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Log.w("CheckPointFCM", "Unable to get FCM token", task.exception)
                                    return@addOnCompleteListener
                                }
                                val token = task.result ?: return@addOnCompleteListener
                                viewModelScope.launch {
                                    userRepository.saveFcmToken(email, token)
                                }
                            }
                        } else {
                            onLogout()
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading profile: ${e.message}") }
            }
        }
    }

    fun onUpdateProfile(newUsername: String, newBio: String, newProfilePictureUri: String?) {
        viewModelScope.launch {
            try {
                val email = preferencesRepository.userEmail.first()
                if (email != null) {
                    val currentUser = authRepository.getUser(email)
                    if (currentUser != null) {
                        _uiState.update { it.copy(isLoading = true) }

                        val processedPic = if (application != null && !newProfilePictureUri.isNullOrBlank()) {
                            MediaStorageManager.encodeToOptimizedBase64(application, newProfilePictureUri, isProfilePicture = true)
                        } else newProfilePictureUri

                        val updatedUser = currentUser.copy(
                            username = newUsername,
                            bio = newBio,
                            profilePictureUri = processedPic
                        )
                        authRepository.updateUser(updatedUser)

                        _uiState.update {
                            it.copy(
                                username = newUsername,
                                bio = newBio,
                                profilePictureUri = processedPic,
                                errorMessage = null,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error updating profile: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onCreatePost(
        title: String,
        message: String,
        mediaUri: String? = null,
        mediaType: MediaType = MediaType.NONE,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val email = preferencesRepository.userEmail.first()
                if (email != null) {

                    val processedMediaUri = if (application != null && !mediaUri.isNullOrBlank()) {
                        MediaStorageManager.encodeToOptimizedBase64(application, mediaUri, isProfilePicture = false)
                    } else null

                    val resolvedMediaType = if (!processedMediaUri.isNullOrBlank()) MediaType.IMAGE else MediaType.NONE

                    val newPost = PostEntity(
                        authorEmail = email,
                        title = title,
                        initialMessage = message,
                        mediaUri = processedMediaUri,
                        mediaType = resolvedMediaType
                    )

                    postRepository.createPost(newPost, tags)
                    _uiState.update { it.copy(isLoading = false, postSuccess = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error creating post: ${e.message}") }
            }
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(postId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error deleting post: ${e.message}") }
            }
        }
    }

    fun resetPostSuccess() {
        _uiState.update { it.copy(postSuccess = false) }
    }

    private fun observePosts() {
        viewModelScope.launch {
            try {
                combine(
                    preferencesRepository.userEmail,
                    _sortOrder
                ) { email, sortOrder ->
                    email to sortOrder
                }.collectLatest { (email, sortOrder) ->
                    if (email != null) {
                        postRepository.getAllPosts(email).collectLatest { result ->
                            val sortedList = when (sortOrder) {
                                PostSortOrder.NEWEST -> result.sortedByDescending { it.post.timestamp }
                                PostSortOrder.TRENDING -> result.sortedByDescending { it.favoriteCount }
                            }
                            _uiState.update { currentState ->
                                val updatedSelectedPost = currentState.selectedPost?.let { current ->
                                    result.firstOrNull { it.post.id == current.post.id } ?: current
                                }
                                currentState.copy(
                                    posts = sortedList,
                                    selectedPost = updatedSelectedPost,
                                    sortOrder = sortOrder,
                                    isLoading = false,
                                    isRefreshing = false
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = "Error loading data: ${e.message}") }
            }
        }
    }

    fun onSortOrderChange(newOrder: PostSortOrder) {
        if (_sortOrder.value == newOrder) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                delay(600.milliseconds)
                _sortOrder.value = newOrder
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error sorting checkpoints: ${e.message}") }
            }
        }
    }

    fun onSearchSortOrderChange(newOrder: PostSortOrder) {
        _uiState.update { it.copy(searchSortOrder = newOrder) }
    }

    fun onToggleLike(postId: String) {
        viewModelScope.launch {
            try {
                val email = preferencesRepository.userEmail.first()
                if (email != null) {
                    _uiState.update { state ->
                        val updatedPosts = state.posts.map { item ->
                            if (item.post.id == postId) {
                                val newIsLiked = !item.isLikedByCurrentUser
                                val newCount = if (newIsLiked) item.favoriteCount + 1 else (item.favoriteCount - 1).coerceAtLeast(0)
                                item.copy(isLikedByCurrentUser = newIsLiked, favoriteCount = newCount)
                            } else {
                                item
                            }
                        }
                        val updatedSelected = state.selectedPost?.let { item ->
                            if (item.post.id == postId) {
                                val newIsLiked = !item.isLikedByCurrentUser
                                val newCount = if (newIsLiked) item.favoriteCount + 1 else (item.favoriteCount - 1).coerceAtLeast(0)
                                item.copy(isLikedByCurrentUser = newIsLiked, favoriteCount = newCount)
                            } else {
                                item
                            }
                        }
                        state.copy(posts = updatedPosts, selectedPost = updatedSelected)
                    }
                    postRepository.toggleLike(postId, email)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error liking checkpoint: ${e.message}") }
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            try {
                preferencesRepository.clearSearchHistory()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error clearing history: ${e.message}") }
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val email = uiState.value.currentUserEmail
            if (email != null) {
                postRepository.syncPostsLikesAndComments()
                userRepository.syncUsersAndFollows(email)
                chatRepository.syncMessages(email)

                if (application != null) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        application,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    refreshUserLocation(isGranted)
                }
            }
            loadUserProfile()
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

    override fun onCleared() {
        super.onCleared()
        stopPeriodicSync()
    }

    private suspend fun ensureUserExists(): Boolean {
        val email = preferencesRepository.userEmail.first()
        if (email.isNullOrBlank()) return false

        val user = authRepository.getUser(email)
        return if (user == null) {
            onLogout()
            false
        } else {
            true
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            try {
                stopPeriodicSync()
                searchJob?.cancel()
                followJob?.cancel()
                chatJob?.cancel()
                commentJob?.cancel()
                _uiState.value = MainUiState(isDarkTheme = _uiState.value.isDarkTheme)
                userRepository.clearLocalData()
                preferencesRepository.clearSession()
                authRepository.logout()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error logging out: ${e.message}") }
            }
        }
    }

    fun onStatusChange(newStatus: UserStatus) {
        _uiState.update { it.copy(userStatus = newStatus) }
    }

    fun onThemeChange(isDark: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setTheme(isDark)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error changing theme: ${e.message}") }
            }
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
            launch {
                userRepository.isFollowing(followerEmail, followedEmail).collectLatest { isFollowing ->
                    _uiState.update { it.copy(isFollowingSelectedUser = isFollowing) }
                }
            }
            launch {
                userRepository.getFollowersCount(followedEmail).collectLatest { count ->
                    _uiState.update { it.copy(selectedUserFollowersCount = count) }
                }
            }
            launch {
                userRepository.getFollowingCount(followedEmail).collectLatest { count ->
                    _uiState.update { it.copy(selectedUserFollowingCount = count) }
                }
            }
        }
    }

    fun onToggleFollow() {
        val followedUser = _uiState.value.selectedUserForPopup ?: return
        val followerEmail = _uiState.value.currentUserEmail ?: return

        viewModelScope.launch {
            try {
                userRepository.toggleFollow(followerEmail, followedUser.email)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Follow failed: ${e.message}") }
            }
        }
    }

    fun onOpenChat(partner: UserEntity) {
        _uiState.update { it.copy(selectedChatPartner = partner) }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            val currentEmail = preferencesRepository.userEmail.first() ?: _uiState.value.currentUserEmail
            if (currentEmail != null) {
                chatRepository.getChatMessages(currentEmail, partner.email).collectLatest { messages ->
                    _uiState.update { it.copy(currentChatMessages = messages) }
                }
            }
        }
    }

    fun onOpenChatByEmail(partnerEmail: String) {
        if (partnerEmail.isBlank()) return
        viewModelScope.launch {
            val partner = _uiState.value.chatPartners.firstOrNull { it.email == partnerEmail }
                ?: authRepository.getUser(partnerEmail)
                ?: UserEntity(email = partnerEmail, username = partnerEmail.split("@")[0])

            onOpenChat(partner)
        }
    }

    fun onSendMessage(content: String) {
        if (content.isBlank()) return
        val currentEmail = _uiState.value.currentUserEmail ?: return
        val partnerEmail = _uiState.value.selectedChatPartner?.email ?: return

        viewModelScope.launch {
            try {
                val message = MessageEntity(
                    senderEmail = currentEmail,
                    receiverEmail = partnerEmail,
                    content = content
                )
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to send message: ${e.message}") }
            }
        }
    }

    fun onCloseChat() {
        chatJob?.cancel()
        _uiState.update { it.copy(selectedChatPartner = null, currentChatMessages = emptyList()) }
    }

    fun onOpenPostDetail(postWithAuthor: PostWithAuthor) {
        _uiState.update { it.copy(selectedPost = postWithAuthor) }

        commentJob?.cancel()
        commentJob = viewModelScope.launch {
            postRepository.getCommentsForPost(postWithAuthor.post.id).collectLatest { comments ->
                _uiState.update { it.copy(currentPostComments = comments) }
            }
        }
    }

    fun onOpenPostDetailById(postId: String) {
        if (postId.isBlank()) return
        viewModelScope.launch {
            val post = _uiState.value.posts.firstOrNull { it.post.id == postId }
                ?: postRepository.getPostById(postId, _uiState.value.currentUserEmail ?: "")

            if (post != null) {
                onOpenPostDetail(post)
            }
        }
    }

    fun onAddComment(content: String, parentCommentId: String? = null) {
        if (content.isBlank()) return
        val currentEmail = _uiState.value.currentUserEmail ?: return
        val postId = _uiState.value.selectedPost?.post?.id ?: return

        viewModelScope.launch {
            try {
                val comment = CommentEntity(
                    postId = postId,
                    authorEmail = currentEmail,
                    content = content,
                    parentCommentId = parentCommentId
                )
                postRepository.addComment(comment)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add comment: ${e.message}") }
            }
        }
    }

    fun onClosePostDetail() {
        commentJob?.cancel()
        _uiState.update { it.copy(selectedPost = null, currentPostComments = emptyList()) }
    }

    fun onDismissUserPopup() {
        followJob?.cancel()
        _uiState.update {
            it.copy(
                selectedUserForPopup = null,
                isFollowingSelectedUser = false,
                selectedUserFollowersCount = 0,
                selectedUserFollowingCount = 0
            )
        }
    }

    fun onLocalUsersRadiusChange(radiusKm: Int) {
        _uiState.update { it.copy(localUsersRadiusKm = radiusKm) }
    }

    fun onSearchNearbyUsers(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            _uiState.update { it.copy(isLocalUsersLoading = true, errorMessage = null) }
            try {
                userRepository.updateUserLocation(email, latitude, longitude)
                val users = userRepository.findNearbyUsers(
                    currentUserEmail = email,
                    latitude = latitude,
                    longitude = longitude,
                    radiusKm = _uiState.value.localUsersRadiusKm
                )
                _uiState.update { it.copy(localUsers = users, isLocalUsersLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLocalUsersLoading = false,
                        errorMessage = "Nearby users search failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshUserLocation(isPermissionGranted: Boolean) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            if (isPermissionGranted) {
                try {
                    if (application != null &&
                        PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        val locationManager = application.getSystemService(android.location.LocationManager::class.java)
                        val location = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                            ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        if (location != null) {
                            userRepository.updateUserLocation(email, location.latitude, location.longitude)
                            Log.d("CheckPointDebug", "GPS: Posizione aggiornata all'avvio")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CheckPointDebug", "GPS: Errore aggiornamento posizione: ${e.message}")
                }
            } else {
                userRepository.deleteUserLocation(email)
                Log.d("CheckPointDebug", "GPS: Posizione rimossa (permessi negati)")
            }
        }
    }
}