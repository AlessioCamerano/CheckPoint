package com.example.checkpoint.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.repository.*
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.SearchUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getDatabase(app)
                val notifRepo = NotificationRepositoryImpl()
                return SearchViewModel(
                    postRepository = PostRepositoryImpl(db.postDao(), db.commentDao(), notifRepo),
                    userRepository = UserRepositoryImpl(db.userDao(), notifRepo, db),
                    preferencesRepository = UserPreferencesRepositoryImpl(app)
                ) as T
            }
        }
    }

    init {
        observeTags()
        observeTrendingTags()
        observeSearchHistory()
    }

    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            launch { userRepository.searchUsersByUsername(query).collectLatest { u -> _uiState.update { it.copy(searchResultsUsers = u) } } }
            launch { postRepository.searchPostsByTitle(query, email).collectLatest { p -> _uiState.update { it.copy(searchResultsPosts = p) } } }
            launch { postRepository.searchUniqueTags(query).collectLatest { t -> _uiState.update { it.copy(searchResultsTags = t) } } }
        }
    }

    fun onSaveSearchQuery(query: String) {
        if (query.isNotBlank()) viewModelScope.launch { preferencesRepository.saveSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { preferencesRepository.clearSearchHistory() }
    }

    fun onSearchSortOrderChange(newOrder: PostSortOrder) {
        _uiState.update { it.copy(searchSortOrder = newOrder) }
    }

    fun onSearchByTag(tag: String) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            postRepository.searchPostsByTag(tag, email).collectLatest { posts -> _uiState.update { it.copy(tagResults = posts) } }
        }
    }

    private fun observeTags() {
        viewModelScope.launch { postRepository.getAllUniqueTags().collectLatest { tags -> _uiState.update { it.copy(availableTags = tags) } } }
    }

    private fun observeTrendingTags() {
        viewModelScope.launch { postRepository.getTrendingTags().collectLatest { tags -> _uiState.update { it.copy(trendingTags = tags) } } }
    }

    private fun observeSearchHistory() {
        viewModelScope.launch { preferencesRepository.searchHistory.collectLatest { history -> _uiState.update { it.copy(searchHistory = history) } } }
    }
}