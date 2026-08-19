package com.example.checkpoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.AuthRepository
import com.example.checkpoint.data.MainRepository
import com.example.checkpoint.data.MainRepositoryImpl
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.preferences.SessionManager
import com.example.checkpoint.ui.components.MainUiState
import com.example.checkpoint.ui.components.UserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MainRepository = MainRepositoryImpl(),
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Stato di caricamento iniziale della sessione
    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkSession()
        loadData()
    }

    private fun checkSession() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collectLatest { loggedIn ->
                _isLoggedIn.value = loggedIn
                _isSessionLoaded.value = true
                if (loggedIn) {
                    loadUserProfile()
                }
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.userEmail.collectLatest { email ->
                if (!email.isNullOrBlank()) {
                    // Controllo una tantum all'avvio o al cambio email
                    val user = authRepository.getUser(email)
                    if (user != null) {
                        _uiState.update { it.copy(data = "Welcome back, ${user.username}!") }
                    } else {
                        onLogout()
                    }
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Prima di caricare i dati, verifica se l'utente esiste ancora
            if (ensureUserExists()) {
                _uiState.update { it.copy(isLoading = true) }
                repository.getStreamData().collect { result ->
                    _uiState.update {
                        it.copy(
                            data = result,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Helper per verificare l'esistenza dell'utente durante le interazioni.
     * Restituisce true se l'utente esiste, false se è stato fatto il logout forzato.
     */
    private suspend fun ensureUserExists(): Boolean {
        val email = authRepository.userEmail.first()
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
            authRepository.clearSession()
        }
    }

    fun onStatusChange(newStatus: UserStatus) {
        _uiState.update { it.copy(userStatus = newStatus) }
    }

    // Factory per creare il MainViewModel
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                val sessionManager = SessionManager(application)
                val authRepository = AuthRepository(database.userDao(), sessionManager)
                return MainViewModel(authRepository = authRepository) as T
            }
        }
    }
}
