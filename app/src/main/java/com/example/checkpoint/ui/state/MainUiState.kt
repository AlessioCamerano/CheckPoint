package com.example.checkpoint.ui.state

import androidx.compose.ui.graphics.Color
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.model.PostWithAuthor

enum class UserStatus(val color: Color, val label: String) {
    Online(Color.Green, "Online"),
    Busy(Color.Red, "Busy"),
    Offline(Color.Gray, "Offline")
}

enum class PostSortOrder {
    NEWEST, TRENDING
}

data class MainUiState(
    val username: String = "Username",
    val bio: String = "",
    val profilePictureUri: String? = null,
    val data: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val postSuccess: Boolean = false,
    val errorMessage: String? = null,
    val userStatus: UserStatus = UserStatus.Online,
    val posts: List<PostWithAuthor> = emptyList(),
    val sortOrder: PostSortOrder = PostSortOrder.NEWEST,
    val isDarkTheme: Boolean = true
)
