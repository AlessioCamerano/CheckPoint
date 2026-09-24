package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Entità Room che rappresenta un "like" messo da un utente a un post.
 */
@Entity(
    tableName = "post_likes",
    primaryKeys = ["postId", "userEmail"],
    indices = [
        Index(value = ["userEmail"]),
    ],
)
data class PostLikeEntity(
    val postId: String,
    val userEmail: String,
)
