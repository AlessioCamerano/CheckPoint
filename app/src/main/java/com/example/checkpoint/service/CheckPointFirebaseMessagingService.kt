package com.example.checkpoint.service

import android.util.Log
import com.example.checkpoint.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servizio Firebase Cloud Messaging (FCM) per la ricezione delle notifiche push in background e in primo piano.
 */
class CheckPointFirebaseMessagingService : FirebaseMessagingService() {

    // ------------------------------------------------------------------------------------------------
    // Ricezione Messaggi e Token FCM
    // ------------------------------------------------------------------------------------------------

    /**
     * Chiamato quando viene ricevuto un messaggio FCM remoto.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "CheckPoint"
        val body = message.notification?.body ?: message.data["body"] ?: "Nuova notifica"
        val route = message.data["route"] ?: "home"

        Log.d("CheckPointFCM", "FCM message received: $title - $body")
        NotificationHelper.showSystemNotification(this, title, body, route)
    }

    /**
     * Chiamato quando viene generato un nuovo token di registrazione FCM per il dispositivo.
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("CheckPointFCM", "New FCM token: $token")
    }
}
