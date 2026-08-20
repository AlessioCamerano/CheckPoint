package com.example.checkpoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.repository.AuthRepository
import com.example.checkpoint.data.repository.UserPreferencesRepository
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.UserEntity
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
                
                val authRepository = AuthRepository(database.userDao())
                val preferencesRepository = UserPreferencesRepository(application)
                
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

            if (password != confirmPassword) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Passwords do not match") }
                return@launch
            }

            val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d).+$".toRegex()
            if (!password.matches(passwordRegex)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Password must contain at least one letter and one number") }
                return@launch
            }

            if (!email.endsWith("@gmail.com", ignoreCase = true)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Only @gmail.com addresses are allowed") }
                return@launch
            }

            try {
                if (authRepository.isEmailTaken(email)) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Email already taken") }
                    return@launch
                }

                if (authRepository.isUsernameTaken(username)) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Username already taken") }
                    return@launch
                }

                val newUser = UserEntity(email, username, password)
                authRepository.registerUser(newUser)
                preferencesRepository.saveSession(email)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onLogin(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepository.loginUser(identifier)
                if (user != null && user.password == password) {
                    preferencesRepository.saveSession(user.email)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid credentials") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onLogout() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun resetState() {
        _uiState.update { AuthUiState() }
    }
}
