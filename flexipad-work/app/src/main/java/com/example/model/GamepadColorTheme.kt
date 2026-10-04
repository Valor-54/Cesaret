package com.example.model

data class GamepadColorTheme(
    val name: String,
    val primaryColorHex: Long,
    val backgroundColorHex: Long,
    val textColorHex: Long,
    val borderColorHex: Long,
    val defaultOpacity: Float = 0.75f
)

object PresetColorThemes {
    val THEMES = listOf(
        GamepadColorTheme(
            name = "Turuncu (Taktik)",
            primaryColorHex = 0xFFFF9800,
            backgroundColorHex = 0xFF16191D,
            textColorHex = 0xFFFFFFFF,
            borderColorHex = 0xFFFF9800,
            defaultOpacity = 0.75f
        ),
        GamepadColorTheme(
            name = "Kırmızı (Savaş)",
            primaryColorHex = 0xFFF44336,
            backgroundColorHex = 0xFF200E0E,
            textColorHex = 0xFFFFFFFF,
            borderColorHex = 0xFFFF5252,
            defaultOpacity = 0.80f
        ),
        GamepadColorTheme(
            name = "Mavi (Hava)",
            primaryColorHex = 0xFF2196F3,
            backgroundColorHex = 0xFF0D1B2A,
            textColorHex = 0xFFE0F7FA,
            borderColorHex = 0xFF64B5F6,
            defaultOpacity = 0.75f
        ),
        GamepadColorTheme(
            name = "Yeşil (Kamuflaj)",
            primaryColorHex = 0xFF4CAF50,
            backgroundColorHex = 0xFF101D11,
            textColorHex = 0xFFE8F5E9,
            borderColorHex = 0xFF81C784,
            defaultOpacity = 0.75f
        ),
        GamepadColorTheme(
            name = "Mor (Cyberpunk)",
            primaryColorHex = 0xFFAB47BC,
            backgroundColorHex = 0xFF1E1026,
            textColorHex = 0xFFF3E5F5,
            borderColorHex = 0xFFCE93D8,
            defaultOpacity = 0.80f
        ),
        GamepadColorTheme(
            name = "Siyah (Stealth)",
            primaryColorHex = 0xFF424242,
            backgroundColorHex = 0xFF0A0D11,
            textColorHex = 0xFFEEEEEE,
            borderColorHex = 0xFF616161,
            defaultOpacity = 0.85f
        ),
        GamepadColorTheme(
            name = "Neon (Esports)",
            primaryColorHex = 0xFF00E676,
            backgroundColorHex = 0xFF002411,
            textColorHex = 0xFF00FF7F,
            borderColorHex = 0xFF00E676,
            defaultOpacity = 0.85f
        ),
        GamepadColorTheme(
            name = "Şeffaf (Gizli HUD)",
            primaryColorHex = 0xFFB0BEC5,
            backgroundColorHex = 0x22000000,
            textColorHex = 0xCCFFFFFF,
            borderColorHex = 0x66FFFFFF,
            defaultOpacity = 0.35f
        )
    )

    val COLOR_PALETTE = listOf(
        0xFFFF9800L, // Tactical Orange
        0xFFF44336L, // Combat Red
        0xFFE91E63L, // Pink
        0xFF9C27B0L, // Purple
        0xFF673AB7L, // Deep Purple
        0xFF3F51B5L, // Indigo
        0xFF2196F3L, // Blue
        0xFF03A9F4L, // Light Blue
        0xFF00BCD4L, // Cyan
        0xFF009688L, // Teal
        0xFF4CAF50L, // Green
        0xFF8BC34AL, // Light Green
        0xFFCDDC39L, // Lime
        0xFFFFEB3BL, // Yellow
        0xFFFFC107L, // Amber
        0xFFFF5722L, // Deep Orange
        0xFF795548L, // Brown
        0xFF9E9E9EL, // Grey
        0xFF607D8BL, // Blue Grey
        0xFFFFFFFFL, // White
        0xFF000000L  // Black
    )
}
