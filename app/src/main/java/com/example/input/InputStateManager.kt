package com.example.input

import android.util.Log
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized Input State Manager:
 * - Tracks every active on-screen control instance and associated pointer ID.
 * - Guarantees DOWN -> HOLD -> UP lifecycle.
 * - Employs pointer-to-control mapping and reference counting so lifting one finger
 *   never accidentally releases another finger's active input.
 * - Safe release mechanism for CANCEL, service destruction, rotation, and overlay hide.
 */
class InputStateManager(
    private val onInputDown: (item: GamepadControlItem, pointerId: Int) -> Unit,
    private val onInputUp: (item: GamepadControlItem, pointerId: Int) -> Unit
) {
    private val TAG = "InputStateManager"

    data class ActiveTouch(
        val controlId: String,
        val item: GamepadControlItem,
        val pointerId: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Maps pointerId -> ActiveTouch
    private val activeByPointer = ConcurrentHashMap<Int, ActiveTouch>()
    // Maps controlId -> Set of pointerIds pressing it (reference count)
    private val activePointersByControl = ConcurrentHashMap<String, MutableSet<Int>>()

    /**
     * Handles touch DOWN for a specific control and pointer ID.
     */
    @Synchronized
    fun onPointerDown(item: GamepadControlItem, pointerId: Int) {
        val pointerSet = activePointersByControl.getOrPut(item.id) { ConcurrentHashMap.newKeySet() }
        val wasEmpty = pointerSet.isEmpty()
        pointerSet.add(pointerId)

        val activeTouch = ActiveTouch(
            controlId = item.id,
            item = item,
            pointerId = pointerId
        )
        activeByPointer[pointerId] = activeTouch

        Log.d(TAG, "Touch DOWN: [${item.label}] (Pointer: $pointerId, Active Count: ${pointerSet.size})")

        // Only fire DOWN to input pipeline on initial press
        if (wasEmpty) {
            try {
                onInputDown(item, pointerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onInputDown for ${item.label}", e)
            }
        }
    }

    /**
     * Handles touch UP for a specific pointer ID.
     */
    @Synchronized
    fun onPointerUp(pointerId: Int) {
        val touch = activeByPointer.remove(pointerId) ?: return
        val pointerSet = activePointersByControl[touch.controlId]
        pointerSet?.remove(pointerId)

        val isNowReleased = pointerSet.isNullOrEmpty()
        if (isNowReleased) {
            activePointersByControl.remove(touch.controlId)
        }

        Log.d(TAG, "Touch UP: [${touch.item.label}] (Pointer: $pointerId, Remaining on control: ${pointerSet?.size ?: 0})")

        // Only fire UP to input pipeline when all fingers leave this control
        if (isNowReleased) {
            try {
                onInputUp(touch.item, pointerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onInputUp for ${touch.item.label}", e)
            }
        }
    }

    /**
     * Safely releases all active touches (called on rotation, service stop, activity destroy).
     */
    @Synchronized
    fun releaseAll() {
        val activeList = activeByPointer.values.toList()
        for (touch in activeList) {
            try {
                onInputUp(touch.item, touch.pointerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing touch for ${touch.item.label}", e)
            }
        }
        activeByPointer.clear()
        activePointersByControl.clear()
        Log.i(TAG, "All active input touches safely released")
    }

    fun isControlActive(controlId: String): Boolean {
        return activePointersByControl[controlId]?.isNotEmpty() == true
    }

    val activeCount: Int
        get() = activeByPointer.size
}
