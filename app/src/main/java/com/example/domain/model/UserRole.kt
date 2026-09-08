package com.example.domain.model

enum class UserRole {
    HOUSEHOLD,
    KAWADIWALA;

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.uppercase()) {
                "KAWADIWALA" -> KAWADIWALA
                else -> HOUSEHOLD
            }
        }
    }
}
