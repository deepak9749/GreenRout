package com.example.data.repository

import com.example.data.model.AppNotification
import com.example.data.remote.FirebaseManager
import com.example.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class NotificationRepository {
    private val firestore: FirebaseFirestore? get() = FirebaseManager.getFirestore()
    private val notificationsMap = mutableMapOf<String, AppNotification>()
    private val notificationsFlow = MutableStateFlow<List<AppNotification>>(emptyList())

    init {
        // Safe FCM token check without blocking or crashing
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { _ -> }
        } catch (_: Exception) {}

        // Initial welcome notification
        val welcome = AppNotification(
            id = "notif_welcome",
            userId = "all",
            title = "Welcome to Green Route",
            message = "Bridging homes to a greener future through seamless recyclable waste coordination.",
            timestamp = System.currentTimeMillis()
        )
        notificationsMap[welcome.id] = welcome
        notificationsFlow.value = notificationsMap.values.toList()
    }

    fun getNotifications(userId: String): Flow<List<AppNotification>> {
        return notificationsFlow
    }

    suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: String
    ): Result<Unit> {
        val id = "notif_${UUID.randomUUID().toString().take(8)}"
        val notification = AppNotification(
            id = id,
            userId = userId,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = type
        )
        notificationsMap[id] = notification
        notificationsFlow.value = notificationsMap.values.sortedByDescending { it.timestamp }

        val firestoreInstance = firestore
        if (firestoreInstance != null) {
            try {
                firestoreInstance.collection(Constants.COLLECTION_NOTIFICATIONS)
                    .document(id)
                    .set(notification.toMap())
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    fun markAsRead(notificationId: String) {
        val n = notificationsMap[notificationId]
        if (n != null) {
            notificationsMap[notificationId] = n.copy(isRead = true)
            notificationsFlow.value = notificationsMap.values.sortedByDescending { it.timestamp }
        }
    }
}
