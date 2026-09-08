package com.example

import android.content.Context
import com.example.data.local.LanguagePreferences
import com.example.data.local.ThemePreferences
import com.example.data.remote.FirebaseManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.MarketRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RequestRepository
import com.example.data.repository.RouteRepository
import com.example.data.repository.WasteRepository
import com.example.utils.LocationHelper

class GreenRouteAppContainer(context: Context) {
    val languagePreferences = LanguagePreferences(context)
    val themePreferences = ThemePreferences(context)
    val locationHelper = LocationHelper(context)

    init {
        FirebaseManager.init(context)
    }

    val authRepository = AuthRepository()
    val wasteRepository = WasteRepository()
    val notificationRepository = NotificationRepository()
    val requestRepository = RequestRepository(wasteRepository, notificationRepository)
    val marketRepository = MarketRepository()
    val routeRepository = RouteRepository()
}
