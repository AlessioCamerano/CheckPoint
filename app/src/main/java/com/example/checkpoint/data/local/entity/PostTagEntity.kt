package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Entità Room che rappresenta un tag associato a un post.
 */
@Entity(
    tableName = "post_tags",
    primaryKeys = ["postId", "tag"],
    indices = [
        Index(value = ["tag"]),
    ],
)
data class PostTagEntity(
    val postId: String,
    val tag: String,
)
