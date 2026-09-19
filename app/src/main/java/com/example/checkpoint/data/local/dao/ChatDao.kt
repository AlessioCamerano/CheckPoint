package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per la gestione delle chat e messaggi.
 */
@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Recupera la cronologia dei messaggi tra due utenti.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderEmail = :user1 AND receiverEmail = :user2) 
           OR (senderEmail = :user2 AND receiverEmail = :user1)
        ORDER BY timestamp ASC
    """)
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>

    /**
     * Recupera l'elenco degli utenti con cui l'utente corrente ha scambiato messaggi.
     */
    @Query("""
        SELECT DISTINCT users.* FROM users 
        INNER JOIN messages ON users.email = messages.senderEmail OR users.email = messages.receiverEmail
        WHERE (messages.senderEmail = :currentUserEmail OR messages.receiverEmail = :currentUserEmail)
          AND users.email != :currentUserEmail
    """)
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>
}
