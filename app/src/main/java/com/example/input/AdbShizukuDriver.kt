package com.example.input

import android.content.Context
import android.util.Log
import com.example.model.GamepadButtonType
import com.example.model.InputKeyDefinition
import com.example.shizuku.ShizukuInputBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class AdbShizukuDriver(private val context: Context) {
    val shizukuBridge = ShizukuInputBridge.instance

    private val tag = "AdbShizukuDriver"
    private val executor = Executors.newCachedThreadPool()

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> =
        _isShizukuAvailable.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> =
        _isRootAvailable.asStateFlow()

    private val _statusText =
        MutableStateFlow("Sistem giriş durumu kontrol ediliyor...")
    val statusText: StateFlow<String> =
        _statusText.asStateFlow()

    private val heldKeys =
        ConcurrentHashMap.newKeySet<Int>()

    init {
        checkStatus()
    }

    fun checkStatus() {
        executor.execute {
            var shizukuDetected = false
            var rootDetected = false

            try {
                shizukuDetected = rikka.shizuku.Shizuku.pingBinder()
            } catch (e: Exception) {
                try {
                    val provider = context.packageManager.resolveContentProvider(
                        "moe.shizuku.privileged.api.provider",
                        0
                    )
                    shizukuDetected = provider != null
                } catch (_: Exception) {
                    shizukuDetected = false
                }
            }

            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "id")
                )

                rootDetected = process.waitFor() == 0
            } catch (_: Exception) {
                rootDetected = false
            }

            _isShizukuAvailable.value = shizukuDetected
            _isRootAvailable.value = rootDetected

            _statusText.value = when {
                rootDetected -> "Root erişimi hazır"
                shizukuDetected && shizukuBridge.connected -> "Shizuku bağlı (InputUserService aktif)"
                shizukuDetected -> "Shizuku servisi çalışıyor"
                else -> "Standart erişim"
            }

            Log.i(
                tag,
                "Status: Shizuku=$shizukuDetected Root=$rootDetected Connected=${shizukuBridge.connected}"
            )
        }
    }

    fun getAndroidKeycode(button: GamepadButtonType): Int {
        return when (button) {
            GamepadButtonType.BUTTON_A,
            GamepadButtonType.WT_REPAIR -> 96

            GamepadButtonType.BUTTON_B,
            GamepadButtonType.WT_ARTILLERY -> 97

            GamepadButtonType.BUTTON_X,
            GamepadButtonType.WT_BINOCULAR -> 99

            GamepadButtonType.BUTTON_Y,
            GamepadButtonType.WT_LOCK_TARGET -> 100

            GamepadButtonType.BUTTON_L1,
            GamepadButtonType.WT_SMOKE -> 102

            GamepadButtonType.BUTTON_R1 -> 103

            GamepadButtonType.BUTTON_L2,
            GamepadButtonType.WT_ZOOM -> 104

            GamepadButtonType.BUTTON_R2,
            GamepadButtonType.WT_MAIN_GUN -> 105

            GamepadButtonType.BUTTON_L3 -> 106
            GamepadButtonType.BUTTON_R3 -> 107

            GamepadButtonType.BUTTON_START -> 108
            GamepadButtonType.BUTTON_SELECT -> 109
            GamepadButtonType.BUTTON_HOME -> 110

            GamepadButtonType.DPAD_UP -> 19
            GamepadButtonType.DPAD_DOWN -> 20
            GamepadButtonType.DPAD_LEFT -> 21
            GamepadButtonType.DPAD_RIGHT -> 22

            else -> 0
        }
    }

    fun sendButtonState(
        button: GamepadButtonType,
        isPressed: Boolean
    ) {
        val keycode = getAndroidKeycode(button)

        if (keycode != 0) {
            sendKeycodeState(keycode, isPressed)
        }
    }

    fun sendKeyDefinitionState(
        definition: InputKeyDefinition,
        isPressed: Boolean
    ) {
        if (definition.androidKeycode != 0) {
            sendKeycodeState(
                definition.androidKeycode,
                isPressed
            )
        }
    }

    fun sendKeycodeState(
        keycode: Int,
        isPressed: Boolean
    ) {
        if (keycode == 0) return

        if (isPressed) {
            if (!heldKeys.add(keycode)) return
        } else {
            if (!heldKeys.remove(keycode)) return
        }

        try {
            if (!shizukuBridge.connected) {
                shizukuBridge.start()
                Thread.sleep(150)
            }

            if (!shizukuBridge.connected) {
                Log.w(
                    tag,
                    "Shizuku UserService bağlı değil: keyCode=$keycode"
                )

                if (isPressed) {
                    heldKeys.remove(keycode)
                } else {
                    heldKeys.add(keycode)
                }
                return
            }

            val result = shizukuBridge.sendKeyState(
                keyCode = keycode,
                pressed = isPressed
            )

            Log.d(
                tag,
                "KEY ${if (isPressed) "DOWN" else "UP"} " +
                    "keycode=$keycode result=$result"
            )

            if (!result) {
                if (isPressed) {
                    heldKeys.remove(keycode)
                } else {
                    heldKeys.add(keycode)
                }
            }
        } catch (e: Throwable) {
            Log.e(
                tag,
                "Key injection failed keyCode=$keycode",
                e
            )

            if (isPressed) {
                heldKeys.remove(keycode)
            } else {
                heldKeys.add(keycode)
            }
        }
    }

    private fun sendRawTouch(pointerId: Int, action: Int, screenX: Float, screenY: Float): Boolean {
        return try {
            if (!shizukuBridge.connected) {
                shizukuBridge.start()
                Thread.sleep(150)
            }

            if (!shizukuBridge.connected) {
                Log.w(tag, "Shizuku UserService bağlı değil")
                return false
            }

            shizukuBridge.sendTouch(
                pointerId = pointerId,
                action = action,
                x = screenX,
                y = screenY
            )
        } catch (e: Throwable) {
            Log.e(tag, "RAW touch injection failed", e)
            false
        }
    }

    private fun executeInputCommand(command: String) {
        executor.execute {
            try {
                if (!shizukuBridge.connected) {
                    shizukuBridge.start()
                    Thread.sleep(150)
                }

                if (shizukuBridge.connected) {
                    val exitCode = shizukuBridge.execute(command)
                    Log.v(tag, "Shizuku executed: $command exit=$exitCode")
                } else {
                    Log.w(tag, "Shizuku UserService bağlı değil: $command")
                }
            } catch (e: Exception) {
                Log.e(tag, "Shizuku input command failed: $command", e)
            }
        }
    }

    fun releaseAll() {
        try {
            if (!shizukuBridge.connected) {
                shizukuBridge.start()
                Thread.sleep(150)
            }

            if (shizukuBridge.connected) {
                shizukuBridge.releaseAllInput()
            }
        } catch (e: Throwable) {
            Log.e(
                tag,
                "Tüm input bırakılırken hata",
                e
            )
        } finally {
            heldKeys.clear()
        }

        Log.i(
            tag,
            "All tracked input states released"
        )
    }

    fun destroy() {
        releaseAll()

        try {
            executor.shutdownNow()
        } catch (_: Exception) {
        }
    }
}
