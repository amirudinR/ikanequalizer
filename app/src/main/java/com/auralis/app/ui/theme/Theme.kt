package com.auralis.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.auralis.app.data.model.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    secondary = AccentSecondary,
    onSecondary = Background,
    tertiary = AudioActive,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TrackColor,
    error = Danger,
)

// Light scheme keeps the premium identity but inverts surfaces for readability.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF4A5FD0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6B7FE0),
    tertiary = Color(0xFF1FA37A),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF14161A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161A),
    surfaceVariant = Color(0xFFE8EAEF),
    onSurfaceVariant = Color(0xFF5A616C),
    outline = Color(0xFFD0D4DC),
    error = Danger,
)

@Composable
fun AuralisTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AuralisTypography,
        content = content,
    )
}
