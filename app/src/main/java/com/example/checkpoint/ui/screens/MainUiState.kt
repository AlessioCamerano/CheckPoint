package com.example.checkpoint.ui.screens

data class MainUiState(
    val data: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
