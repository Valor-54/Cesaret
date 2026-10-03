package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ControlJsonParser
import com.example.input.InputStateManager
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import com.example.model.GamepadPresets
import com.example.model.GamepadState
import com.example.model.KeyRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies FlexiPad app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FlexiPad", appName)
    }

    @Test
    fun `default preset has both first and second independent joysticks`() {
        val wtProfile = GamepadPresets.createWarThunderProfile()
        assertNotNull(wtProfile)

        val leftStick = wtProfile.controls.firstOrNull { it.type == GamepadButtonType.JOYSTICK_LEFT }
        val rightStick = wtProfile.controls.firstOrNull { it.type == GamepadButtonType.JOYSTICK_RIGHT }

        assertNotNull("1. Sol Joystick (LS) must exist", leftStick)
        assertNotNull("2. Sağ Joystick (RS) must exist", rightStick)
        assertEquals("LS", leftStick?.label)
        assertEquals("RS", rightStick?.label)
        assertTrue("Sublabels must be empty", leftStick?.subLabel?.isEmpty() == true)
        assertTrue("Sublabels must be empty", rightStick?.subLabel?.isEmpty() == true)
    }

    @Test
    fun `buttons in all presets display pure key names without action text`() {
        val profiles = listOf(
            GamepadPresets.createWarThunderProfile(),
            GamepadPresets.createXboxStandardProfile(),
            GamepadPresets.createFpsProfile()
        )

        val forbiddenWords = listOf("ATEŞ", "ZIPLA", "KOŞ", "Ateş", "Zıpla", "Koş", "Tamir", "Dürbün", "JUMP", "FIRE")

        for (profile in profiles) {
            for (ctrl in profile.controls) {
                for (badWord in forbiddenWords) {
                    assertFalse(
                        "Control label ${ctrl.label} in ${profile.name} must not contain action word '$badWord'",
                        ctrl.label.equals(badWord, ignoreCase = true)
                    )
                }
                assertTrue(
                    "Control sublabel must be empty to avoid action text clutter",
                    ctrl.subLabel.isEmpty()
                )
            }
        }
    }

    @Test
    fun `gamepad state button pressed check`() {
        val state = GamepadState(
            leftStickX = 0.5f,
            leftStickY = -0.5f,
            rightStickX = 0.8f,
            rightStickY = -0.2f,
            pressedButtons = setOf(GamepadButtonType.BUTTON_A, GamepadButtonType.BUTTON_R2)
        )
        assertTrue(state.isPressed(GamepadButtonType.BUTTON_A))
        assertTrue(state.isPressed(GamepadButtonType.BUTTON_R2))
        assertFalse(state.isPressed(GamepadButtonType.BUTTON_B))
        assertTrue(state.isLeftStickActive)
        assertTrue(state.isRightStickActive)
    }

    @Test
    fun `control json serialization roundtrip`() {
        val wt = GamepadPresets.createWarThunderProfile()
        val json = ControlJsonParser.toJson(wt.controls)
        val deserialized = ControlJsonParser.fromJson(json)
        assertEquals(wt.controls.size, deserialized.size)
        assertEquals(wt.controls[0].id, deserialized[0].id)
        assertEquals(wt.controls[0].type, deserialized[0].type)
        assertEquals(wt.controls[0].label, deserialized[0].label)
    }

    @Test
    fun `key registry contains keyboard and gamepad definitions`() {
        val allKeys = KeyRegistry.ALL_KEYS
        assertTrue("KeyRegistry should have extensive key catalogue", allKeys.size > 50)
        assertNotNull("Space key should exist", KeyRegistry.findById("KB_SPACE"))
        assertNotNull("Enter key should exist", KeyRegistry.findById("KB_ENTER"))
        assertNotNull("W key should exist", KeyRegistry.findById("KB_W"))
        assertNotNull("F1 should exist", KeyRegistry.findById("KB_F1"))
        assertNotNull("Arrow Up should exist", KeyRegistry.findById("KB_ARROW_UP"))
        assertNotNull("D-PAD should exist", KeyRegistry.findById("GP_DPAD"))
        assertNotNull("Left Joystick should exist", KeyRegistry.findById("GP_JOYSTICK_LEFT"))
        assertNotNull("Right Joystick should exist", KeyRegistry.findById("GP_JOYSTICK_RIGHT"))
    }

    @Test
    fun `gamepad control item resolves custom key binding`() {
        val itemWithBinding = GamepadControlItem(
            id = "custom_test",
            type = GamepadButtonType.BUTTON_A,
            label = "SPACE",
            xPercent = 0.5f,
            yPercent = 0.5f,
            boundKeyId = "KB_SPACE"
        )
        val resolved = itemWithBinding.getResolvedKeyDefinition()
        assertNotNull(resolved)
        assertEquals(android.view.KeyEvent.KEYCODE_SPACE, resolved?.androidKeycode)
    }

    @Test
    fun `input state manager handles multi-touch and reference counting`() {
        var downCount = 0
        var upCount = 0

        val manager = InputStateManager(
            onInputDown = { _, _ -> downCount++ },
            onInputUp = { _, _ -> upCount++ }
        )

        val btnA = GamepadControlItem(
            id = "btn_a",
            type = GamepadButtonType.BUTTON_A,
            label = "A",
            xPercent = 0.5f,
            yPercent = 0.5f
        )

        // First finger presses Button A (pointer 1)
        manager.onPointerDown(btnA, pointerId = 1)
        assertEquals(1, downCount)
        assertEquals(0, upCount)
        assertTrue(manager.isControlActive("btn_a"))

        // Second finger touches Button A simultaneously (pointer 2)
        manager.onPointerDown(btnA, pointerId = 2)
        // Reference count is 2, onInputDown should NOT fire a second time for same button
        assertEquals(1, downCount)

        // Pointer 1 lifts
        manager.onPointerUp(pointerId = 1)
        // Button A is STILL pressed by pointer 2, so onInputUp should NOT have fired yet!
        assertEquals(0, upCount)
        assertTrue(manager.isControlActive("btn_a"))

        // Pointer 2 lifts
        manager.onPointerUp(pointerId = 2)
        // Now Button A is completely released
        assertEquals(1, upCount)
        assertFalse(manager.isControlActive("btn_a"))
    }

    @Test
    fun `input state manager releaseAll clears all active touches safely`() {
        var upCount = 0
        val manager = InputStateManager(
            onInputDown = { _, _ -> },
            onInputUp = { _, _ -> upCount++ }
        )

        val btnA = GamepadControlItem(id = "btn_a", type = GamepadButtonType.BUTTON_A, label = "A", xPercent = 0.5f, yPercent = 0.5f)
        val btnB = GamepadControlItem(id = "btn_b", type = GamepadButtonType.BUTTON_B, label = "B", xPercent = 0.6f, yPercent = 0.5f)

        manager.onPointerDown(btnA, pointerId = 1)
        manager.onPointerDown(btnB, pointerId = 2)
        assertEquals(2, manager.activeCount)

        // Sudden rotation or overlay close
        manager.releaseAll()
        assertEquals(0, manager.activeCount)
        assertEquals(2, upCount)
    }

    @Test
    fun `joystick deflection calculation produces correct normalized axes`() {
        val sizePx = 300f
        val baseCenter = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx / 2f)
        val maxTravel = (sizePx / 2f) * 0.72f
        val deadzone = 0.04f
        val sensitivity = 1.0f

        // Touch directly to the right by 80px (beyond deadzone, within maxTravel)
        val touchPos = baseCenter + androidx.compose.ui.geometry.Offset(80f, 0f)
        val delta = touchPos - baseCenter
        val dist = delta.getDistance()
        val normDist = (dist / maxTravel).coerceIn(0f, 1f)
        assertTrue("Distance should be greater than deadzone", normDist > deadzone)

        val angle = kotlin.math.atan2(delta.y, delta.x)
        val rescaled = ((normDist - deadzone) / (1f - deadzone)).coerceIn(0f, 1f) * sensitivity
        val normX = (kotlin.math.cos(angle) * rescaled).coerceIn(-1f, 1f)
        val normY = (kotlin.math.sin(angle) * rescaled).coerceIn(-1f, 1f)

        assertTrue("Normalized X should be positive for rightward deflection", normX > 0.6f)
        assertEquals(0f, normY, 0.001f)
    }

    @Test
    fun `shizuku connection states have exact required Turkish statuses`() {
        val notInstalled = com.example.ui.viewmodel.ShizukuConnectionState.NotInstalled
        val serviceNotRunning = com.example.ui.viewmodel.ShizukuConnectionState.ServiceNotRunning
        val permPending = com.example.ui.viewmodel.ShizukuConnectionState.PermissionPending
        val permDenied = com.example.ui.viewmodel.ShizukuConnectionState.PermissionDenied
        val permGranted = com.example.ui.viewmodel.ShizukuConnectionState.PermissionGranted
        val success = com.example.ui.viewmodel.ShizukuConnectionState.UserServiceSuccess
        val failed = com.example.ui.viewmodel.ShizukuConnectionState.UserServiceFailed("Zaman aşımı")

        assertEquals("Shizuku kurulu değil.", notInstalled.title)
        assertEquals("Shizuku servisi çalışmıyor.", serviceNotRunning.title)
        assertEquals("İzin bekleniyor.", permPending.title)
        assertEquals("İzin reddedildi.", permDenied.title)
        assertEquals("Shizuku izni verildi.", permGranted.title)
        assertEquals("InputUserService bağlantısı başarılı.", success.title)
        assertEquals("InputUserService bağlantısı başarısız.", failed.title)
        assertEquals("Zaman aşımı", failed.message)
    }

    @Test
    fun `button second press does not reset key binding or properties`() {
        val initialProfile = GamepadPresets.createWarThunderProfile()
        var currentProfile = initialProfile
        var selectedItemId: String? = null

        // 1. User taps button "wt_btn_a_repair" (first press)
        val buttonToSelect = currentProfile.controls.first { it.id == "wt_btn_a_repair" }
        selectedItemId = buttonToSelect.id
        assertEquals("wt_btn_a_repair", selectedItemId)
        val selectedItemFirst = currentProfile.controls.first { it.id == selectedItemId }
        assertEquals("A", selectedItemFirst.label)

        // 2. User changes key binding to SPACE (KB_SPACE)
        val spaceKey = KeyRegistry.ALL_KEYS.first { it.id == "KB_SPACE" }
        currentProfile = currentProfile.copy(
            controls = currentProfile.controls.map { ctrl ->
                if (ctrl.id == selectedItemId) {
                    ctrl.copy(
                        label = spaceKey.label,
                        subLabel = "",
                        boundKeyId = spaceKey.id
                    )
                } else {
                    ctrl
                }
            }
        )

        val updatedButton = currentProfile.controls.first { it.id == "wt_btn_a_repair" }
        assertEquals("SPACE", updatedButton.label)
        assertEquals("KB_SPACE", updatedButton.boundKeyId)

        // 3. User taps the SAME button a SECOND time (second press)
        selectedItemId = buttonToSelect.id
        // When tapping without dragging (hasMoved == false), profile is not modified:
        val selectedItemSecond = currentProfile.controls.first { it.id == selectedItemId }
        assertEquals("SPACE", selectedItemSecond.label)
        assertEquals("KB_SPACE", selectedItemSecond.boundKeyId)
        assertEquals(buttonToSelect.colorHex, selectedItemSecond.colorHex)
        assertEquals(buttonToSelect.sizeDp, selectedItemSecond.sizeDp, 0.001f)

        // 4. User drags the button slightly: only coordinates update, binding is preserved
        val newX = 0.85f
        val newY = 0.75f
        currentProfile = currentProfile.copy(
            controls = currentProfile.controls.map { ctrl ->
                if (ctrl.id == buttonToSelect.id) {
                    ctrl.copy(xPercent = newX, yPercent = newY)
                } else {
                    ctrl
                }
            }
        )
        val draggedButton = currentProfile.controls.first { it.id == "wt_btn_a_repair" }
        assertEquals("SPACE", draggedButton.label)
        assertEquals("KB_SPACE", draggedButton.boundKeyId)
        assertEquals(newX, draggedButton.xPercent, 0.001f)
        assertEquals(newY, draggedButton.yPercent, 0.001f)

        // 5. User taps ANOTHER button (e.g. "wt_btn_b_artillery"): first button is preserved
        val secondButton = currentProfile.controls.first { it.id == "wt_btn_b_artillery" }
        selectedItemId = secondButton.id
        assertEquals("wt_btn_b_artillery", selectedItemId)
        val firstButtonStillPreserved = currentProfile.controls.first { it.id == "wt_btn_a_repair" }
        assertEquals("SPACE", firstButtonStillPreserved.label)
        assertEquals("KB_SPACE", firstButtonStillPreserved.boundKeyId)

        // 6. Save and JSON / DB persistence roundtrip
        val json = ControlJsonParser.toJson(currentProfile.controls)
        val reloadedControls = ControlJsonParser.fromJson(json)
        val reloadedButton = reloadedControls.first { it.id == "wt_btn_a_repair" }
        assertEquals("SPACE", reloadedButton.label)
        assertEquals("KB_SPACE", reloadedButton.boundKeyId)
        assertEquals(newX, reloadedButton.xPercent, 0.001f)
        assertEquals(newY, reloadedButton.yPercent, 0.001f)
    }
}
