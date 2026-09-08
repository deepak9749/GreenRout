package com.example.data.model

data class RecyclingCompany(
    val id: String = "",
    val companyName: String = "",
    val materials: List<String> = emptyList(),
    val description: String = "",
    val officialWebsite: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "companyName" to companyName,
        "materials" to materials,
        "description" to description,
        "officialWebsite" to officialWebsite,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): RecyclingCompany {
            return RecyclingCompany(
                id = id,
                companyName = map["companyName"] as? String ?: "",
                materials = (map["materials"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                description = map["description"] as? String ?: "",
                officialWebsite = map["officialWebsite"] as? String ?: "",
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
