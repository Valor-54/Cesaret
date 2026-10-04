package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarPadDarkColorScheme = darkColorScheme(
    primary = TacticalAmber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332005),
    onPrimaryContainer = TacticalAmberLight,
    secondary = ScopeCyan,
    onSecondary = Color.Black,
    tertiary = CombatRed,
    background = StealthBackground,
    onBackground = TextPrimary,
    surface = StealthSurface,
    onSurface = TextPrimary,
    surfaceVariant = StealthCard,
    onSurfaceVariant = TextSecondary,
    outline = StealthBorder
)

@Composable
fun WarPadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WarPadDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WarPadTheme(content = content)
}
