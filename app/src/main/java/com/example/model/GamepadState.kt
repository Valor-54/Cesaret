package com.example.model

/**
 * Immutable snapshot of the real-time gamepad state.
 */
data class GamepadState(
    val leftStickX: Float = 0f, // -1.0 (left) to 1.0 (right)
    val leftStickY: Float = 0f, // -1.0 (up) to 1.0 (down)
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val l2Trigger: Float = 0f,  // 0.0 (released) to 1.0 (full pull)
    val r2Trigger: Float = 0f,  // 0.0 (released) to 1.0 (full pull)
    val pressedButtons: Set<GamepadButtonType> = emptySet(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isPressed(type: GamepadButtonType): Boolean {
        return pressedButtons.contains(type)
    }

    val isLeftStickActive: Boolean
        get() = kotlin.math.abs(leftStickX) > 0.03f || kotlin.math.abs(leftStickY) > 0.03f

    val isRightStickActive: Boolean
        get() = kotlin.math.abs(rightStickX) > 0.03f || kotlin.math.abs(rightStickY) > 0.03f
}
