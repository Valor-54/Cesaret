package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * FlexiPad Accessibility Service:
 * Provides authentic Android system-level touch injection into any background game
 * (including GForce, GeForce NOW, and native games) using Android's official
 * dispatchGesture framework (Android 7.0+ / 8.0+ API 26+).
 *
 * Implements DOWN -> HOLD -> UP lifecycle:
 * - DOWN: dispatches initial touch stroke with willContinue = true
 * - HOLD: keeps touch actively pressed on the target game window
 * - UP: releases touch stroke cleanly with willContinue = false
 * - CANCEL / RELEASE: releases all active pointers safely to prevent sticky buttons
 */
class FlexiPadAccessibilityService : AccessibilityService() {
    private val TAG = "FlexiPadAccessibility"

    // Tracks ongoing active touch strokes by pointerId
    private val activeStrokes = ConcurrentHashMap<Int, ActivePointerStroke>()
    private val scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2)

    data class ActivePointerStroke(
        val pointerId: Int,
        var currentX: Float,
        var currentY: Float,
        var strokeDescription: GestureDescription.StrokeDescription? = null,
        var isHeld: Boolean = true,
        var holdJob: java.util.concurrent.ScheduledFuture<*>? = null
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.i(TAG, "FlexiPad Accessibility Service connected and ready for system gesture injection")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive monitoring
    }

    override fun onInterrupt() {
        Log.w(TAG, "FlexiPad Accessibility Service interrupted")
        releaseAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAll()
        scheduler.shutdownNow()
        instance = null
        _isServiceActive.value = false
        Log.i(TAG, "FlexiPad Accessibility Service destroyed")
    }

    /**
     * Injects a real touch DOWN event at screen coordinates (screenX, screenY).
     * On Android 8.0+ (API 26+), willContinue = true keeps the touch held down on the target game!
     */
    fun injectTouchDown(pointerId: Int, screenX: Float, screenY: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val path = Path().apply {
            moveTo(screenX, screenY)
            lineTo(screenX + 0.1f, screenY + 0.1f)
        }

        try {
            val stroke = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                GestureDescription.StrokeDescription(path, 0, 100, true)
            } else {
                GestureDescription.StrokeDescription(path, 0, 100)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(stroke)
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.v(TAG, "Pointer $pointerId DOWN injected at ($screenX, $screenY)")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Pointer $pointerId DOWN cancelled")
                }
            }, null)

            // Periodic continuation to keep touch genuinely HELD without timeout
            val holdJob = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduler.scheduleWithFixedDelay({
                    val active = activeStrokes[pointerId] ?: return@scheduleWithFixedDelay
                    if (!active.isHeld) return@scheduleWithFixedDelay
                    val contPath = Path().apply {
                        moveTo(active.currentX, active.currentY)
                        lineTo(active.currentX, active.currentY)
                    }
                    try {
                        val nextStroke = try {
                            active.strokeDescription?.continueStroke(contPath, 0, 90, true)
                                ?: GestureDescription.StrokeDescription(contPath, 0, 90, true)
                        } catch (e: Exception) {
                            GestureDescription.StrokeDescription(contPath, 0, 90, true)
                        }
                        val contGesture = GestureDescription.Builder().addStroke(nextStroke).build()
                        dispatchGesture(contGesture, null, null)
                        active.strokeDescription = nextStroke
                    } catch (ignored: Exception) {
                    }
                }, 75, 75, java.util.concurrent.TimeUnit.MILLISECONDS)
            } else {
                null
            }

            activeStrokes[pointerId] = ActivePointerStroke(
                pointerId = pointerId,
                currentX = screenX,
                currentY = screenY,
                strokeDescription = stroke,
                isHeld = true,
                holdJob = holdJob
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting touch DOWN for pointer $pointerId", e)
        }
    }

    /**
     * Injects a touch MOVE event (e.g. for analog joysticks or dragging)
     */
    fun injectTouchMove(pointerId: Int, screenX: Float, screenY: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val active = activeStrokes[pointerId] ?: return

        val path = Path().apply {
            moveTo(active.currentX, active.currentY)
            lineTo(screenX, screenY)
        }

        try {
            val prevStroke = active.strokeDescription
            val nextStroke = if (prevStroke != null) {
                try {
                    prevStroke.continueStroke(path, 0, 80, true)
                } catch (e: Exception) {
                    GestureDescription.StrokeDescription(path, 0, 80, true)
                }
            } else {
                GestureDescription.StrokeDescription(path, 0, 80, true)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(nextStroke)
                .build()

            dispatchGesture(gesture, null, null)
            active.currentX = screenX
            active.currentY = screenY
            active.strokeDescription = nextStroke
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting touch MOVE for pointer $pointerId", e)
        }
    }

    /**
     * Injects a real touch UP / RELEASE event at screen coordinates.
     * Completes the touch lifecycle cleanly, releasing the button in the game.
     */
    fun injectTouchUp(pointerId: Int, screenX: Float? = null, screenY: Float? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val active = activeStrokes.remove(pointerId)
        active?.holdJob?.cancel(true)
        active?.isHeld = false

        val x = screenX ?: active?.currentX ?: return
        val y = screenY ?: active?.currentY ?: return

        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }

        try {
            val stroke = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && active?.strokeDescription != null) {
                try {
                    active.strokeDescription!!.continueStroke(path, 0, 20, false)
                } catch (e: Exception) {
                    GestureDescription.StrokeDescription(path, 0, 20, false)
                }
            } else {
                GestureDescription.StrokeDescription(path, 0, 20, false)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(stroke)
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.v(TAG, "Pointer $pointerId UP injected at ($x, $y)")
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting touch UP for pointer $pointerId", e)
        }
    }

    /**
     * Injects a quick discrete tap at (screenX, screenY)
     */
    fun injectTap(screenX: Float, screenY: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path().apply {
            moveTo(screenX, screenY)
            lineTo(screenX + 0.1f, screenY + 0.1f)
        }
        try {
            val stroke = GestureDescription.StrokeDescription(path, 0, 50, false)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting tap at ($screenX, $screenY)", e)
        }
    }

    /**
     * Safely releases all active touches on service stop, rotation, or overlay close.
     */
    fun releaseAll() {
        val keys = activeStrokes.keys.toList()
        for (pointerId in keys) {
            injectTouchUp(pointerId)
        }
        activeStrokes.clear()
    }

    companion object {
        var instance: FlexiPadAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun isConnected(): Boolean {
            return instance != null && _isServiceActive.value
        }
    }
}
