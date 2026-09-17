package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Tipi di media supportati nei post.
 */
enum class MediaType {
    IMAGE, VIDEO, NONE
}

/**
 * Entità Post
 */
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorName: String = "",
    val authorEmail: String = "",
    val title: String = "",
    val initialMessage: String = "",
    val isFavorite: Boolean = false,
    val favoriteCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaUri: String? = null,
    val mediaType: MediaType = MediaType.NONE,
    val isMuted: Boolean = false
)
