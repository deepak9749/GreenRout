package com.example.domain.model

import androidx.annotation.StringRes
import com.example.R

enum class AvailabilityStatus(@StringRes val labelRes: Int) {
    AVAILABLE(R.string.available_status),
    BOOKED(R.string.booked),
    PICKED_UP(R.string.status_picked_up),
    EXPIRED(R.string.expired_status),
    REMOVED(R.string.removed_status);

    companion object {
        fun fromString(value: String?): AvailabilityStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AVAILABLE
        }
    }
}
