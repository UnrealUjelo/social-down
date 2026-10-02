package com.socialdown.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.socialdown.app.core.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Indigo,
    tertiary = Cyan,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = androidx.compose.ui.graphics.Color(0xFFD7DDEA),
    error = androidx.compose.ui.graphics.Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF7895FF),
    onPrimary = Navy,
    secondary = androidx.compose.ui.graphics.Color(0xFFAA9BFF),
    tertiary = androidx.compose.ui.graphics.Color(0xFF55D9F7),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = androidx.compose.ui.graphics.Color(0xFF34425D),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
)

@Composable
fun SocialDownTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
