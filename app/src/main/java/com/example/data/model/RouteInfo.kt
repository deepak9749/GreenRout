package com.example.data.model

import com.google.android.gms.maps.model.LatLng
import java.util.Locale

data class RouteInfo(
    val points: List<LatLng>,
    val distanceMeters: Double,
    val distanceText: String,
    val durationSeconds: Double,
    val durationText: String,
    val destinationName: String = "",
    val isRealRoadRoute: Boolean = true
) {
    companion object {
        fun formatDistance(meters: Double): String {
            return if (meters >= 1000.0) {
                String.format(Locale.US, "%.1f km", meters / 1000.0)
            } else {
                "${meters.toInt().coerceAtLeast(10)} m"
            }
        }

        fun formatDuration(seconds: Double): String {
            val totalMinutes = (seconds / 60).toInt().coerceAtLeast(1)
            return if (totalMinutes >= 60) {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins > 0) "$hours hr $mins min" else "$hours hr"
            } else {
                "$totalMinutes min"
            }
        }
    }
}
