package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "post_likes",
    primaryKeys = ["postId", "userEmail"],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["userEmail"])
    ]
)
data class PostLikeEntity(
    val postId: String,
    val userEmail: String
)
