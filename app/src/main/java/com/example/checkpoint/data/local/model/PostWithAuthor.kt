package com.example.checkpoint.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.checkpoint.data.local.entity.PostEntity
import com.example.checkpoint.data.local.entity.PostTagEntity
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Modello di relazione Room che combina un post con l'autore, i tag associati e lo stato dei preferiti.
 */
data class PostWithAuthor(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "authorEmail",
        entityColumn = "email",
    )
    val author: UserEntity? = null,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId",
    )
    val tags: List<PostTagEntity> = emptyList(),
    val isLikedByCurrentUser: Boolean = false,
    val favoriteCount: Int = 0,
) {
    /**
     * Restituisce il profilo dell'autore o un profilo di ripiego (fallback) se l'utente non è memorizzato in locale.
     */
    val safeAuthor: UserEntity
        get() = author ?: UserEntity(email = post.authorEmail)
}
