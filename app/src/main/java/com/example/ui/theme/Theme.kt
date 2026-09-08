package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color(0xFF00390F),
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFA5F4AC),
    secondary = GreenSecondaryDark,
    onSecondary = Color(0xFF003A12),
    secondaryContainer = Color(0xFF1B4D20),
    tertiary = EarthTertiaryDark,
    background = NeutralDarkBackground,
    surface = NeutralDarkSurface,
    surfaceVariant = NeutralDarkSurfaceVariant,
    outline = NeutralDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = EarthTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EarthTertiaryContainer,
    onTertiaryContainer = EarthOnTertiaryContainer,
    background = NeutralLightBackground,
    surface = NeutralLightSurface,
    surfaceVariant = NeutralLightSurfaceVariant,
    outline = NeutralLightOutline
)

@Composable
fun GreenRouteTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false, // Keep consistent Green Route branding by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
