package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "post_tags",
    primaryKeys = ["postId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tag"])] // Indice per ricerche veloci per tag
)
data class PostTagEntity(
    val postId: String,
    val tag: String
)
