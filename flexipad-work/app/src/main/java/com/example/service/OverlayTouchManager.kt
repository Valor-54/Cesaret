package com.example.service

import android.graphics.Rect
import android.graphics.Region
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.example.model.ButtonShape
import com.example.model.GamepadProfile
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Manages the precise touchable region for the system overlay window.
 * Ensures that touch events are ONLY captured by visible virtual buttons,
 * the joystick, and the floating quick bubble.
 * All transparent/empty screen areas pass touch events directly through
 * to underlying applications.
 */
class OverlayTouchManager(
    private val overlayView: View,
    private val getCurrentProfile: () -> GamepadProfile?,
    private val isEditMode: () -> Boolean,
    private val isHudVisible: () -> Boolean,
    private val getLiveDragOffsets: () -> Map<String, Pair<Float, Float>> = { emptyMap() }
) {
    private val TAG = "OverlayTouchManager"
    private var internalInsetsListener: Any? = null
    private var addMethod: Method? = null
    private var removeMethod: Method? = null
    private var setTouchableInsetsMethod: Method? = null
    private var touchableRegionField: Field? = null

    init {
        unsealHiddenApis()
        setupReflection()
    }

    private fun unsealHiddenApis() {
        try {
            val forNameMethod = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod",
                String::class.java,
                arrayOf<Class<*>>()::class.java
            )
            val vmRuntimeClass = forNameMethod.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntimeMethod = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "getRuntime",
                null
            ) as Method
            val vmRuntime = getRuntimeMethod.invoke(null)
            val setHiddenApiExemptionsMethod = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                arrayOf<Class<*>>(Array<String>::class.java)
            ) as Method
            setHiddenApiExemptionsMethod.invoke(vmRuntime, arrayOf(arrayOf("L")))
            Log.d(TAG, "Hidden API restrictions exempted successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "Reflection unseal not supported or already exempt", e)
        }
    }

    private fun setupReflection() {
        try {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val insetsInfoClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")

            addMethod = ViewTreeObserver::class.java.getDeclaredMethod("addOnComputeInternalInsetsListener", listenerClass).apply { isAccessible = true }
            removeMethod = ViewTreeObserver::class.java.getDeclaredMethod("removeOnComputeInternalInsetsListener", listenerClass).apply { isAccessible = true }
            setTouchableInsetsMethod = insetsInfoClass.getDeclaredMethod("setTouchableInsets", Int::class.javaPrimitiveType).apply { isAccessible = true }
            touchableRegionField = insetsInfoClass.getDeclaredField("touchableRegion").apply { isAccessible = true }

            internalInsetsListener = Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                    computeInsets(args[0])
                }
                null
            }
            Log.d(TAG, "OnComputeInternalInsetsListener reflection setup succeeded")
        } catch (e: Throwable) {
            Log.w(TAG, "Reflection for OnComputeInternalInsetsListener not available", e)
        }
    }

    fun attach() {
        try {
            val vto = overlayView.viewTreeObserver
            if (vto.isAlive && addMethod != null && internalInsetsListener != null) {
                addMethod?.invoke(vto, internalInsetsListener)
                Log.d(TAG, "OnComputeInternalInsetsListener attached successfully")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to attach OnComputeInternalInsetsListener", e)
        }
    }

    fun detach() {
        try {
            val vto = overlayView.viewTreeObserver
            if (vto.isAlive && removeMethod != null && internalInsetsListener != null) {
                removeMethod?.invoke(vto, internalInsetsListener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to detach OnComputeInternalInsetsListener", e)
        }
    }

    fun requestUpdate() {
        overlayView.post {
            overlayView.requestLayout()
        }
    }

    private fun computeInsets(insetsInfo: Any) {
        try {
            // TOUCHABLE_INSETS_REGION = 3
            setTouchableInsetsMethod?.invoke(insetsInfo, 3)
            val region = touchableRegionField?.get(insetsInfo) as? Region ?: return
            region.setEmpty()

            val w = overlayView.width
            val h = overlayView.height
            if (w <= 0 || h <= 0) return

            val density = overlayView.resources.displayMetrics.density

            if (!isHudVisible()) {
                val bubbleRect = Rect(
                    (10 * density).toInt(),
                    (10 * density).toInt(),
                    (58 * density).toInt(),
                    (58 * density).toInt()
                )
                region.set(bubbleRect)
                return
            }

            if (isEditMode()) {
                region.op(
                    Rect((10 * density).toInt(), (10 * density).toInt(), (58 * density).toInt(), (58 * density).toInt()),
                    Region.Op.UNION
                )
                val topBannerHeight = (54 * density).toInt()
                region.op(Rect((w * 0.2f).toInt(), 0, (w * 0.8f).toInt(), topBannerHeight), Region.Op.UNION)
                val bottomBarHeight = (120 * density).toInt()
                region.op(Rect((w * 0.15f).toInt(), h - bottomBarHeight, (w * 0.85f).toInt(), h), Region.Op.UNION)

                val profile = getCurrentProfile() ?: return
                val liveOffsets = getLiveDragOffsets()
                for (item in profile.controls) {
                    val effX = liveOffsets[item.id]?.first ?: item.xPercent
                    val effY = liveOffsets[item.id]?.second ?: item.yPercent
                    val cx = w * effX
                    val cy = h * effY
                    val widthPx = (if (item.shape == ButtonShape.PILL) item.sizeDp * 1.35f else item.sizeDp) * density
                    val heightPx = item.sizeDp * density
                    val pad = 4 * density
                    val rect = Rect(
                        (cx - widthPx / 2f - pad).toInt().coerceAtLeast(0),
                        (cy - heightPx / 2f - pad).toInt().coerceAtLeast(0),
                        (cx + widthPx / 2f + pad).toInt().coerceAtMost(w),
                        (cy + heightPx / 2f + pad).toInt().coerceAtMost(h)
                    )
                    region.op(rect, Region.Op.UNION)
                }
            } else {
                region.op(
                    Rect((10 * density).toInt(), (10 * density).toInt(), (58 * density).toInt(), (58 * density).toInt()),
                    Region.Op.UNION
                )
                val profile = getCurrentProfile() ?: return
                for (item in profile.controls) {
                    if (item.isVisible) {
                        val cx = w * item.xPercent
                        val cy = h * item.yPercent
                        val widthPx = (if (item.shape == ButtonShape.PILL) item.sizeDp * 1.35f else item.sizeDp) * density
                        val heightPx = item.sizeDp * density
                        val rect = Rect(
                            (cx - widthPx / 2f).toInt().coerceAtLeast(0),
                            (cy - heightPx / 2f).toInt().coerceAtLeast(0),
                            (cx + widthPx / 2f).toInt().coerceAtMost(w),
                            (cy + heightPx / 2f).toInt().coerceAtMost(h)
                        )
                        region.op(rect, Region.Op.UNION)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error calculating touchable region", e)
        }
    }

    fun isPointInsideTouchableControl(x: Float, y: Float): Boolean {
        val w = overlayView.width
        val h = overlayView.height
        if (w <= 0 || h <= 0) return false
        val density = overlayView.resources.displayMetrics.density

        if (x in (10 * density)..(58 * density) && y in (10 * density)..(58 * density)) return true
        if (!isHudVisible()) return false

        val profile = getCurrentProfile() ?: return false
        val liveOffsets = if (isEditMode()) getLiveDragOffsets() else emptyMap()

        for (item in profile.controls) {
            if (item.isVisible || isEditMode()) {
                val effX = liveOffsets[item.id]?.first ?: item.xPercent
                val effY = liveOffsets[item.id]?.second ?: item.yPercent
                val cx = w * effX
                val cy = h * effY
                val widthPx = (if (item.shape == ButtonShape.PILL) item.sizeDp * 1.35f else item.sizeDp) * density
                val heightPx = item.sizeDp * density
                val pad = if (isEditMode()) 4 * density else 0f
                if (x >= (cx - widthPx / 2f - pad) &&
                    x <= (cx + widthPx / 2f + pad) &&
                    y >= (cy - heightPx / 2f - pad) &&
                    y <= (cy + heightPx / 2f + pad)
                ) {
                    return true
                }
            }
        }
        return false
    }
}
