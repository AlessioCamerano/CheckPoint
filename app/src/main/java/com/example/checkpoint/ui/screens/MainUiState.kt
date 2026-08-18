package com.example.checkpoint.ui.screens

import androidx.compose.ui.graphics.Color

enum class UserStatus(val color: Color, val label: String) {
    Online(Color.Green, "Online"),
    Busy(Color.Red, "Busy"),
    Offline(Color.Gray, "Offline")
}

data class MainUiState(
    val data: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userStatus: UserStatus = UserStatus.Online
)
