package com.example.model

enum class ControlZone {
    LEFT,
    RIGHT,
    CENTER
}

enum class ButtonShape {
    CIRCLE,
    ROUNDED,
    SQUARE,
    PILL
}

/**
 * Represents an individual interactive button or joystick in the on-screen layout.
 * [xPercent] and [yPercent] are normalized relative coordinates (0.0 to 1.0)
 * ensuring exact responsive adaptation to any screen size and orientation.
 */
data class GamepadControlItem(
    val id: String,
    val type: GamepadButtonType,
    val label: String,
    val subLabel: String = "",
    val xPercent: Float,
    val yPercent: Float,
    val sizeDp: Float = 60f,
    val opacity: Float = 0.70f,
    val colorHex: Long = 0xFF3DDC84,
    val textColorHex: Long = 0xFFFFFFFF,
    val borderColorHex: Long = 0xFF3DDC84,
    val backgroundColorHex: Long = 0xFF16191D,
    val shape: ButtonShape = ButtonShape.ROUNDED,
    val fontSizeSp: Float = 14f,
    val cornerRadiusDp: Float = 16f,
    val isVisible: Boolean = true,
    val zone: ControlZone = ControlZone.RIGHT,
    val customKeycode: Int = 0,
    val boundKeyId: String = "",
    val rotationDegrees: Float = 0f,
    val isLocked: Boolean = false
) {
    /**
     * Resolves the effective key definition for input injection.
     */
    fun getResolvedKeyDefinition(): InputKeyDefinition? {
        if (boundKeyId.isNotBlank()) {
            val found = KeyRegistry.findById(boundKeyId)
            if (found != null) return found
        }
        return KeyRegistry.findByGamepadType(type)
    }
}
