package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Relazione di follow tra due utenti.
 */
@Entity(
    tableName = "follows",
    primaryKeys = ["followerEmail", "followedEmail"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["followerEmail"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["followedEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FollowEntity(
    val followerEmail: String, // Utente che segue
    val followedEmail: String  // Utente seguito
)
