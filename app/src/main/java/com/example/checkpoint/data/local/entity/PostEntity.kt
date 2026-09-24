package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Tipologie di media allegabili a un post.
 */
enum class MediaType {
    IMAGE, VIDEO, NONE
}

/**
 * Entità Room che rappresenta un post pubblicato sulla piattaforma.
 */
@Entity(
    tableName = "posts",
    indices = [
        Index(value = ["authorEmail"]),
    ],
)
data class PostEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorEmail: String = "",
    val title: String = "",
    val initialMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val mediaUri: String? = null,
    val mediaType: MediaType = MediaType.NONE,
)
