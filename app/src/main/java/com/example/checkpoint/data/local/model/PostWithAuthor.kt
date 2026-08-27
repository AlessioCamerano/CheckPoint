package com.example.checkpoint.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.checkpoint.data.local.entity.Post
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity

data class PostWithAuthor(
    @Embedded val post: Post,
    @Relation(
        parentColumn = "authorEmail",
        entityColumn = "email"
    )
    val author: UserEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val tags: List<PostTagEntity> = emptyList(),
    
    val isLikedByCurrentUser: Boolean = false
)
