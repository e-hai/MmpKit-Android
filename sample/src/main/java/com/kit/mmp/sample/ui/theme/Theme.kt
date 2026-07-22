package com.kit.mmp.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = Slate950,
    secondary = MistMuted,
    onSecondary = Slate950,
    tertiary = Coral,
    background = Slate950,
    onBackground = Mist,
    surface = Slate900,
    onSurface = Mist,
    surfaceVariant = Slate800,
    onSurfaceVariant = MistMuted,
)

private val LightColorScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    secondary = Slate800,
    onSecondary = Color.White,
    tertiary = Coral,
    background = Color(0xFFF4F7FB),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFE6EDF5),
    onSurfaceVariant = Color(0xFF4B5A6D),
)

@Composable
fun MmpKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
