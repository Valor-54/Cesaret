package com.example.shizuku

import android.hardware.input.InputManager
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
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
    private var gestureDownTime = 0L

    private val inputManager: InputManager by lazy {
        val method = InputManager::class.java.getDeclaredMethod("getInstance")
        method.isAccessible = true
        method.invoke(null) as InputManager
    }

    private val injectMethod: Method by lazy {
        InputManager::class.java.getDeclaredMethod(
            "injectInputEvent",
            android.view.InputEvent::class.java,
            Int::class.javaPrimitiveType
        ).apply {
            isAccessible = true
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

        return super.onTransact(code, data, reply, flags)
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

            injectMethod.invoke(
                inputManager,
                event,
                0
            ) as Boolean

        } finally {
            event.recycle()
        }
    }

    fun destroy() {
        pointers.clear()

        Log.i(
            TAG,
            "FlexiPad Shizuku UserService durduruluyor"
        )

        System.exit(0)
    }
}
