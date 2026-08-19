package com.example.checkpoint.ui.components

/**
 * Stato della UI durante i flussi di Login e Signup.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
