package com.example.checkpoint.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.checkpoint.data.local.entity.CommentEntity
import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Modello di relazione Room che combina un commento con i dati del suo autore.
 */
data class CommentWithAuthor(
    @Embedded val comment: CommentEntity,
    @Relation(
        parentColumn = "authorEmail",
        entityColumn = "email",
    )
    val author: UserEntity? = null,
) {
    /**
     * Restituisce il profilo dell'autore o un profilo di ripiego (fallback) se l'utente non è memorizzato in locale.
     */
    val safeAuthor: UserEntity
        get() = author ?: UserEntity(email = comment.authorEmail)
}
