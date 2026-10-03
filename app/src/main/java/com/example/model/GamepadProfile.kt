package com.example.model

/**
 * Profile containing a complete virtual gamepad layout and configuration.
 */
data class GamepadProfile(
    val id: Long = 0,
    val name: String,
    val description: String,
    val isPreset: Boolean = false,
    val controls: List<GamepadControlItem> = emptyList(),
    val globalOpacity: Float = 0.70f,
    val hapticFeedback: Boolean = true,
    val leftStickDeadzone: Float = 0.04f,
    val leftStickSensitivity: Float = 1.0f,
    val leftStickMaxTravel: Float = 0.72f,
    val rightStickDeadzone: Float = 0.04f,
    val rightStickSensitivity: Float = 1.0f,
    val rightStickMaxTravel: Float = 0.72f,
    val dynamicJoystickCenter: Boolean = false,
    val inputMode: InputMode = InputMode.OVERLAY_HUD,
    val centerAimFreeZone: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
