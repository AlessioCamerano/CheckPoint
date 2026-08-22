package com.example.checkpoint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.MainRepository
import com.example.checkpoint.data.repository.MainRepositoryImpl
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.ui.state.MainUiState
import com.example.checkpoint.ui.state.UserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val mainRepository: MainRepository = MainRepositoryImpl(),
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
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
        observeTheme()
        loadData()
    }

    private fun observeTheme() {
        viewModelScope.launch {
            preferencesRepository.isDarkTheme.collectLatest { isDark ->
                if (isDark != null) {
                    _uiState.update { it.copy(isDarkTheme = isDark) }
                }
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            preferencesRepository.isLoggedIn.collectLatest { loggedIn ->
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
            preferencesRepository.userEmail.collectLatest { email ->
                if (!email.isNullOrBlank()) {
                    // Controllo una tantum all'avvio o al cambio email
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
        }
    }

    fun onUpdateProfile(newUsername: String, newBio: String, newProfilePictureUri: String?) {
        viewModelScope.launch {
            val email = preferencesRepository.userEmail.first()
            if (email != null) {
                val currentUser = authRepository.getUser(email)
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        username = newUsername,
                        bio = newBio,
                        profilePictureUri = newProfilePictureUri
                    )
                    try {
                        authRepository.updateUser(updatedUser)
                        _uiState.update { 
                            it.copy(
                                username = newUsername,
                                bio = newBio,
                                profilePictureUri = newProfilePictureUri
                            ) 
                        }
                    } catch (e: Exception) {
                        // Gestione errore (es. username già preso)
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
                mainRepository.getStreamData().collect { result ->
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
            preferencesRepository.clearSession()
        }
    }

    fun onStatusChange(newStatus: UserStatus) {
        _uiState.update { it.copy(userStatus = newStatus) }
    }

    fun onThemeChange(isDark: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTheme(isDark)
        }
    }

    // Factory per creare il MainViewModel
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                
                val authRepository = AuthRepository(database.userDao())
                val preferencesRepository = UserPreferencesRepository(application)
                
                return MainViewModel(authRepository = authRepository, preferencesRepository = preferencesRepository) as T
            }
        }
    }
}
