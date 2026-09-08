package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.local.AppThemeMode
import com.example.data.local.LanguagePreferences
import com.example.data.local.LocalThemePreferences
import com.example.presentation.navigation.GreenRouteNavGraph
import com.example.ui.theme.GreenRouteTheme

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: GreenRouteAppContainer

    override fun attachBaseContext(newBase: Context) {
        val prefs = LanguagePreferences(newBase)
        val localizedContext = LanguagePreferences.applyLocale(newBase, prefs.currentLanguageCode)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = GreenRouteAppContainer(applicationContext)

        setContent {
            val themePreferences = appContainer.themePreferences
            val themeMode by themePreferences.themeMode.collectAsState()
            val isDark = themeMode == AppThemeMode.DARK

            CompositionLocalProvider(LocalThemePreferences provides themePreferences) {
                GreenRouteTheme(darkTheme = isDark) {
                    GreenRouteNavGraph(
                        appContainer = appContainer,
                        onLanguageChanged = { _ ->
                            recreate()
                        }
                    )
                }
            }
        }
    }
}
