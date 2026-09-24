package com.example.checkpoint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.CheckPointApplication
import com.example.checkpoint.data.repository.PostRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserRepository
import com.example.checkpoint.ui.state.PostSortOrder
import com.example.checkpoint.ui.state.SearchUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel per la ricerca di post, utenti e tag con supporto alla cronologia ricerche.
 */
class SearchViewModel(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as CheckPointApplication
                return SearchViewModel(
                    postRepository = application.container.postRepository,
                    userRepository = application.container.userRepository,
                    preferencesRepository = application.container.preferencesRepository,
                ) as T
            }
        }
    }

    init {
        observeTags()
        observeTrendingTags()
        observeSearchHistory()
    }


    // ------------------------------------------------------------------------------------------------
    // Esecuzione Ricerca e Cronologia
    // ------------------------------------------------------------------------------------------------

    /** Aggiorna il testo di ricerca e recupera contemporaneamente utenti, post e tag corrispondenti. */
    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            launch { userRepository.searchUsersByUsername(query).collectLatest { u -> _uiState.update { it.copy(searchResultsUsers = u) } } }
            launch { postRepository.searchPostsByTitle(query, email).collectLatest { p -> _uiState.update { it.copy(searchResultsPosts = p) } } }
            launch { postRepository.searchUniqueTags(query).collectLatest { t -> _uiState.update { it.copy(searchResultsTags = t) } } }
        }
    }

    /** Salva la stringa di ricerca nella cronologia utente. */
    fun onSaveSearchQuery(query: String) {
        if (query.isNotBlank()) viewModelScope.launch { preferencesRepository.saveSearchQuery(query) }
    }

    /** Cancella l'intera cronologia ricerche. */
    fun clearSearchHistory() {
        viewModelScope.launch { preferencesRepository.clearSearchHistory() }
    }

    /** Cambia l'ordine di ordinamento dei risultati di ricerca. */
    fun onSearchSortOrderChange(newOrder: PostSortOrder) {
        _uiState.update { it.copy(searchSortOrder = newOrder) }
    }

    /** Cerca i post associati a uno specifico tag. */
    fun onSearchByTag(tag: String) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first() ?: return@launch
            postRepository.searchPostsByTag(tag, email).collectLatest { posts -> _uiState.update { it.copy(tagResults = posts) } }
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Osservazione Tag e Cronologia
    // ------------------------------------------------------------------------------------------------

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
