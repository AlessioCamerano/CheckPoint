package com.example.checkpoint.util

import android.text.format.DateUtils

/**
 * Utility per la gestione del tempo.
 */
object TimeUtils {

    /**
     * Converte un timestamp in una stringa relativa (es. "2 hours ago").
     */
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val duration = now - timestamp
        
        return if (duration < DateUtils.MINUTE_IN_MILLIS) {
            "Just now"
        } else {
            DateUtils.getRelativeTimeSpanString(
                timestamp,
                now,
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }
    }
}
