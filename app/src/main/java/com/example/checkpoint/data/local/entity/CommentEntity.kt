package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Rappresenta un commento su un post.
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["authorEmail"],
            onDelete = ForeignKey.CASCADE
        )
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
