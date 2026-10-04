package com.example.shizuku

import android.hardware.input.InputManager
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class InputUserService : Binder() {

    companion object {
        private const val TAG = "FlexiPadShizuku"

        private const val DESCRIPTOR =
            "com.example.shizuku.IInputUserService"

        private const val TRANSACTION_EXECUTE =
            IBinder.FIRST_CALL_TRANSACTION

        private const val TRANSACTION_PING =
            IBinder.FIRST_CALL_TRANSACTION + 1

        private const val TRANSACTION_TOUCH =
            IBinder.FIRST_CALL_TRANSACTION + 2

        // Gerçek KeyEvent ACTION_DOWN / ACTION_UP (basılı tutma desteği)
        private const val TRANSACTION_KEY =
            IBinder.FIRST_CALL_TRANSACTION + 3

        // Enjeksiyon yolunun gerçekten hazır olup olmadığını bildirir
        private const val TRANSACTION_INFO =
            IBinder.FIRST_CALL_TRANSACTION + 4

        // Shizuku'nun UserService için tanımladığı destroy() işlem kodu
        private const val TRANSACTION_DESTROY = 16777114

        private const val TOUCH_DOWN = 0
        private const val TOUCH_MOVE = 1
        private const val TOUCH_UP = 2
    }

    private data class PointerState(
        val pointerId: Int,
        var x: Float,
        var y: Float
    )

    private val pointers = ConcurrentHashMap<Int, PointerState>()
    private val touchLock = Any()
    private val heldKeys = ConcurrentHashMap.newKeySet<Int>()
    private var gestureDownTime = 0L

    /**
     * Enjeksiyon yolu, Android sürümüne göre değişir. Shizuku'nun açtığı shell
     * sürecinde ActivityThread.currentApplication() olmadığından, yeni sürümlerde
     * InputManager.getInstance() başarısız olabilir. Bu yüzden sırayla denenen
     * birden fazla aday kullanılır; ilk çalışan hatırlanır.
     */
    private class Injector(
        val name: String,
        private val target: Any,
        private val method: Method,
        private val extra: Array<Any?>
    ) {
        fun invoke(event: InputEvent): Boolean {
            val r = method.invoke(target, event, 0, *extra)
            return (r as? Boolean) ?: true
        }
    }

    @Volatile
    private var workingInjector: Injector? = null

    @Volatile
    private var lastInjectError: String? = null

    private val injectors: List<Injector> by lazy { buildInjectors() }

    private fun findInject(cls: Class<*>, vararg params: Class<*>): Method? {
        return try {
            cls.getMethod("injectInputEvent", *params)
        } catch (_: Throwable) {
            try {
                cls.getDeclaredMethod("injectInputEvent", *params)
                    .apply { isAccessible = true }
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun buildInjectors(): List<Injector> {
        val list = mutableListOf<Injector>()
        val intType = Int::class.javaPrimitiveType!!

        fun tryAdd(name: String, provider: () -> Any?) {
            try {
                val target = provider()
                if (target == null) {
                    Log.w(TAG, "Enjeksiyon adayı yok: $name (null)")
                    return
                }
                val cls = target.javaClass

                findInject(cls, InputEvent::class.java, intType)?.let {
                    list.add(Injector(name, target, it, emptyArray()))
                }
                findInject(cls, InputEvent::class.java, intType, intType)?.let {
                    list.add(Injector("$name/3arg", target, it, arrayOf<Any?>(-1)))
                }
            } catch (e: Throwable) {
                val cause = e.cause ?: e
                Log.w(TAG, "Enjeksiyon adayı kullanılamadı: $name -> ${cause.javaClass.simpleName}: ${cause.message}")
                lastInjectError = "$name: ${cause.javaClass.simpleName}"
            }
        }

        tryAdd("InputManagerGlobal") {
            Class.forName("android.hardware.input.InputManagerGlobal")
                .getMethod("getInstance")
                .invoke(null)
        }

        tryAdd("InputManager") {
            InputManager::class.java
                .getDeclaredMethod("getInstance")
                .apply { isAccessible = true }
                .invoke(null)
        }

        tryAdd("IInputManager") {
            val sm = Class.forName("android.os.ServiceManager")
            val binder = sm.getMethod("getService", String::class.java)
                .invoke(null, "input") as? IBinder
            Class.forName("android.hardware.input.IInputManager\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, binder)
        }

        Log.i(TAG, "Enjeksiyon adayları: ${list.map { it.name }}")
        return list
    }

    /** OLAYI SİSTEME ENJEKTE EDER. Dönüş: sistemin olayı kabul edip etmediği. */
    private fun injectEvent(event: InputEvent): Boolean {
        val cached = workingInjector
        if (cached != null) {
            try {
                return cached.invoke(event)
            } catch (e: Throwable) {
                Log.w(TAG, "Çalışan enjektör başarısız oldu, adaylar yeniden denenecek", e)
                workingInjector = null
            }
        }

        for (candidate in injectors) {
            try {
                val result = candidate.invoke(event)
                workingInjector = candidate
                lastInjectError = null
                return result
            } catch (e: Throwable) {
                val cause = e.cause ?: e
                lastInjectError =
                    "${candidate.name}: ${cause.javaClass.simpleName}: ${cause.message}"
                Log.w(TAG, "Enjeksiyon başarısız -> $lastInjectError")
            }
        }
        return false
    }

    private fun describeInjector(): String {
        val names = injectors.map { it.name }
        return if (names.isEmpty()) {
            "YOK: sistem giriş enjeksiyon yolu bulunamadı" +
                (lastInjectError?.let { " ($it)" } ?: "")
        } else {
            "OK: yol=${workingInjector?.name ?: names.first()} adaylar=$names" +
                (lastInjectError?.let { " sonHata=$it" } ?: "")
        }
    }

    init {
        attachInterface(null, DESCRIPTOR)
        Log.i(TAG, "FlexiPad Shizuku UserService başladı")
    }

    override fun onTransact(
        code: Int,
        data: Parcel,
        reply: Parcel?,
        flags: Int
    ): Boolean {

        if (code == INTERFACE_TRANSACTION) {
            reply?.writeString(DESCRIPTOR)
            return true
        }

        if (code == TRANSACTION_PING) {
            reply?.writeNoException()
            reply?.writeInt(1)
            return true
        }

        if (code == TRANSACTION_EXECUTE) {
            data.enforceInterface(DESCRIPTOR)

            val command = data.readString() ?: ""

            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("sh", "-c", command)
                )

                val exitCode = process.waitFor()

                reply?.writeNoException()
                reply?.writeInt(exitCode)

            } catch (e: Throwable) {
                Log.e(TAG, "Komut çalıştırılamadı", e)

                reply?.writeNoException()
                reply?.writeInt(-1)
            }

            return true
        }

        if (code == TRANSACTION_TOUCH) {
            data.enforceInterface(DESCRIPTOR)

            val pointerId = data.readInt()
            val action = data.readInt()
            val x = data.readFloat()
            val y = data.readFloat()

            val result = injectTouch(
                pointerId = pointerId,
                action = action,
                x = x,
                y = y
            )

            reply?.writeNoException()
            reply?.writeInt(if (result) 1 else 0)

            return true
        }

        if (code == TRANSACTION_INFO) {
            data.enforceInterface(DESCRIPTOR)
            reply?.writeNoException()
            reply?.writeString(
                try {
                    describeInjector()
                } catch (e: Throwable) {
                    "YOK: ${e.javaClass.simpleName}: ${e.message}"
                }
            )
            return true
        }

        if (code == TRANSACTION_KEY) {
            data.enforceInterface(DESCRIPTOR)

            val keycode = data.readInt()
            val keyAction = data.readInt() // 0 = DOWN, 1 = UP
            val source = data.readInt()

            val result = injectKey(keycode, keyAction, source)

            reply?.writeNoException()
            reply?.writeInt(if (result) 1 else 0)

            return true
        }

        if (code == TRANSACTION_DESTROY) {
            reply?.writeNoException()
            destroy()
            return true
        }

        return super.onTransact(code, data, reply, flags)
    }

    private fun injectKey(keycode: Int, keyAction: Int, source: Int): Boolean {
        return try {
            val now = SystemClock.uptimeMillis()
            val isDown = keyAction == 0

            if (isDown) heldKeys.add(keycode) else heldKeys.remove(keycode)

            val event = KeyEvent(
                now,
                now,
                if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                keycode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                source
            )

            injectEvent(event)
        } catch (e: Throwable) {
            Log.e(TAG, "Key injection failed keycode=$keycode", e)
            false
        }
    }

    private fun releaseEverything() {
        synchronized(touchLock) {
            val ids = pointers.keys.sortedDescending()
            for (id in ids) {
                injectTouch(id, TOUCH_UP, pointers[id]?.x ?: 0f, pointers[id]?.y ?: 0f)
            }
            pointers.clear()
        }
        heldKeys.toList().forEach { injectKey(it, 1, InputDevice.SOURCE_KEYBOARD) }
        heldKeys.clear()
    }

    private fun injectTouch(
        pointerId: Int,
        action: Int,
        x: Float,
        y: Float
    ): Boolean = synchronized(touchLock) {
        return try {
            when (action) {
                TOUCH_DOWN -> {
                    if (pointers.isEmpty()) {
                        gestureDownTime = SystemClock.uptimeMillis()
                        pointers[pointerId] = PointerState(pointerId, x, y)

                        injectMotionEvent(MotionEvent.ACTION_DOWN)
                    } else {
                        pointers[pointerId] = PointerState(pointerId, x, y)

                        val index = pointers.keys.sorted().indexOf(pointerId)

                        injectMotionEvent(
                            MotionEvent.ACTION_POINTER_DOWN or
                                (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                        )
                    }
                }

                TOUCH_MOVE -> {
                    val pointer = pointers[pointerId]
                        ?: return false

                    pointer.x = x
                    pointer.y = y

                    injectMotionEvent(MotionEvent.ACTION_MOVE)
                }

                TOUCH_UP -> {
                    val pointer = pointers[pointerId]
                        ?: return false

                    pointer.x = x
                    pointer.y = y

                    val sortedIds = pointers.keys.sorted()
                    val index = sortedIds.indexOf(pointerId)
                    val isLast = pointers.size == 1

                    val actionCode =
                        if (isLast) {
                            MotionEvent.ACTION_UP
                        } else {
                            MotionEvent.ACTION_POINTER_UP or
                                (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                        }

                    val result = injectMotionEvent(actionCode)

                    pointers.remove(pointerId)

                    if (pointers.isEmpty()) {
                        gestureDownTime = 0L
                    }

                    result
                }

                else -> false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Touch injection failed", e)
            false
        }
    }

    private fun injectMotionEvent(action: Int): Boolean {

        val sortedPointers =
            pointers.values.sortedBy { it.pointerId }

        if (sortedPointers.isEmpty()) {
            return false
        }

        val pointerProperties =
            Array(sortedPointers.size) {
                MotionEvent.PointerProperties()
            }

        val pointerCoords =
            Array(sortedPointers.size) {
                MotionEvent.PointerCoords()
            }

        sortedPointers.forEachIndexed { index, pointer ->

            pointerProperties[index].id =
                pointer.pointerId

            pointerProperties[index].toolType =
                MotionEvent.TOOL_TYPE_FINGER

            pointerCoords[index].x =
                pointer.x

            pointerCoords[index].y =
                pointer.y

            pointerCoords[index].pressure =
                1f

            pointerCoords[index].size =
                1f
        }

        val now = SystemClock.uptimeMillis()
        val downTime =
            if (gestureDownTime != 0L) gestureDownTime else now

        val event = MotionEvent.obtain(
            downTime,
            now,
            action,
            sortedPointers.size,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )

        return try {

            injectEvent(event)

        } finally {
            event.recycle()
        }
    }

    fun destroy() {
        try {
            releaseEverything()
        } catch (_: Throwable) {
        }
        pointers.clear()

        Log.i(
            TAG,
            "FlexiPad Shizuku UserService durduruluyor"
        )

        System.exit(0)
    }
}
