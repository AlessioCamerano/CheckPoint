package com.example.checkpoint.ui.state

/**
 * Stato dell'interfaccia utente durante i flussi di autenticazione (Login e Registrazione).
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)
