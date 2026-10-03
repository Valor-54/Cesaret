package com.example.input

import android.content.Context
import android.util.Log
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import com.example.model.GamepadProfile
import com.example.model.GamepadState
import com.example.model.InputMode
import com.example.model.InputKeyDefinition
import com.example.service.FlexiPadAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Central input state manager.
 *
 * IMPORTANT:
 * There is one authoritative input route at a time:
 *
 * BLUETOOTH_HID -> Bluetooth HID only
 * UDP_NETWORK    -> UDP only
 * SHIZUKU_ADB    -> Shizuku/Root shell only
 * OVERLAY_HUD    -> Accessibility touch fallback only
 *
 * The UI generates DOWN/HOLD/UP state changes.
 * This class owns the persistent state and dispatches the
 * resulting state to the selected backend.
 */
class GamepadInputManager(private val context: Context) {

    private val TAG = "GamepadInputManager"

    val bluetoothDriver = BluetoothHidDriver(context)
    val udpDriver = UdpNetworkDriver()
    val adbDriver = AdbShizukuDriver(context)
    val hapticDriver = HapticDriver(context)

    private val _currentState = MutableStateFlow(GamepadState())
    val currentState: StateFlow<GamepadState> = _currentState.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<String>>(emptyList())
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private var activeProfile: GamepadProfile? = null

    /*
     * Accessibility pointer bookkeeping.
     *
     * These IDs belong only to FlexiPad's internal bookkeeping.
     * They are NOT Android MotionEvent pointer IDs.
     */
    private val controlPointerMap = ConcurrentHashMap<String, Int>()
private val joystickLastPosition = ConcurrentHashMap<String, Pair<Float, Float>>()

    @Volatile
    private var nextPointerId = 1

    private fun addLog(message: String) {
        val current = _recentLogs.value.toMutableList()

        if (current.size >= 15) {
            current.removeAt(0)
        }

        current.add(
            "[${System.currentTimeMillis() % 100000}] $message"
        )

        _recentLogs.value = current
    }

    fun setProfile(profile: GamepadProfile) {
        /*
         * Release the previous profile before changing routes.
         * This prevents a button belonging to the previous profile
         * from remaining logically pressed.
         */
        releaseAll()

        activeProfile = profile
        hapticDriver.isEnabled = profile.hapticFeedback

        when (profile.inputMode) {
            InputMode.BLUETOOTH_HID -> {
                bluetoothDriver.start()
                addLog("INPUT MODE -> BLUETOOTH HID")
            }

            InputMode.UDP_NETWORK -> {
                udpDriver.start()
                addLog("INPUT MODE -> UDP")
            }

            InputMode.SHIZUKU_ADB -> {
                adbDriver.checkStatus()
                addLog("INPUT MODE -> SHIZUKU/ADB")
            }

            InputMode.OVERLAY_HUD -> {
                addLog("INPUT MODE -> OVERLAY HUD")
            }
        }
    }

    /**
     * Updates one joystick without affecting the other joystick.
     */
    fun updateJoystick(
        isLeft: Boolean,
        rawX: Float,
        rawY: Float
    ) {
        val x = rawX.coerceIn(-1f, 1f)
        val y = rawY.coerceIn(-1f, 1f)

        val previous = _currentState.value

        val unchanged = if (isLeft) {
            previous.leftStickX == x &&
                previous.leftStickY == y
        } else {
            previous.rightStickX == x &&
                previous.rightStickY == y
        }

        if (unchanged) {
            return
        }

        val updated = if (isLeft) {
            previous.copy(
                leftStickX = x,
                leftStickY = y,
                timestamp = System.currentTimeMillis()
            )
        } else {
            previous.copy(
                rightStickX = x,
                rightStickY = y,
                timestamp = System.currentTimeMillis()
            )
        }

        _currentState.value = updated

        /*
         * HID/UDP receive the complete state.
         * This means LS and RS can be active simultaneously.
         */
        dispatchToActiveDrivers(updated)

        if (activeProfile?.inputMode == InputMode.SHIZUKU_ADB) {
            updateShizukuJoystick(isLeft, x, y)
        } else if (activeProfile?.inputMode == InputMode.OVERLAY_HUD) {
            updateAccessibilityJoystick(isLeft, x, y)
        }

    }

    /**
     * Accessibility joystick fallback.
     *
     * This is intentionally isolated from the real gamepad backends.
     * The IDs here are FlexiPad bookkeeping IDs, not Android pointer IDs.
     */
    private fun updateShizukuJoystick(isLeft: Boolean, x: Float, y: Float) {
    val controlId =
        if (isLeft) "shizuku_joystick_left" else "shizuku_joystick_right"

    val pointerId = controlPointerMap[controlId] ?: return

    val joystickType =
        if (isLeft) GamepadButtonType.JOYSTICK_LEFT
        else GamepadButtonType.JOYSTICK_RIGHT

    val item = activeProfile?.controls
        ?.firstOrNull { it.type == joystickType }
        ?: return

    val metrics = context.resources.displayMetrics
    val centerX = item.xPercent * metrics.widthPixels
    val centerY = item.yPercent * metrics.heightPixels
    val maxRadiusPx = item.sizeDp * metrics.density * 0.42f

    val targetX = centerX + x * maxRadiusPx
    val targetY = centerY + y * maxRadiusPx

    joystickLastPosition[controlId] = targetX to targetY

    adbDriver.sendRawTouchMove(pointerId, targetX, targetY)
}

fun beginJoystickTouch(isLeft: Boolean) {
    if (activeProfile?.inputMode != InputMode.SHIZUKU_ADB) return

    val joystickType =
        if (isLeft) GamepadButtonType.JOYSTICK_LEFT
        else GamepadButtonType.JOYSTICK_RIGHT

    val item = activeProfile?.controls
        ?.firstOrNull { it.type == joystickType }
        ?: return

    val controlId =
        if (isLeft) "shizuku_joystick_left"
        else "shizuku_joystick_right"

    if (controlPointerMap.containsKey(controlId)) return

    val metrics = context.resources.displayMetrics
    val centerX = item.xPercent * metrics.widthPixels
    val centerY = item.yPercent * metrics.heightPixels

    val pointerId = allocatePointerId()

    val downOk = adbDriver.sendRawTouchDown(
        pointerId,
        centerX,
        centerY
    )

    if (!downOk) {
        addLog("${if (isLeft) "LS" else "RS"} RAW TOUCH DOWN FAILED")
        return
    }

    controlPointerMap[controlId] = pointerId
    joystickLastPosition[controlId] = centerX to centerY

    addLog("${if (isLeft) "LS" else "RS"} RAW TOUCH DOWN")
}

fun endJoystickTouch(isLeft: Boolean) {
    if (activeProfile?.inputMode != InputMode.SHIZUKU_ADB) return

    val controlId =
        if (isLeft) "shizuku_joystick_left"
        else "shizuku_joystick_right"

    val pointerId = controlPointerMap.remove(controlId) ?: return

    val joystickType =
        if (isLeft) GamepadButtonType.JOYSTICK_LEFT
        else GamepadButtonType.JOYSTICK_RIGHT

    val item = activeProfile?.controls
        ?.firstOrNull { it.type == joystickType }

    val metrics = context.resources.displayMetrics

    val centerX = item?.xPercent?.times(metrics.widthPixels)
        ?: (metrics.widthPixels / 2f)

    val centerY = item?.yPercent?.times(metrics.heightPixels)
        ?: (metrics.heightPixels / 2f)

    val lastPosition = joystickLastPosition.remove(controlId)

    val upX = lastPosition?.first ?: centerX
    val upY = lastPosition?.second ?: centerY

    adbDriver.sendRawTouchUp(pointerId, upX, upY)

    addLog("${if (isLeft) "LS" else "RS"} RAW TOUCH UP")
}

private fun updateAccessibilityJoystick(
        isLeft: Boolean,
        x: Float,
        y: Float
    ) {
        if (activeProfile?.inputMode == InputMode.SHIZUKU_ADB) {
            updateShizukuJoystick(isLeft, x, y)
            return
        }
        val accessibility = FlexiPadAccessibilityService.instance
            ?: return

        val joystickType =
            if (isLeft) {
                GamepadButtonType.JOYSTICK_LEFT
            } else {
                GamepadButtonType.JOYSTICK_RIGHT
            }

        val item = activeProfile
            ?.controls
            ?.firstOrNull { it.type == joystickType }
            ?: return

        val metrics = context.resources.displayMetrics

        val centerX =
            item.xPercent * metrics.widthPixels

        val centerY =
            item.yPercent * metrics.heightPixels

        val maxRadiusPx =
            item.sizeDp *
                metrics.density *
                0.42f

        val targetX =
            centerX + x * maxRadiusPx

        val targetY =
            centerY + y * maxRadiusPx

        val controlId =
            if (isLeft) {
                "accessibility_joystick_left"
            } else {
                "accessibility_joystick_right"
            }

        val neutral =
            kotlin.math.abs(x) < 0.02f &&
                kotlin.math.abs(y) < 0.02f

        if (neutral) {
            val pointerId =
                controlPointerMap.remove(controlId)

            if (pointerId != null) {
                accessibility.injectTouchUp(
                    pointerId,
                    centerX,
                    centerY
                )

                addLog(
                    "${if (isLeft) "LS" else "RS"} TOUCH UP -> NEUTRAL"
                )
            }

            return
        }

        val existing =
            controlPointerMap[controlId]

        if (existing == null) {
            val pointerId = allocatePointerId()

            controlPointerMap[controlId] = pointerId

            accessibility.injectTouchDown(
                pointerId,
                targetX,
                targetY
            )

            addLog(
                "${if (isLeft) "LS" else "RS"} TOUCH DOWN"
            )
        } else {
            accessibility.injectTouchMove(
                existing,
                targetX,
                targetY
            )
        }
    }

    private fun allocatePointerId(): Int {
        synchronized(this) {
            repeat(32) {
                val id = nextPointerId and 31
                nextPointerId++

                if (nextPointerId >= 32) {
                    nextPointerId = 0
                }

                if (!controlPointerMap.containsValue(id)) {
                    return id
                }
            }

            return 0
        }
    }

    /**
     * Updates analog trigger state.
     */
    fun updateTrigger(
        isLeft: Boolean,
        value: Float
    ) {
        val clamped = value.coerceIn(0f, 1f)
        val previous = _currentState.value

        val oldValue =
            if (isLeft) {
                previous.l2Trigger
            } else {
                previous.r2Trigger
            }

        if (oldValue == clamped) {
            return
        }

        val updated =
            if (isLeft) {
                previous.copy(
                    l2Trigger = clamped,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                previous.copy(
                    r2Trigger = clamped,
                    timestamp = System.currentTimeMillis()
                )
            }

        _currentState.value = updated
        dispatchToActiveDrivers(updated)

        if (clamped > 0.8f && oldValue <= 0.8f) {
            hapticDriver.heavyClickFeedback()
        }

        /*
         * Trigger also exposes a digital L2/R2 state for backends
         * that use the button representation.
         */
        val triggerButton =
            if (isLeft) {
                GamepadButtonType.BUTTON_L2
            } else {
                GamepadButtonType.BUTTON_R2
            }

        val pressed = clamped > 0.5f
        val wasPressed = oldValue > 0.5f

        if (pressed != wasPressed) {
            setButtonPressed(triggerButton, pressed)
        }
    }

    /**
     * Maintains one authoritative pressed-button set.
     *
     * Repeated DOWN events while already pressed are ignored.
     * Repeated UP events while already released are ignored.
     */
    fun setButtonPressed(
        button: GamepadButtonType,
        isPressed: Boolean
    ) {
        val current =
            _currentState.value.pressedButtons.toMutableSet()

        val changed =
            if (isPressed) {
                current.add(button)
            } else {
                current.remove(button)
            }

        if (!changed) {
            return
        }

        if (isPressed) {
            hapticDriver.clickFeedback()
        }

        val updated =
            _currentState.value.copy(
                pressedButtons = current,
                timestamp = System.currentTimeMillis()
            )

        _currentState.value = updated

        /*
         * Send to exactly the selected backend.
         */
        dispatchButtonToActiveBackend(
            button,
            isPressed,
            updated
        )

        addLog(
            "BUTTON ${button.defaultLabel} -> " +
                if (isPressed) "DOWN" else "UP"
        )
    }

    /**
     * Handles custom controls.
     */
    fun setCustomControlPressed(
        item: GamepadControlItem,
        isPressed: Boolean
    ) {
        /*
         * Accessibility is ONLY the selected backend in OVERLAY_HUD.
         */
        if (activeProfile?.inputMode == InputMode.OVERLAY_HUD || activeProfile?.inputMode == InputMode.SHIZUKU_ADB) {
            injectAccessibilityButton(item, isPressed)
        }

        val definition =
            item.getResolvedKeyDefinition()

        if (definition != null) {
            if (
                definition.isGamepadInput &&
                definition.gamepadButtonType != null
            ) {
                setButtonPressed(
                    definition.gamepadButtonType,
                    isPressed
                )
            } else {
                sendKeyboardDefinition(
                    definition,
                    isPressed
                )
            }
        } else {
            /*
             * Controls without a custom key definition use
             * their GamepadButtonType directly.
             */
            setButtonPressed(
                item.type,
                isPressed
            )
        }
    }

    private fun injectAccessibilityButton(
        item: GamepadControlItem,
        isPressed: Boolean
    ) {
        val accessibility =
            FlexiPadAccessibilityService.instance
                ?: return

        val metrics =
            context.resources.displayMetrics

        val screenX =
            item.xPercent * metrics.widthPixels

        val screenY =
            item.yPercent * metrics.heightPixels

        if (isPressed) {
            /*
             * Ignore duplicate DOWN for the same control.
             */
            if (controlPointerMap.containsKey(item.id)) {
                return
            }

            val pointerId = allocatePointerId()

            controlPointerMap[item.id] = pointerId

            accessibility.injectTouchDown(
                pointerId,
                screenX,
                screenY
            )

            addLog(
                "TOUCH [${item.label}] DOWN"
            )
        } else {
            val pointerId =
                controlPointerMap.remove(item.id)

            if (pointerId != null) {
                accessibility.injectTouchUp(
                    pointerId,
                    screenX,
                    screenY
                )

                addLog(
                    "TOUCH [${item.label}] UP"
                )
            }
        }
    }

    private fun sendKeyboardDefinition(
        definition: InputKeyDefinition,
        isPressed: Boolean
    ) {
        if (isPressed) {
            hapticDriver.clickFeedback()
        }

        when (activeProfile?.inputMode) {
            InputMode.SHIZUKU_ADB -> {
                adbDriver.sendKeyDefinitionState(
                    definition,
                    isPressed
                )
            }

            InputMode.BLUETOOTH_HID -> {
                if (definition.hidUsageId != 0) {
                    bluetoothDriver.sendKeyboardKey(
                        definition.hidUsageId,
                        isPressed
                    )
                }
            }

            else -> {
                /*
                 * OVERLAY_HUD uses Accessibility for touch controls.
                 * UDP has no keyboard report in this manager.
                 */
            }
        }

        addLog(
            "KEY [${definition.label}] -> " +
                if (isPressed) "DOWN" else "UP"
        )
    }

    /**
     * Sends button state only through the selected backend.
     */
    private fun dispatchButtonToActiveBackend(
        button: GamepadButtonType,
        isPressed: Boolean,
        state: GamepadState
    ) {
        when (activeProfile?.inputMode) {

            InputMode.BLUETOOTH_HID -> {
                /*
                 * The complete GamepadState is sent so simultaneous
                 * buttons + both sticks remain synchronized.
                 */
                bluetoothDriver.sendGamepadState(state)
            }

            InputMode.UDP_NETWORK -> {
                udpDriver.sendState(state)
            }

            InputMode.SHIZUKU_ADB -> {
                adbDriver.sendButtonState(
                    button,
                    isPressed
                )
            }

            InputMode.OVERLAY_HUD -> {
                /*
                 * Touch controls are handled separately by
                 * Accessibility. No duplicate key injection.
                 */
            }

            null -> {
                Log.w(
                    TAG,
                    "Button ignored: no active input profile"
                )
            }
        }
    }

    /**
     * Sends the complete state to the selected analog backend.
     */
    private fun dispatchToActiveDrivers(
        state: GamepadState
    ) {
        when (activeProfile?.inputMode) {

            InputMode.BLUETOOTH_HID -> {
                bluetoothDriver.sendGamepadState(state)
            }

            InputMode.UDP_NETWORK -> {
                udpDriver.sendState(state)
            }

            /*
             * Shizuku/ADB does not have a proper generic analog
             * gamepad report through the current driver.
             */
            InputMode.SHIZUKU_ADB -> {
                // Digital buttons are dispatched separately.
            }

            InputMode.OVERLAY_HUD -> {
                // Accessibility joystick/button path is separate.
            }

            null -> {
                // No active profile.
            }
        }
    }

    /**
     * Releases every active state.
     */
    fun releaseAll() {

        /*
         * First stop Accessibility gestures.
         */
        if (activeProfile?.inputMode == InputMode.SHIZUKU_ADB) {
            val metrics = context.resources.displayMetrics

            listOf(
                "shizuku_joystick_left" to GamepadButtonType.JOYSTICK_LEFT,
                "shizuku_joystick_right" to GamepadButtonType.JOYSTICK_RIGHT
            ).forEach { (controlId, joystickType) ->
                val pointerId = controlPointerMap.remove(controlId)
                    ?: return@forEach

                val item = activeProfile?.controls
                    ?.firstOrNull { it.type == joystickType }

                val centerX = item?.xPercent?.times(metrics.widthPixels)
                    ?: (metrics.widthPixels / 2f)
                val centerY = item?.yPercent?.times(metrics.heightPixels)
                    ?: (metrics.heightPixels / 2f)

                joystickLastPosition.remove(controlId)
                adbDriver.sendRawTouchUp(pointerId, centerX, centerY)
            }
        }

        controlPointerMap.clear()

        FlexiPadAccessibilityService
            .instance
            ?.releaseAll()

        /*
         * Release all digital buttons through the selected backend.
         */
        val oldState = _currentState.value

        when (activeProfile?.inputMode) {

            InputMode.BLUETOOTH_HID -> {
                bluetoothDriver.sendGamepadState(
                    GamepadState()
                )
            }

            InputMode.UDP_NETWORK -> {
                udpDriver.sendState(
                    GamepadState()
                )
            }

            InputMode.SHIZUKU_ADB -> {
                oldState.pressedButtons.forEach { button ->
                    adbDriver.sendButtonState(
                        button,
                        false
                    )
                }

                adbDriver.releaseAll()
            }

            InputMode.OVERLAY_HUD -> {
                // Accessibility release above.
            }

            null -> {
                // Nothing active.
            }
        }

        _currentState.value = GamepadState()

        addLog("ALL INPUTS RELEASED")
    }

    fun destroy() {
        releaseAll()

        bluetoothDriver.stop()
        udpDriver.stop()
    }
}
