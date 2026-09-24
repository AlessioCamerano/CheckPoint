package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entità Room che rappresenta un commento associato a un post.
 */
@Entity(
    tableName = "comments",
    indices = [
        Index(value = ["postId"]),
        Index(value = ["authorEmail"])
    ]
)
data class CommentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val authorEmail: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val parentCommentId: String? = null
)
