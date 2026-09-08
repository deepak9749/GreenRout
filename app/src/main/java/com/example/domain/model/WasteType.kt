package com.example.domain.model

import androidx.annotation.StringRes
import com.example.R

enum class WasteType(
    val key: String,
    @StringRes val titleRes: Int,
    val defaultUnit: String = "kg"
) {
    PLASTIC("Plastic", R.string.waste_plastic, "kg"),
    GLASS("Glass", R.string.waste_glass, "kg"),
    IRON("Iron", R.string.waste_iron, "kg"),
    METAL("Metal", R.string.waste_metal, "kg"),
    FRIDGE("Fridge", R.string.waste_fridge, "unit"),
    TV("TV", R.string.waste_tv, "unit"),
    ELECTRONICS("Electronics", R.string.waste_electronics, "unit"),
    PAPER("Paper", R.string.waste_paper, "kg"),
    OTHER("Other recyclable waste", R.string.waste_other, "kg");

    companion object {
        fun fromKey(key: String): WasteType {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER
        }
    }
}
