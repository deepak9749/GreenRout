package com.example.domain.model

import androidx.annotation.StringRes
import com.example.R

enum class RequestStatus(@StringRes val labelRes: Int) {
    PENDING(R.string.status_pending),
    ACCEPTED(R.string.status_accepted),
    COMPLETED(R.string.status_completed),
    REJECTED(R.string.status_rejected),
    WITHDRAWN(R.string.status_withdrawn),
    EXPIRED(R.string.status_expired),
    CANCELLED(R.string.status_cancelled);

    companion object {
        fun fromString(value: String?): RequestStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
