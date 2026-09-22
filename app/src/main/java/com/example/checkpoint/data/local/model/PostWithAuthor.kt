package com.example.checkpoint.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity

data class PostWithAuthor(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "authorEmail",
        entityColumn = "email"
    )
    val author: UserEntity? = null,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val tags: List<PostTagEntity> = emptyList(),
    
    val isLikedByCurrentUser: Boolean = false,
    val favoriteCount: Int = 0
) {
    val safeAuthor: UserEntity
        get() = author ?: UserEntity(email = post.authorEmail)
}
