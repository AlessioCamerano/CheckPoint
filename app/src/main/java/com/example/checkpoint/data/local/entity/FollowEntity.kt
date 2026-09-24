package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Entità Room che rappresenta la relazione di follow tra due utenti.
 */
@Entity(
    tableName = "follows",
    primaryKeys = ["followerEmail", "followedEmail"],
    indices = [
        Index(value = ["followedEmail"]),
    ],
)
data class FollowEntity(
    val followerEmail: String,
    val followedEmail: String,
)
