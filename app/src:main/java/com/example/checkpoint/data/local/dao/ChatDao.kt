package com.example.checkpoint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso ai dati per messaggi e conversazioni di chat.
 */
@Dao
interface ChatDao {

    // ------------------------------------------------------------------------------------------------
    // Gestione Messaggi (Inserimento)
    // ------------------------------------------------------------------------------------------------

    /** Salva o aggiorna un singolo messaggio. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /** Salva o aggiorna una lista di messaggi. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)


    // ------------------------------------------------------------------------------------------------
    // Lettura Messaggi e Chat
    // ------------------------------------------------------------------------------------------------

    /** Messaggi scambiati tra due utenti in ordine cronologico. */
    @Query(
        "SELECT * FROM messages WHERE (senderEmail = :user1 AND receiverEmail = :user2) OR (senderEmail = :user2 AND receiverEmail = :user1) ORDER BY timestamp ASC",
    )
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>

    /** Utenti con cui l'utente corrente ha una chat attiva. */
    @Query(
        "SELECT DISTINCT users.* FROM users INNER JOIN messages ON users.email = messages.senderEmail OR users.email = messages.receiverEmail WHERE (messages.senderEmail = :currentUserEmail OR messages.receiverEmail = :currentUserEmail) AND users.email != :currentUserEmail",
    )
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>
}
