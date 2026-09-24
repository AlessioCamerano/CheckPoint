package com.example.checkpoint.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entità Room che rappresenta un messaggio diretto tra due utenti.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["senderEmail"]),
        Index(value = ["receiverEmail"]),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
