package com.example.checkpoint.ui.state

import androidx.compose.ui.graphics.Color
import com.example.checkpoint.data.local.entity.Post

enum class UserStatus(val color: Color, val label: String) {
    Online(Color.Green, "Online"),
    Busy(Color.Red, "Busy"),
    Offline(Color.Gray, "Offline")
}

data class MainUiState(
    val username: String = "Username",
    val bio: String = "",
    val profilePictureUri: String? = null,
    val data: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userStatus: UserStatus = UserStatus.Online,
    val posts: List<Post> = emptyList(),
    val isDarkTheme: Boolean = true
)
