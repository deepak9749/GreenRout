package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateTimeFormatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatDateTime(timestamp: Long): String {
        return try {
            dateTimeFormatter.format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatTime(timestamp: Long): String {
        return try {
            timeFormatter.format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatRemainingTime(expiresAt: Long): String {
        val remainingMs = expiresAt - System.currentTimeMillis()
        if (remainingMs <= 0) return "Expired"
        val hours = remainingMs / (1000 * 60 * 60)
        val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
