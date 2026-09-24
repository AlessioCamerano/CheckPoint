package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entità Room che rappresenta un profilo utente.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"]),
    ],
)
data class UserEntity(
    @PrimaryKey val email: String = "",
    val username: String = "",
    val bio: String = "",
    val profilePictureUri: String? = null,
)
