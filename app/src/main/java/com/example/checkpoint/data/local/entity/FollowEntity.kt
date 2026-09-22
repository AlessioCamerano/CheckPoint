package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Relazione di follow tra due utenti.
 */
@Entity(
    tableName = "follows",
    primaryKeys = ["followerEmail", "followedEmail"],
    indices = [
        Index(value = ["followerEmail"]),
        Index(value = ["followedEmail"])
    ]
)
data class FollowEntity(
    val followerEmail: String, // Utente che segue
    val followedEmail: String  // Utente seguito
)
