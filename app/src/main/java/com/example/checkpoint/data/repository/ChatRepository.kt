package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.data.local.dao.ChatDao
import com.example.checkpoint.data.local.entity.MessageEntity
import com.example.checkpoint.data.local.entity.UserEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Gestione dei messaggi di chat e sincronizzazione in tempo reale.
 */
interface ChatRepository {

    // ------------------------------------------------------------------------------------------------
    // Lettura Dati Chat
    // ------------------------------------------------------------------------------------------------

    /** Restituisce l'elenco degli utenti con cui l'utente corrente ha una chat attiva. */
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>

    /** Restituisce la cronologia dei messaggi scambiati tra due utenti. */
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>


    // ------------------------------------------------------------------------------------------------
    // Invio e Sincronizzazione Messaggi
    // ------------------------------------------------------------------------------------------------

    /** Invia un messaggio salvandolo in locale e inviandolo a Firestore. */
    suspend fun sendMessage(message: MessageEntity)

    /** Sincronizza i messaggi inviati e ricevuti da Firestore salvandoli in Room. */
    suspend fun syncMessages(currentUserEmail: String)

    /** Avvia l'ascolto in tempo reale dei messaggi da Firestore. */
    fun startRealtimeSync(currentUserEmail: String)

    /** Interrompe l'ascolto in tempo reale. */
    fun stopRealtimeSync()
}

/**
 * Implementazione concreta di [ChatRepository] integrata con Room, Firestore e notifiche.
 */
class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ChatRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messagesListener: ListenerRegistration? = null


    // ------------------------------------------------------------------------------------------------
    // Lettura Dati Chat
    // ------------------------------------------------------------------------------------------------

    override fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>> =
        chatDao.getChatPartners(currentUserEmail)

    override fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>> =
        chatDao.getChatMessages(user1, user2)


    // ------------------------------------------------------------------------------------------------
    // Invio e Sincronizzazione Messaggi
    // ------------------------------------------------------------------------------------------------

    override suspend fun sendMessage(message: MessageEntity) {
        try {
            firestore.collection("messages").document(message.id).set(message).await()
            chatDao.insertMessage(message)
            if (message.receiverEmail.isNotBlank() &&
                message.receiverEmail != "global" &&
                message.receiverEmail != message.senderEmail
            ) {
                notificationRepository.createNotification(
                    recipientEmail = message.receiverEmail,
                    title = "Nuovo messaggio",
                    body = message.content,
                    route = "chat",
                    type = "message",
                    senderEmail = message.senderEmail,
                    targetId = message.senderEmail,
                )
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore invio messaggio: ${e.message}")
        }
    }

    override suspend fun syncMessages(currentUserEmail: String) {
        try {
            val receivedSnapshot = firestore.collection("messages")
                .whereIn("receiverEmail", listOf(currentUserEmail, "global"))
                .get()
                .await()

            val sentSnapshot = firestore.collection("messages")
                .whereEqualTo("senderEmail", currentUserEmail)
                .get()
                .await()

            val allMessages = receivedSnapshot.toObjects(MessageEntity::class.java) +
                sentSnapshot.toObjects(MessageEntity::class.java)

            chatDao.insertMessages(allMessages)
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Sync Messages fail: ${e.message}")
        }
    }

    override fun startRealtimeSync(currentUserEmail: String) {
        stopRealtimeSync()
        messagesListener = firestore.collection("messages").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            snapshot?.documentChanges?.forEach { change ->
                val msg = change.document.toObject(MessageEntity::class.java)
                if (msg.receiverEmail == currentUserEmail ||
                    msg.senderEmail == currentUserEmail ||
                    msg.receiverEmail == "global"
                ) {
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED) {
                            chatDao.insertMessage(msg)
                        }
                    }
                }
            }
        }
    }

    override fun stopRealtimeSync() {
        messagesListener?.remove()
    }
}
