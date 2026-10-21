package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RedLight,
    onPrimary = PureWhite,
    primaryContainer = RedDark,
    onPrimaryContainer = PureWhite,
    secondary = RedPrimary,
    onSecondary = PureWhite,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = PureWhite,
    primaryContainer = RedContainer,
    onPrimaryContainer = OnRedContainer,
    secondary = RedDark,
    onSecondary = PureWhite,
    background = BackgroundLight,
    onBackground = SlateDark,
    surface = SurfaceLight,
    onSurface = SlateDark,
    surfaceVariant = CardBackground,
    onSurfaceVariant = SlateMedium,
    outline = BorderLight
)

@Composable
fun JobExpressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
