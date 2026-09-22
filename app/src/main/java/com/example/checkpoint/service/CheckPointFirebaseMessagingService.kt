package com.example.checkpoint.service

import android.util.Log
import com.example.checkpoint.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CheckPointFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "CheckPoint"
        val body = message.notification?.body ?: message.data["body"] ?: "Nuova notifica"
        val route = message.data["route"] ?: "home"

        Log.d("CheckPointFCM", "FCM message received: $title - $body")
        NotificationHelper.showSystemNotification(this, title, body, route)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("CheckPointFCM", "New FCM token: $token")
    }
}
