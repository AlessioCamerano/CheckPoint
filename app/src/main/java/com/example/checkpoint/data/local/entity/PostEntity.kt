package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entità Post
 */
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorEmail: String,
    val title: String,
    val initialMessage: String,
    val isFavorite: Boolean = false,
    val favoriteCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
