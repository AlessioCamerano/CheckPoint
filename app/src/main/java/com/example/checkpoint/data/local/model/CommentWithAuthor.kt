package com.example.checkpoint.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.UserEntity

data class CommentWithAuthor(
    @Embedded val comment: CommentEntity,
    @Relation(
        parentColumn = "authorEmail",
        entityColumn = "email"
    )
    val author: UserEntity? = null
) {
    val safeAuthor: UserEntity
        get() = author ?: UserEntity(email = comment.authorEmail)
}
