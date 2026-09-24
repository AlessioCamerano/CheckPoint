package com.example.checkpoint.data.repository

import android.util.Log
import com.example.checkpoint.ui.state.AppNotification
import com.example.checkpoint.util.TimeUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Gestione della creazione, eliminazione e osservazione in tempo reale delle notifiche utente.
 */
interface NotificationRepository {

    // ------------------------------------------------------------------------------------------------
    // Creazione e Gestione Notifiche
    // ------------------------------------------------------------------------------------------------

    /** Crea e invia una nuova notifica a un utente destinatario. */
    suspend fun createNotification(
        recipientEmail: String,
        title: String,
        body: String,
        route: String = "home",
        type: String = "general",
        senderEmail: String? = null,
        targetId: String? = null,
    )

    /** Elimina una notifica specifica tramite ID. */
    suspend fun deleteNotification(notificationId: String)

    /** Segna una notifica come letta. */
    suspend fun markNotificationAsRead(notificationId: String)

    /** Elimina tutte le notifiche di un utente. */
    suspend fun clearAllNotifications(currentUserEmail: String)


    // ------------------------------------------------------------------------------------------------
    // Osservazione Notifiche
    // ------------------------------------------------------------------------------------------------

    /** Osserva il flusso in tempo reale delle notifiche destinate all'utente corrente. */
    fun observeNotifications(currentUserEmail: String): Flow<List<AppNotification>>
}

/**
 * Implementazione concreta di [NotificationRepository] con Cloud Firestore.
 */
class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : NotificationRepository {

    // ------------------------------------------------------------------------------------------------
    // Creazione e Gestione Notifiche
    // ------------------------------------------------------------------------------------------------

    override suspend fun createNotification(
        recipientEmail: String,
        title: String,
        body: String,
        route: String,
        type: String,
        senderEmail: String?,
        targetId: String?,
    ) {
        if (recipientEmail.isBlank()) return
        try {
            val docRef = firestore.collection("notifications").document()
            val notificationData = hashMapOf(
                "id" to docRef.id,
                "recipientEmail" to recipientEmail,
                "title" to title,
                "body" to body,
                "route" to route,
                "type" to type,
                "senderEmail" to senderEmail,
                "targetId" to targetId,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
            )
            docRef.set(notificationData).await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error creating notification: ${e.message}", e)
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        if (notificationId.isBlank()) return
        try {
            firestore.collection("notifications").document(notificationId).delete().await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error deleting notification: ${e.message}", e)
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        try {
            firestore.collection("notifications").document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error marking notification as read: ${e.message}", e)
        }
    }

    override suspend fun clearAllNotifications(currentUserEmail: String) {
        if (currentUserEmail.isBlank()) return
        try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("recipientEmail", currentUserEmail)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("CheckPointDebug", "Error clearing notifications: ${e.message}", e)
        }
    }


    // ------------------------------------------------------------------------------------------------
    // Osservazione Notifiche
    // ------------------------------------------------------------------------------------------------

    override fun observeNotifications(currentUserEmail: String): Flow<List<AppNotification>> = callbackFlow {
        if (currentUserEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientEmail", currentUserEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    AppNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        route = doc.getString("route") ?: "home",
                        time = TimeUtils.getTimeAgo(doc.getLong("timestamp") ?: 0L),
                        type = doc.getString("type") ?: "general",
                        senderEmail = doc.getString("senderEmail"),
                        targetId = doc.getString("targetId"),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false,
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }
}
