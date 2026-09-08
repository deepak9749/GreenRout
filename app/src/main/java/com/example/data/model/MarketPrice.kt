package com.example.data.model

data class MarketPrice(
    val id: String = "",
    val wasteType: String = "",
    val approximatePrice: String = "",
    val unit: String = "kg",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "wasteType" to wasteType,
        "approximatePrice" to approximatePrice,
        "unit" to unit,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): MarketPrice {
            return MarketPrice(
                id = id,
                wasteType = map["wasteType"] as? String ?: "",
                approximatePrice = map["approximatePrice"] as? String ?: "",
                unit = map["unit"] as? String ?: "kg",
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
