package com.example.data.model

import com.example.domain.model.UserRole

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.HOUSEHOLD,
    val isDemoUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to id,
            "name" to name,
            "email" to email,
            "role" to role.name,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
        if (isDemoUser) {
            map["isDemoUser"] = true
        }
        return map
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): User {
            return User(
                id = id,
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                role = UserRole.fromString(map["role"] as? String),
                isDemoUser = map["isDemoUser"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
