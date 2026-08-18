package com.example.checkpoint.data.model

data class Post(
    val id: String,
    val authorName: String,
    val title: String,
    val initialMessage: String,
    val isFavorite: Boolean = false,
    val favoriteCount: Int = 0,
    val timeAgo: String
)
