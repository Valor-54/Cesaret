package com.example.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.input.GamepadInputManager
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import com.example.model.GamepadProfile

/**
 * Hosts an individual gamepad control widget (Button, Joystick, D-Pad, Trigger)
 * inside its own dedicated lightweight overlay window.
 * Ensures zero touch interference with neighboring areas or background games.
 */
@Composable
fun SingleControlHost(
    item: GamepadControlItem,
    profile: GamepadProfile,
    inputManager: GamepadInputManager
) {
    when (item.type) {
        GamepadButtonType.JOYSTICK_LEFT -> {
            VirtualJoystick(
                sizeDp = item.sizeDp,
                label = item.label,
                color = Color(item.colorHex),
                opacity = item.opacity,
                sensitivity = profile.leftStickSensitivity,
                deadzone = profile.leftStickDeadzone,
                maxTravelFactor = profile.leftStickMaxTravel,
                dynamicCenter = profile.dynamicJoystickCenter,
                isEditMode = false,
                onMove = { x, y ->
                    inputManager.updateJoystick(isLeft = true, rawX = x, rawY = y)
                },
                onTouchStart = {
                    inputManager.beginJoystickTouch(isLeft = true)
                },
                onTouchEnd = {
                    inputManager.endJoystickTouch(isLeft = true)
                },
                onClickThumb = {
                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_L3, true)
                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_L3, false)
                }
            )
        }
        GamepadButtonType.JOYSTICK_RIGHT -> {
            VirtualJoystick(
                sizeDp = item.sizeDp,
                label = item.label,
                color = Color(item.colorHex),
                opacity = item.opacity,
                sensitivity = profile.rightStickSensitivity,
                deadzone = profile.rightStickDeadzone,
                maxTravelFactor = profile.rightStickMaxTravel,
                dynamicCenter = profile.dynamicJoystickCenter,
                isEditMode = false,
                onMove = { x, y ->
                    inputManager.updateJoystick(isLeft = false, rawX = x, rawY = y)
                },
                onTouchStart = {
                    inputManager.beginJoystickTouch(isLeft = false)
                },
                onTouchEnd = {
                    inputManager.endJoystickTouch(isLeft = false)
                },
                onClickThumb = {
                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_R3, true)
                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_R3, false)
                }
            )
        }
        GamepadButtonType.DPAD -> {
            VirtualDpad(
                sizeDp = item.sizeDp,
                color = Color(item.colorHex),
                opacity = item.opacity,
                isEditMode = false,
                onDirectionChanged = { dir, pressed ->
                    inputManager.setButtonPressed(dir, pressed)
                }
            )
        }
        GamepadButtonType.BUTTON_L2, GamepadButtonType.BUTTON_R2 -> {
            VirtualTrigger(
                sizeDp = item.sizeDp,
                label = item.label,
                subLabel = item.subLabel,
                color = Color(item.colorHex),
                opacity = item.opacity,
                isEditMode = false,
                onTriggerChanged = { pressure ->
                    val isLeft = item.type == GamepadButtonType.BUTTON_L2
                    inputManager.updateTrigger(isLeft = isLeft, value = pressure)
                }
            )
        }
        else -> {
            VirtualButton(
                sizeDp = item.sizeDp,
                label = item.label,
                subLabel = item.subLabel,
                color = Color(item.colorHex),
                textColor = Color(item.textColorHex),
                borderColor = Color(item.borderColorHex),
                backgroundColor = Color(item.backgroundColorHex),
                shape = item.shape,
                fontSizeSp = item.fontSizeSp,
                cornerRadiusDp = item.cornerRadiusDp,
                opacity = item.opacity,
                isEditMode = false,
                onPressChanged = { pressed ->
                    inputManager.setCustomControlPressed(item, pressed)
                }
            )
        }
    }
}
