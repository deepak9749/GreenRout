package com.example.data.model

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "INFO"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "message" to message,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "type" to type
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AppNotification {
            return AppNotification(
                id = id,
                userId = map["userId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isRead = map["isRead"] as? Boolean ?: false,
                type = map["type"] as? String ?: "INFO"
            )
        }
    }
}
