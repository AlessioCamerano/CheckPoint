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
 * Repository dedicato alla logica di messaging tra utenti.
 */
interface ChatRepository {
    fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>>
    fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>>
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>
    suspend fun sendMessage(message: MessageEntity)
    suspend fun syncMessages(currentUserEmail: String)
    fun startRealtimeSync(currentUserEmail: String)
    fun stopRealtimeSync()
}

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChatRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messagesListener: ListenerRegistration? = null

    override fun getChatPartners(currentUserEmail: String): Flow<List<UserEntity>> = chatDao.getChatPartners(currentUserEmail)
    override fun getChatMessages(user1: String, user2: String): Flow<List<MessageEntity>> = chatDao.getChatMessages(user1, user2)
    override fun getAllMessagesFlow(): Flow<List<MessageEntity>> = chatDao.getAllMessagesFlow()

    override suspend fun sendMessage(message: MessageEntity) {
        try {
            firestore.collection("messages").document(message.id).set(message).await()
            chatDao.insertMessage(message)
            if (message.receiverEmail.isNotBlank() && message.receiverEmail != "global" && message.receiverEmail != message.senderEmail) {
                notificationRepository.createNotification(
                    recipientEmail = message.receiverEmail,
                    title = "Nuovo messaggio",
                    body = message.content,
                    route = "chat",
                    type = "message",
                    senderEmail = message.senderEmail,
                    targetId = message.senderEmail
                )
            }
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Errore invio messaggio: ${e.message}")
        }
    }

    override suspend fun syncMessages(currentUserEmail: String) {
        try {
            val receivedSnapshot = firestore.collection("messages").whereIn("receiverEmail", listOf(currentUserEmail, "global")).get().await()
            val sentSnapshot = firestore.collection("messages").whereEqualTo("senderEmail", currentUserEmail).get().await()
            val allMessages = receivedSnapshot.toObjects(MessageEntity::class.java) + sentSnapshot.toObjects(MessageEntity::class.java)
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
                if (msg.receiverEmail == currentUserEmail || msg.senderEmail == currentUserEmail || msg.receiverEmail == "global") {
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.ADDED) chatDao.insertMessage(msg)
                    }
                }
            }
        }
    }

    override fun stopRealtimeSync() {
        messagesListener?.remove()
    }
}