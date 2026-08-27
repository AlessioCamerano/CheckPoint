package com.example.checkpoint.ui.viewmodel

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
import com.example.checkpoint.data.local.entity.MediaType
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.model.PostWithAuthor
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.UserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(
    private val mainRepository: MainRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sortOrder = MutableStateFlow(PostSortOrder.NEWEST)

    private var searchJob: kotlinx.coroutines.Job? = null

    // Stato di caricamento iniziale della sessione
    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Factory per creare il MainViewModel
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)

                val authRepository = AuthRepositoryImpl(database.userDao())
                val preferencesRepository = UserPreferencesRepositoryImpl(application)
                val mainRepository = MainRepositoryImpl(database.postDao(), database.userDao())

                return MainViewModel(
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
        observePosts()
        observeTags()
        observeTrendingTags()
        observeSearchHistory()
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
                    mainRepository.searchPostsByTag(query, email).collectLatest { posts ->
                        _uiState.update { it.copy(searchResultsTags = posts) }
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

    private fun checkSession() {
        viewModelScope.launch {
            try {
                preferencesRepository.isLoggedIn.collectLatest { loggedIn ->
                    _isLoggedIn.value = loggedIn
                    _isSessionLoaded.value = true
                    if (loggedIn) {
                        loadUserProfile()
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
                        val user = authRepository.getUser(email)
                        if (user != null) {
                            _uiState.update { 
                                it.copy(
                                    username = user.username,
                                    bio = user.bio,
                                    profilePictureUri = user.profilePictureUri,
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
        isMuted: Boolean = false,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val email = preferencesRepository.userEmail.first()
                val currentUsername = _uiState.value.username
                if (email != null) {
                    val newPost = Post(
                        authorName = currentUsername,
                        authorEmail = email,
                        title = title,
                        initialMessage = message,
                        mediaUri = mediaUri,
                        mediaType = mediaType,
                        isMuted = isMuted
                    )
                    mainRepository.createPost(newPost, tags)
                    _uiState.update { it.copy(isLoading = false, postSuccess = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error creating post: ${e.message}") }
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
                                PostSortOrder.TRENDING -> result.sortedByDescending { it.post.favoriteCount }
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
                kotlinx.coroutines.delay(600.milliseconds)
                
                _sortOrder.value = newOrder
                // Il caricamento viene rimosso appena viene caricata la nuova lista
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error sorting posts: ${e.message}") }
            }
        }
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
            loadUserProfile()
            kotlinx.coroutines.delay(1000.milliseconds)
            _uiState.update { it.copy(isRefreshing = false) }
        }
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
}
