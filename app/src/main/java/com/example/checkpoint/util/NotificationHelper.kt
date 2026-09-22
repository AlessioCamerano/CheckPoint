package com.example.checkpoint.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.checkpoint.MainActivity
import com.example.checkpoint.R

object NotificationHelper {
    const val EXTRA_TARGET_ROUTE = "extra_target_route"
    const val EXTRA_TARGET_ID = "extra_target_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val CHANNEL_ID = "checkpoint_notifications"
    const val CHANNEL_NAME = "CheckPoint notifications"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Notifications from CheckPoint"
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        body: String,
        route: String = "home",
        targetId: String? = null,
        notificationId: Int = System.currentTimeMillis().toInt() % Int.MAX_VALUE,
        notificationDocumentId: String? = null
    ) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_ROUTE, route)
            if (!targetId.isNullOrBlank()) {
                putExtra(EXTRA_TARGET_ID, targetId)
            }
            if (!notificationDocumentId.isNullOrBlank()) {
                putExtra(EXTRA_NOTIFICATION_ID, notificationDocumentId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }
}