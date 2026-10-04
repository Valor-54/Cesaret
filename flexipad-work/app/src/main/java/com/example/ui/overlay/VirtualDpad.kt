package com.example.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamepadButtonType
import kotlin.math.atan2

@Composable
fun VirtualDpad(
    modifier: Modifier = Modifier,
    sizeDp: Float = 115f,
    color: Color = Color(0xFF9E9E9E),
    opacity: Float = 0.65f,
    isEditMode: Boolean = false,
    onDirectionChanged: (type: GamepadButtonType, isPressed: Boolean) -> Unit = { _, _ -> }
) {
    var activeDirection by remember { mutableStateOf<GamepadButtonType?>(null) }

    fun updateDirection(offset: Offset, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val dx = offset.x - cx
        val dy = offset.y - cy
        val distSq = dx * dx + dy * dy
        val deadzoneSq = (width * 0.15f) * (width * 0.15f)

        if (distSq < deadzoneSq) {
            activeDirection?.let { onDirectionChanged(it, false) }
            activeDirection = null
            return
        }

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val newDir = when {
            angle in -45f..45f -> GamepadButtonType.DPAD_RIGHT
            angle in 45f..135f -> GamepadButtonType.DPAD_DOWN
            angle in -135f..-45f -> GamepadButtonType.DPAD_UP
            else -> GamepadButtonType.DPAD_LEFT
        }

        if (newDir != activeDirection) {
            activeDirection?.let { onDirectionChanged(it, false) }
            activeDirection = newDir
            onDirectionChanged(newDir, true)
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("gamepad_dpad")
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        down.consume()

                        updateDirection(down.position, size.width.toFloat(), size.height.toFloat())
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                            updateDirection(change.position, size.width.toFloat(), size.height.toFloat())
                        }
                        activeDirection?.let { onDirectionChanged(it, false) }
                        activeDirection = null
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val armWidth = w * 0.34f
            val armLength = (w - armWidth) / 2f

            val path = Path().apply {
                moveTo(armLength, 0f)
                lineTo(armLength + armWidth, 0f)
                lineTo(armLength + armWidth, armLength)
                lineTo(w, armLength)
                lineTo(w, armLength + armWidth)
                lineTo(armLength + armWidth, armLength + armWidth)
                lineTo(armLength + armWidth, h)
                lineTo(armLength, h)
                lineTo(armLength, armLength + armWidth)
                lineTo(0f, armLength + armWidth)
                lineTo(0f, armLength)
                lineTo(armLength, armLength)
                close()
            }

            drawPath(
                path = path,
                color = Color(0xFF14171A).copy(alpha = opacity * 0.7f)
            )
            drawPath(
                path = path,
                color = color.copy(alpha = opacity),
                style = Stroke(width = 2.dp.toPx())
            )

            activeDirection?.let { dir ->
                val highlightColor = Color.White.copy(alpha = 0.5f)
                when (dir) {
                    GamepadButtonType.DPAD_UP -> drawRect(
                        color = highlightColor,
                        topLeft = Offset(armLength, 0f),
                        size = androidx.compose.ui.geometry.Size(armWidth, armLength)
                    )
                    GamepadButtonType.DPAD_DOWN -> drawRect(
                        color = highlightColor,
                        topLeft = Offset(armLength, armLength + armWidth),
                        size = androidx.compose.ui.geometry.Size(armWidth, armLength)
                    )
                    GamepadButtonType.DPAD_LEFT -> drawRect(
                        color = highlightColor,
                        topLeft = Offset(0f, armLength),
                        size = androidx.compose.ui.geometry.Size(armLength, armWidth)
                    )
                    GamepadButtonType.DPAD_RIGHT -> drawRect(
                        color = highlightColor,
                        topLeft = Offset(armLength + armWidth, armLength),
                        size = androidx.compose.ui.geometry.Size(armLength, armWidth)
                    )
                    else -> Unit
                }
            }
        }

        Text(
            text = "D-PAD",
            color = Color.White.copy(alpha = opacity * 0.8f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
