package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimarySleek,
    secondary = SecondarySleek,
    tertiary = AccentOrange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = OnDarkBackground,
    onSurface = OnDarkSurface,
    onPrimary = OnDarkSurface,
    onSecondary = OnDarkSurface,
    onTertiary = OnDarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimarySleek,
    secondary = SecondarySleek,
    tertiary = AccentOrange,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = OnLightBackground,
    onSurface = OnLightSurface,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onTertiary = LightSurface
)

@Composable
fun KaziTVTheme(
    darkTheme: Boolean = true, // Standardizes on dark theme by default, highly premium
    dynamicColor: Boolean = false, // Force our custom brand identity colors
    content: @Composable () -> Unit
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
