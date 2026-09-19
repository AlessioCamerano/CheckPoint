package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Tipi di media supportati nei post.
 */
enum class MediaType {
    IMAGE, VIDEO, NONE
}

/**
 * Entità Post normalizzata
 */
@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["authorEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["authorEmail"])]
)
data class PostEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorEmail: String = "",
    val title: String = "",
    val initialMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val mediaUri: String? = null,
    val mediaType: MediaType = MediaType.NONE,
    val isMuted: Boolean = false
)
