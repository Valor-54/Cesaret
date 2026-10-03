package com.example.input

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.GamepadButtonType
import com.example.model.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Handles Bluetooth HID Composite Device (Gamepad + Keyboard) profile (API 28+).
 * This presents the Android device as a genuine physical Bluetooth Gamepad and Keyboard to any host
 * (such as a PC running GeForce NOW, tablet, console, or secondary device).
 */
class BluetoothHidDriver(private val context: Context) {
    private val TAG = "BluetoothHidDriver"
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _statusMessage = MutableStateFlow("Bluetooth HID Hazır")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val executor = Executors.newSingleThreadExecutor()
    private val pressedHidKeys = ConcurrentHashMap.newKeySet<Int>()

    /**
     * Composite USB/Bluetooth HID Descriptor
     * Report ID 1: Gamepad (16 buttons, 4 axes, 8-way Hat Switch D-Pad, 2 analog triggers)
     * Report ID 2: Keyboard (Modifiers, 6-key rollover standard USB HID)
     */
    private val HID_REPORT_DESCRIPTOR = byteArrayOf(
        // --- REPORT ID 1: GAMEPAD ---
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // USAGE (Gamepad)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0xA1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
        0x85.toByte(), 0x01.toByte(), //     REPORT_ID (1)
        // 16 Buttons (Bits 0-15)
        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x10.toByte(), //     USAGE_MAXIMUM (Button 16)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
        0x95.toByte(), 0x10.toByte(), //     REPORT_COUNT (16)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        // Hat Switch / D-Pad (4 bits, 0-7 directions, 8 = neutral)
        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x39.toByte(), //     USAGE (Hat switch)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x07.toByte(), //     LOGICAL_MAXIMUM (7)
        0x35.toByte(), 0x00.toByte(), //     PHYSICAL_MINIMUM (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // PHYSICAL_MAXIMUM (315)
        0x65.toByte(), 0x14.toByte(), //     UNIT (Eng Rot:Angular Pos)
        0x75.toByte(), 0x04.toByte(), //     REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x42.toByte(), //     INPUT (Data,Var,Abs,Null)
        // 4 bits padding for byte alignment
        0x75.toByte(), 0x04.toByte(), //     REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x01.toByte(), //     INPUT (Cnst,Ary,Abs)
        // 4 Analog Axes: Left X, Left Y, Right X (Z), Right Y (Rz) (-127 to 127)
        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
        0x09.toByte(), 0x32.toByte(), //     USAGE (Z)
        0x09.toByte(), 0x35.toByte(), //     USAGE (Rz)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x04.toByte(), //     REPORT_COUNT (4)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        // 2 Analog Triggers: L2 (Rx), R2 (Ry) (0 to 255)
        0x09.toByte(), 0x33.toByte(), //     USAGE (Rx - L2 Trigger)
        0x09.toByte(), 0x34.toByte(), //     USAGE (Ry - R2 Trigger)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        0xC0.toByte(),               //   END_COLLECTION (Physical)
        0xC0.toByte(),               // END_COLLECTION (Gamepad)

        // --- REPORT ID 2: KEYBOARD ---
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs) ; Modifier byte
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x81.toByte(), 0x01.toByte(), //   INPUT (Cnst,Ary,Abs) ; Reserved byte
        0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (0)
        0x29.toByte(), 0xFF.toByte(), //   USAGE_MAXIMUM (255)
        0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs) ; 6 keycodes
        0xC0.toByte()                 // END_COLLECTION (Keyboard)
    )

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                hidDevice = proxy
                _statusMessage.value = "Bluetooth HID Servisi Bağlandı"
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isConnected.value = false
                _statusMessage.value = "Bluetooth HID Servis Bağlantısı Kesildi"
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            if (registered) {
                _statusMessage.value = "FlexiPad Composite HID Gamepad & Klavye Kayıtlı"
            } else {
                _statusMessage.value = "HID Kaydı Kaldırıldı"
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _isConnected.value = true
                    _statusMessage.value = "Eşleşildi: ${device?.name ?: "Bilinmeyen Cihaz"}"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    _isConnected.value = false
                    _statusMessage.value = "Bağlantı Kesildi"
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _statusMessage.value = "Bağlanıyor..."
                }
            }
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                bluetoothAdapter?.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
            } catch (e: Exception) {
                Log.e(TAG, "HID Device Proxy error", e)
                _statusMessage.value = "Bluetooth HID Başlamadı: ${e.message}"
            }
        } else {
            _statusMessage.value = "Bluetooth HID Android 9+ gerektirir"
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val hid = hidDevice ?: return
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "FlexiPad Controller",
                "FlexiPad Virtual Gamepad and Keyboard for Cloud Gaming",
                "FlexiPad",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HID_REPORT_DESCRIPTOR
            )
            hid.registerApp(sdp, null, null, executor, hidCallback)
        } catch (e: Exception) {
            Log.e(TAG, "registerApp failed", e)
            _statusMessage.value = "HID Kayıt Hatası: ${e.localizedMessage}"
        }
    }

    /**
     * Translates the current [GamepadState] into raw HID Gamepad report bytes (Report ID 1)
     */
    @SuppressLint("MissingPermission")
    fun sendGamepadState(state: GamepadState) {
        val hid = hidDevice ?: return
        val target = connectedDevice ?: return

        val report = ByteArray(9)

        // Buttons 1-8 bitmask
        var btnLow = 0
        if (state.isPressed(GamepadButtonType.BUTTON_A)) btnLow = btnLow or (1 shl 0)
        if (state.isPressed(GamepadButtonType.BUTTON_B)) btnLow = btnLow or (1 shl 1)
        if (state.isPressed(GamepadButtonType.BUTTON_X)) btnLow = btnLow or (1 shl 2)
        if (state.isPressed(GamepadButtonType.BUTTON_Y)) btnLow = btnLow or (1 shl 3)
        if (state.isPressed(GamepadButtonType.BUTTON_L1)) btnLow = btnLow or (1 shl 4)
        if (state.isPressed(GamepadButtonType.BUTTON_R1)) btnLow = btnLow or (1 shl 5)
        if (state.isPressed(GamepadButtonType.BUTTON_L3)) btnLow = btnLow or (1 shl 6)
        if (state.isPressed(GamepadButtonType.BUTTON_R3)) btnLow = btnLow or (1 shl 7)
        report[0] = btnLow.toByte()

        // Buttons 9-16 bitmask
        var btnHigh = 0
        if (state.isPressed(GamepadButtonType.BUTTON_SELECT)) btnHigh = btnHigh or (1 shl 0)
        if (state.isPressed(GamepadButtonType.BUTTON_START)) btnHigh = btnHigh or (1 shl 1)
        if (state.isPressed(GamepadButtonType.BUTTON_HOME)) btnHigh = btnHigh or (1 shl 2)
        if (state.l2Trigger > 0.5f || state.isPressed(GamepadButtonType.BUTTON_L2)) btnHigh = btnHigh or (1 shl 3)
        if (state.r2Trigger > 0.5f || state.isPressed(GamepadButtonType.BUTTON_R2)) btnHigh = btnHigh or (1 shl 4)
        report[1] = btnHigh.toByte()

        // Hat Switch (D-Pad)
        val dpadValue = calculateHatSwitch(state)
        report[2] = dpadValue.toByte()

        // Left Stick (-127 to 127)
        report[3] = (state.leftStickX * 127f).coerceIn(-127f, 127f).toInt().toByte()
        report[4] = (state.leftStickY * 127f).coerceIn(-127f, 127f).toInt().toByte()

        // Right Stick (-127 to 127)
        report[5] = (state.rightStickX * 127f).coerceIn(-127f, 127f).toInt().toByte()
        report[6] = (state.rightStickY * 127f).coerceIn(-127f, 127f).toInt().toByte()

        // Analog Triggers (0 to 255)
        report[7] = (state.l2Trigger * 255f).coerceIn(0f, 255f).toInt().toByte()
        report[8] = (state.r2Trigger * 255f).coerceIn(0f, 255f).toInt().toByte()

        try {
            hid.sendReport(target, 1, report)
        } catch (e: Exception) {
            Log.e(TAG, "sendReport Gamepad failed", e)
        }
    }

    /**
     * Sends a real USB Keyboard HID report (Report ID 2) for standard keyboard keys
     */
    @SuppressLint("MissingPermission")
    fun sendKeyboardKey(hidUsageId: Int, isPressed: Boolean) {
        val hid = hidDevice ?: return
        val target = connectedDevice ?: return

        if (isPressed) {
            pressedHidKeys.add(hidUsageId)
        } else {
            pressedHidKeys.remove(hidUsageId)
        }

        val report = ByteArray(8)
        var modifier = 0
        var keyIndex = 2
        for (code in pressedHidKeys) {
            when (code) {
                0xE0 -> modifier = modifier or (1 shl 0) // Left Ctrl
                0xE1 -> modifier = modifier or (1 shl 1) // Left Shift
                0xE2 -> modifier = modifier or (1 shl 2) // Left Alt
                0xE3 -> modifier = modifier or (1 shl 3) // Left GUI
                0xE4 -> modifier = modifier or (1 shl 4) // Right Ctrl
                0xE5 -> modifier = modifier or (1 shl 5) // Right Shift
                0xE6 -> modifier = modifier or (1 shl 6) // Right Alt
                0xE7 -> modifier = modifier or (1 shl 7) // Right GUI
                else -> {
                    if (keyIndex < 8) {
                        report[keyIndex++] = code.toByte()
                    }
                }
            }
        }
        report[0] = modifier.toByte()

        try {
            hid.sendReport(target, 2, report)
        } catch (e: Exception) {
            Log.e(TAG, "sendReport Keyboard failed", e)
        }
    }

    private fun calculateHatSwitch(state: GamepadState): Int {
        val up = state.isPressed(GamepadButtonType.DPAD_UP)
        val down = state.isPressed(GamepadButtonType.DPAD_DOWN)
        val left = state.isPressed(GamepadButtonType.DPAD_LEFT)
        val right = state.isPressed(GamepadButtonType.DPAD_RIGHT)

        return when {
            up && right -> 1
            down && right -> 3
            down && left -> 5
            up && left -> 7
            up -> 0
            right -> 2
            down -> 4
            left -> 6
            else -> 8 // Centered / Neutral
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            hidDevice?.unregisterApp()
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            hidDevice = null
            connectedDevice = null
            _isConnected.value = false
            pressedHidKeys.clear()
        } catch (e: Exception) {
            Log.e(TAG, "stop failed", e)
        }
    }
}
