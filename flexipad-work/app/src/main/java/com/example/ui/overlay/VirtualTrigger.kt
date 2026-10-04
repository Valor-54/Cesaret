package com.example.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Virtual Analog Trigger (LT / RT):
 * Displays strictly the trigger name and analog pull percentage.
 */
@Composable
fun VirtualTrigger(
    modifier: Modifier = Modifier,
    sizeDp: Float = 68f,
    label: String = "RT",
    subLabel: String = "",
    color: Color = Color(0xFFF44336),
    opacity: Float = 0.8f,
    isEditMode: Boolean = false,
    testTag: String = "gamepad_trigger",
    onTriggerChanged: (pressure: Float) -> Unit = {}
) {
    var pressure by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(width = (sizeDp * 1.15f).dp, height = sizeDp.dp)
            .testTag(testTag)
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        down.consume()

                        pressure = 1.0f
                        onTriggerChanged(1.0f)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    change?.consume()
                                    break
                                }
                                change.consume()
                                val p = (change.position.y / size.height).coerceIn(0.2f, 1f)
                                pressure = p
                                onTriggerChanged(p)
                            }
                        } finally {
                            pressure = 0f
                            onTriggerChanged(0f)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            // Base Background
            drawRoundRect(
                color = Color(0xFF14181C).copy(alpha = opacity * 0.7f),
                cornerRadius = corner
            )

            // Pressure Gauge Fill
            if (pressure > 0.05f) {
                val fillHeight = size.height * pressure
                drawRoundRect(
                    color = color.copy(alpha = (opacity * 0.85f).coerceIn(0f, 1f)),
                    topLeft = Offset(0f, size.height - fillHeight),
                    size = Size(size.width, fillHeight),
                    cornerRadius = corner
                )
            }

            // Outer Frame
            drawRoundRect(
                color = color.copy(alpha = opacity),
                cornerRadius = corner,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = (sizeDp * 0.28f).sp,
                fontWeight = FontWeight.Black
            )
            if (pressure > 0.05f) {
                Text(
                    text = "${(pressure * 100).toInt()}%",
                    color = Color.Yellow,
                    fontSize = (sizeDp * 0.16f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
