package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "post_tags",
    primaryKeys = ["postId", "tag"],
    indices = [
        Index(value = ["tag"]),
        Index(value = ["postId"])
    ]
)
data class PostTagEntity(
    val postId: String,
    val tag: String
)
