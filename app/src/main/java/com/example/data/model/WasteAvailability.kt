package com.example.data.model

import com.example.domain.model.AvailabilityStatus

data class WasteAvailability(
    val id: String = "",
    val householdId: String = "",
    val householdName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val wasteTypes: List<String> = emptyList(),
    val perWasteQuantity: Map<String, Double> = emptyMap(),
    val photoUrl: String? = null,
    val locationType: String = "CURRENT", // "CURRENT" or "CUSTOM"
    val availableAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = availableAt + (24L * 60L * 60L * 1000L), // Strictly 24 hours
    val status: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
    val acceptedKawadiwalaId: String? = null,
    val pickedUpAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt || status == AvailabilityStatus.EXPIRED

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "householdId" to householdId,
            "householdName" to householdName,
            "lat" to lat,
            "lng" to lng,
            "wasteTypes" to wasteTypes,
            "perWasteQuantity" to perWasteQuantity,
            "photoUrl" to photoUrl,
            "locationType" to locationType,
            "availableAt" to availableAt,
            "expiresAt" to expiresAt,
            "status" to status.name,
            "acceptedKawadiwalaId" to acceptedKawadiwalaId,
            "pickedUpAt" to pickedUpAt,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): WasteAvailability {
            val availableAtVal = (map["availableAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val expiresAtVal = (map["expiresAt"] as? Number)?.toLong() ?: (availableAtVal + 24L * 3600L * 1000L)
            val rawStatus = AvailabilityStatus.fromString(map["status"] as? String)
            val finalStatus = if (System.currentTimeMillis() > expiresAtVal && rawStatus == AvailabilityStatus.AVAILABLE) {
                AvailabilityStatus.EXPIRED
            } else {
                rawStatus
            }

            return WasteAvailability(
                id = id,
                householdId = map["householdId"] as? String ?: "",
                householdName = map["householdName"] as? String ?: "",
                lat = (map["lat"] as? Number)?.toDouble() ?: 0.0,
                lng = (map["lng"] as? Number)?.toDouble() ?: 0.0,
                wasteTypes = (map["wasteTypes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                perWasteQuantity = ((map["perWasteQuantity"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    val key = k as? String
                    val num = (v as? Number)?.toDouble()
                    if (key != null && num != null) key to num else null
                }?.toMap()) ?: emptyMap(),
                photoUrl = map["photoUrl"] as? String,
                locationType = map["locationType"] as? String ?: "CURRENT",
                availableAt = availableAtVal,
                expiresAt = expiresAtVal,
                status = finalStatus,
                acceptedKawadiwalaId = map["acceptedKawadiwalaId"] as? String,
                pickedUpAt = (map["pickedUpAt"] as? Number)?.toLong(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: availableAtVal,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: availableAtVal
            )
        }
    }
}
