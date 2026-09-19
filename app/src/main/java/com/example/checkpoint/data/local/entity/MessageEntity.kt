package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Rappresenta un messaggio diretto tra due utenti.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["senderEmail"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["receiverEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
