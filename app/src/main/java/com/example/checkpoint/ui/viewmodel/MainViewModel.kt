package com.example.checkpoint.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.AuthRepositoryImpl
import com.example.checkpoint.data.repository.MainRepository
import com.example.checkpoint.data.repository.MainRepositoryImpl
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserPreferencesRepositoryImpl
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.UserStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainViewModel(
    private val application: Application,
    private val mainRepository: MainRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sortOrder = MutableStateFlow(PostSortOrder.NEWEST)

    private var searchJob: Job? = null
    private var followJob: Job? = null
    private var chatJob: Job? = null
    private var commentJob: Job? = null
    private var periodicSyncJob: Job? = null

    // Stato di caricamento iniziale della sessione
    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // Factory per creare il MainViewModel
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)

                val authRepository = AuthRepositoryImpl(database.userDao())
                val preferencesRepository = UserPreferencesRepositoryImpl(application)
                val mainRepository = MainRepositoryImpl(
                    database.postDao(), 
                    database.userDao(),
                    database.chatDao(),
                    database.commentDao(),
                    database // Passaggio dell'istanza DB
                )

                return MainViewModel(
                    application = application,
                    mainRepository = mainRepository,
                    authRepository = authRepository,
                    preferencesRepository = preferencesRepository
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
        observeCurrentUserFollows() // Osserva follower/following dell'utente loggato
        observeChatPartners()       // Osserva le chat attive
    }

    // Gestione reattiva dei partner di chat
    private fun observeChatPartners() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) {
                    mainRepository.getChatPartners(email).collectLatest { partners ->
                        _uiState.update { it.copy(chatPartners = partners) }
                    }
                }
            }
        }
    }

    // Osserva le relazioni di follow dell'utente corrente
    private fun observeCurrentUserFollows() {
        viewModelScope.launch {
            preferencesRepository.userEmail.collectLatest { email ->
                if (email != null) {
                    launch {
                        mainRepository.getFollowers(email).collectLatest { followers ->
                            _uiState.update { it.copy(followersList = followers) }
                        }
                    }
                    launch {
                        mainRepository.getFollowing(email).collectLatest { following ->
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
                mainRepository.getTrendingTags().collectLatest { tags ->
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
                
                // User Search
                launch {
                    mainRepository.searchUsersByUsername(query).collectLatest { users ->
                        _uiState.update { it.copy(searchResultsUsers = users) }
                    }
                }
                
                // Posts search
                launch {
                    mainRepository.searchPostsByTitle(query, email).collectLatest { posts ->
                        _uiState.update { it.copy(searchResultsPosts = posts) }
                    }
                }
                
                // Tags search
                launch {
                    mainRepository.searchUniqueTags(query).collectLatest { tags ->
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
            mainRepository.searchPostsByTag(tag, email).collectLatest { posts ->
                _uiState.update { it.copy(tagResults = posts) }
            }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            try {
                mainRepository.getAllUniqueTags().collectLatest { tags ->
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
                            // Avvia sincronizzazione automatica e real-time
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
                        var user = authRepository.getUser(email)
                        
                        // Se l'utente non è in Room, si prova a recuperarlo da Firebase e crearlo
                        if (user == null) {
                            val currentEmail = authRepository.getCurrentUserEmail()
                            if (currentEmail == email) {
                                // loggato su Firebase
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
                        val updatedUser = currentUser.copy(
                            username = newUsername,
                            bio = newBio,
                            profilePictureUri = newProfilePictureUri
                        )
                        authRepository.updateUser(updatedUser)
                        _uiState.update { 
                            it.copy(
                                username = newUsername,
                                bio = newBio,
                                profilePictureUri = newProfilePictureUri,
                                errorMessage = null
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error updating profile: ${e.message}") }
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
                    val newPost = PostEntity(
                        authorEmail = email,
                        title = title,
                        initialMessage = message,
                        mediaUri = mediaUri,
                        mediaType = mediaType
                    )
                    mainRepository.createPost(newPost, tags)
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
                mainRepository.deletePost(postId)
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
                        mainRepository.getAllPosts(email).collectLatest { result ->
                            val sortedList = when (sortOrder) {
                                PostSortOrder.NEWEST -> result.sortedByDescending { it.post.timestamp }
                                PostSortOrder.TRENDING -> result.sortedByDescending { it.favoriteCount }
                            }
                            _uiState.update {
                                it.copy(
                                    posts = sortedList,
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
                // Mostra il caricamento per feedback visivo
                _uiState.update { it.copy(isLoading = true) }
                
                // Piccolo delay simulato
                delay(600.milliseconds)
                
                _sortOrder.value = newOrder
                // Il caricamento viene rimosso appena viene caricata la nuova lista
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error sorting posts: ${e.message}") }
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
                    mainRepository.toggleLike(postId, email)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error liking post: ${e.message}") }
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
                mainRepository.syncDataFromFirestore(email)

                val isGranted = ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                refreshUserLocation(isGranted)
            }
            loadUserProfile()
            delay(1000.milliseconds)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun startPeriodicSync(email: String) {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            mainRepository.syncDataFromFirestore(email)
            
            while (true) {
                delay(60.seconds)
                mainRepository.syncHighPriorityData(email)
            }
        }
        mainRepository.startRealtimeSync(email)
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        mainRepository.stopRealtimeSync()
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicSync()
    }

    /**
     * Helper per verificare l'esistenza dell'utente durante le interazioni.
     * Restituisce true se l'utente esiste, false se è stato fatto il logout forzato.
     */
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
                mainRepository.clearLocalData()
                preferencesRepository.clearSession()
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

    // Gestione apertura popup profilo
    fun onOpenUserPopup(user: UserEntity) {
        val currentEmail = _uiState.value.currentUserEmail
        if (user.email != currentEmail && currentEmail != null) {
            _uiState.update { it.copy(selectedUserForPopup = user) }
            observeFollowStatus(currentEmail, user.email)
        }
    }

    // Avvia l'osservazione dello stato di follow per l'utente visualizzato nel popup
    private fun observeFollowStatus(followerEmail: String, followedEmail: String) {
        followJob?.cancel()
        followJob = viewModelScope.launch {
            // Osserva se lo seguiamo
            launch {
                mainRepository.isFollowing(followerEmail, followedEmail).collectLatest { isFollowing ->
                    _uiState.update { it.copy(isFollowingSelectedUser = isFollowing) }
                }
            }
            // Conteggio follower in tempo reale
            launch {
                mainRepository.getFollowersCount(followedEmail).collectLatest { count ->
                    _uiState.update { it.copy(selectedUserFollowersCount = count) }
                }
            }
            // Conteggio seguiti in tempo reale
            launch {
                mainRepository.getFollowingCount(followedEmail).collectLatest { count ->
                    _uiState.update { it.copy(selectedUserFollowingCount = count) }
                }
            }
        }
    }

    // Alterna lo stato di follow (Follow/Unfollow)
    fun onToggleFollow() {
        val followedUser = _uiState.value.selectedUserForPopup ?: return
        val followerEmail = _uiState.value.currentUserEmail ?: return
        
        viewModelScope.launch {
            try {
                mainRepository.toggleFollow(followerEmail, followedUser.email)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Follow failed: ${e.message}") }
            }
        }
    }

    // Logica Chat: apre la chat con un utente
    fun onOpenChat(partner: UserEntity) {
        val currentEmail = _uiState.value.currentUserEmail ?: return
        _uiState.update { it.copy(selectedChatPartner = partner) }
        
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            mainRepository.getChatMessages(currentEmail, partner.email).collectLatest { messages ->
                _uiState.update { it.copy(currentChatMessages = messages) }
            }
        }
    }

    // Invia un nuovo messaggio diretto
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
                mainRepository.sendMessage(message)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to send message: ${e.message}") }
            }
        }
    }

    // Chiude la chat e pulisce lo stato
    fun onCloseChat() {
        chatJob?.cancel()
        _uiState.update { it.copy(selectedChatPartner = null, currentChatMessages = emptyList()) }
    }

    // Logica Commenti: apre il dettaglio del post
    fun onOpenPostDetail(postWithAuthor: PostWithAuthor) {
        _uiState.update { it.copy(selectedPost = postWithAuthor) }
        
        commentJob?.cancel()
        commentJob = viewModelScope.launch {
            mainRepository.getCommentsForPost(postWithAuthor.post.id).collectLatest { comments ->
                _uiState.update { it.copy(currentPostComments = comments) }
            }
        }
    }

    // Aggiunge un commento o una risposta (parentCommentId) a un post
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
                mainRepository.addComment(comment)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add comment: ${e.message}") }
            }
        }
    }

    // Chiude il dettaglio post
    fun onClosePostDetail() {
        commentJob?.cancel()
        _uiState.update { it.copy(selectedPost = null, currentPostComments = emptyList()) }
    }

    // Chiusura popup profilo
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
                mainRepository.updateUserLocation(email, latitude, longitude)
                val users = mainRepository.findNearbyUsers(
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

    /**
     * Aggiorna o rimuove la posizione dell'utente in base ai permessi.
     */
    fun refreshUserLocation(isPermissionGranted: Boolean) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            if (isPermissionGranted) {
                try {
                    val client = LocationServices.getFusedLocationProviderClient(application)
                    // Controllo di sicurezza per Android
                    if (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        
                        val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                        if (location != null) {
                            mainRepository.updateUserLocation(email, location.latitude, location.longitude)
                            Log.d("CheckPointDebug", "GPS: Posizione aggiornata all'avvio")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CheckPointDebug", "GPS: Errore aggiornamento posizione: ${e.message}")
                }
            } else {
                // Se i permessi sono stati rimossi, pulisce i dati online
                mainRepository.deleteUserLocation(email)
                Log.d("CheckPointDebug", "GPS: Posizione rimossa (permessi negati)")
            }
        }
    }
}
