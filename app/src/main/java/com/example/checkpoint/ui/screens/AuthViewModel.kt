package com.example.checkpoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.checkpoint.data.AuthRepository
import com.example.checkpoint.data.local.AppDatabase
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.data.preferences.SessionManager
import com.example.checkpoint.ui.components.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Factory per creare l'AuthViewModel con il repository
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                val sessionManager = com.example.checkpoint.data.preferences.SessionManager(application)
                val repository = AuthRepository(database.userDao(), sessionManager)
                return AuthViewModel(repository) as T
            }
        }
    }

    fun onSignup(email: String, username: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 0. Controllo campi obbligatori
            if (email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "All fields are required") }
                return@launch
            }

            // 1. Controllo corrispondenza password
            if (password != confirmPassword) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Passwords do not match") }
                return@launch
            }

            // 2. Controllo forza password (almeno una lettera e un numero)
            val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d).+$".toRegex()
            if (!password.matches(passwordRegex)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Password must contain at least one letter and one number") }
                return@launch
            }

            // 2.5 Controllo dominio email (solo @gmail.com)
            if (!email.endsWith("@gmail.com", ignoreCase = true)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Only @gmail.com addresses are allowed") }
                return@launch
            }

            try {
                // 3. Controllo email già esistente
                if (repository.isEmailTaken(email)) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Email already taken") }
                    return@launch
                }

                // 4. Controllo username già esistente
                if (repository.isUsernameTaken(username)) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Username already taken") }
                    return@launch
                }

                val newUser = UserEntity(email, username, password)
                repository.registerUser(newUser)
                repository.saveSession(email) // Salva la sessione dopo il signup
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
                val user = repository.loginUser(identifier)
                if (user != null && user.password == password) {
                    repository.saveSession(user.email) // Salva la sessione dopo il login
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
