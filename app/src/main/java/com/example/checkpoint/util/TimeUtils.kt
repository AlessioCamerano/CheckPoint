package com.example.checkpoint.util

import android.text.format.DateUtils

/**
 * Utility per la formattazione temporale e il calcolo del tempo trascorso.
 */
object TimeUtils {

    /**
     * Converte un timestamp millisecondi in una stringa di tempo relativo (es. "2 hours ago" o "Just now").
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
                DateUtils.MINUTE_IN_MILLIS,
            ).toString()
        }
    }
}
