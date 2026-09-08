package com.example.utils

import com.google.android.gms.maps.model.LatLng

object Constants {
    // Default Demo Map Location (Rajpur Sonarpur, Kolkata)
    const val DEMO_LATITUDE = 22.4433497
    const val DEMO_LONGITUDE = 88.4154285
    const val DEFAULT_MAP_ZOOM = 15.5f
    val DEFAULT_DEMO_LOCATION = LatLng(DEMO_LATITUDE, DEMO_LONGITUDE)

    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_HOUSEHOLDS = "households"
    const val COLLECTION_KAWADIWALAS = "kawadiwalas"
    const val COLLECTION_AVAILABILITY = "waste_availability"
    const val COLLECTION_REQUESTS = "kawadiwala_requests"
    const val COLLECTION_MARKET_PRICES = "market_prices"
    const val COLLECTION_RECYCLING_COMPANIES = "recycling_companies"
    const val COLLECTION_NOTIFICATIONS = "notifications"

    // 24 Hour Availability
    const val AVAILABILITY_DURATION_MS = 24L * 60L * 60L * 1000L

    // Notification types
    const val TYPE_REQUEST_RECEIVED = "REQUEST_RECEIVED"
    const val TYPE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED"
    const val TYPE_REQUEST_REJECTED = "REQUEST_REJECTED"
    const val TYPE_AVAILABILITY_EXPIRED = "AVAILABILITY_EXPIRED"
    const val TYPE_PICKUP_COMPLETED = "PICKUP_COMPLETED"
    const val TYPE_PICKUP_CANCELLED = "PICKUP_CANCELLED"
}
