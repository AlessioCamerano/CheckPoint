package com.example.checkpoint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.AuthRepositoryImpl
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.repository.UserPreferencesRepositoryImpl
import com.example.checkpoint.ui.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel per la gestione dei flussi di autenticazione.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)

                val authRepository = AuthRepositoryImpl(database.userDao())
                val preferencesRepository = UserPreferencesRepositoryImpl(application)
                
                return AuthViewModel(authRepository, preferencesRepository) as T
            }
        }
    }

    fun onSignup(email: String, username: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "All fields are required") }
                return@launch
            }

            if (!email.endsWith("@gmail.com")) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Only @gmail.com addresses are allowed") }
                return@launch
            }

            if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Password must contain at least one letter and one number") }
                return@launch
            }

            if (password != confirmPassword) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Passwords do not match") }
                return@launch
            }

            try {
                // Registrazione con Firebase.
                val result = authRepository.signUpWithEmail(email, username, password)
                
                result.onSuccess { user ->
                    preferencesRepository.saveSession(user.email)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onLogin(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Firebase richiede email, se l'utente inserisce lo username si risale all'email tramite Room.
                val email = if (!identifier.contains("@")) {
                    authRepository.getUserByUsername(identifier)?.email ?: identifier
                } else {
                    identifier
                }

                val result = authRepository.signInWithEmail(email, password)
                
                result.onSuccess { user ->
                    preferencesRepository.saveSession(user.email)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid credentials: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = authRepository.signInWithGoogle(idToken)
                result.onSuccess { user ->
                    preferencesRepository.saveSession(user.email)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Google Sign-in failed: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                preferencesRepository.clearSession()
                _uiState.update { it.copy(isSuccess = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error logging out: ${e.message}") }
            }
        }
    }

    fun resetState() {
        _uiState.update { AuthUiState() }
    }
}
