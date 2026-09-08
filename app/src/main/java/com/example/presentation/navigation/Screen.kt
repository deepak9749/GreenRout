package com.example.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object RoleSelection : Screen("role_selection")
    data object HouseholdDashboard : Screen("household_dashboard")
    data object MakeAvailability : Screen("make_availability")
    data object KawadiwalaMap : Screen("kawadiwala_map")
    data object KawadiwalaRequests : Screen("kawadiwala_requests")
    data object MarketDetails : Screen("market_details")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")
    data object BookedLocationTracking : Screen("booked_location_tracking/{availabilityId}") {
        fun createRoute(availabilityId: String): String = "booked_location_tracking/$availabilityId"
    }
}
