package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO per la gestione dei messaggi diretti.
 */
@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // Recupera la cronologia messaggi tra due utenti
    @Query("""
        SELECT * FROM messages 
        WHERE (senderEmail = :user1 AND receiverEmail = :user2) 
           OR (senderEmail = :user2 AND receiverEmail = :user1)
        ORDER BY timestamp ASC
    """)
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>

    // Recupera l'elenco degli utenti con cui si ha una chat attiva
    @Query("""
        SELECT DISTINCT u.* FROM users u
        INNER JOIN messages m ON (u.email = m.senderEmail OR u.email = m.receiverEmail)
        WHERE (m.senderEmail = :currentUserEmail OR m.receiverEmail = :currentUserEmail)
          AND u.email != :currentUserEmail
    """)
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>

    // Segna i messaggi come letti
    @Query("UPDATE messages SET isRead = 1 WHERE receiverEmail = :currentUserEmail AND senderEmail = :otherUserEmail")
    suspend fun markAsRead(currentUserEmail: String, otherUserEmail: String)

    // Verifica se ci sono nuovi messaggi non letti
    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE receiverEmail = :currentUserEmail AND senderEmail = :otherUserEmail AND isRead = 0)")
    fun hasUnreadMessages(currentUserEmail: String, otherUserEmail: String): Flow<Boolean>
}
