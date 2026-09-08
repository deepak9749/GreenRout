package com.example.data.model

import com.example.domain.model.RequestStatus

data class KawadiwalaRequest(
    val id: String = "",
    val wasteAvailabilityId: String = "",
    val kawadiwalaId: String = "",
    val kawadiwalaName: String = "",
    val offersPerWaste: Map<String, Double> = emptyMap(),
    val estimatedArrivalHours: Int = 1, // 1, 2, or 3
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalEstimatedOffer: Double
        get() = offersPerWaste.values.sum()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "wasteAvailabilityId" to wasteAvailabilityId,
            "kawadiwalaId" to kawadiwalaId,
            "kawadiwalaName" to kawadiwalaName,
            "offersPerWaste" to offersPerWaste,
            "estimatedArrivalHours" to estimatedArrivalHours,
            "status" to status.name,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): KawadiwalaRequest {
            return KawadiwalaRequest(
                id = id,
                wasteAvailabilityId = map["wasteAvailabilityId"] as? String ?: "",
                kawadiwalaId = map["kawadiwalaId"] as? String ?: "",
                kawadiwalaName = map["kawadiwalaName"] as? String ?: "",
                offersPerWaste = ((map["offersPerWaste"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    val key = k as? String
                    val num = (v as? Number)?.toDouble()
                    if (key != null && num != null) key to num else null
                }?.toMap()) ?: emptyMap(),
                estimatedArrivalHours = (map["estimatedArrivalHours"] as? Number)?.toInt() ?: 1,
                status = RequestStatus.fromString(map["status"] as? String),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
