package com.example.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ButtonShape

/**
 * Virtual Gamepad Button:
 * - Implements genuine DOWN -> HOLD -> UP touch lifecycle.
 * - Strictly displays the assigned input label (A, B, X, Y, LB, RB, LT, RT, SPACE, W, etc.)
 *   without any action descriptions on the button face.
 * - Multi-touch enabled: each button tracks its own pointer ID independently.
 */
@Composable
fun VirtualButton(
    modifier: Modifier = Modifier,
    sizeDp: Float = 60f,
    label: String,
    subLabel: String = "",
    color: Color = Color(0xFF4CAF50),
    textColor: Color = Color.White,
    borderColor: Color = color,
    backgroundColor: Color = Color(0xFF16191D),
    shape: ButtonShape = ButtonShape.ROUNDED,
    fontSizeSp: Float = 14f,
    cornerRadiusDp: Float = 16f,
    opacity: Float = 0.75f,
    isEditMode: Boolean = false,
    isPressedState: Boolean = false,
    testTag: String = "gamepad_button",
    onPressChanged: (Boolean) -> Unit = {}
) {
    var isPressedLocal by remember { mutableStateOf(false) }
    val effectivePressed = isPressedLocal || isPressedState

    val scale by animateFloatAsState(
        targetValue = if (effectivePressed) 0.90f else 1.0f,
        label = "btn_scale"
    )

    val widthDp = if (shape == ButtonShape.PILL) sizeDp * 1.35f else sizeDp
    val heightDp = sizeDp

    Box(
        modifier = modifier
            .size(width = widthDp.dp, height = heightDp.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag)
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        down.consume()
                        isPressedLocal = true
                        onPressChanged(true)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    change?.consume()
                                    break
                                }
                                change.consume()
                            }
                        } finally {
                            isPressedLocal = false
                            onPressChanged(false)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val corner = when (shape) {
                ButtonShape.CIRCLE -> CornerRadius(w / 2f, h / 2f)
                ButtonShape.SQUARE -> CornerRadius(4.dp.toPx(), 4.dp.toPx())
                ButtonShape.PILL -> CornerRadius(h / 2f, h / 2f)
                ButtonShape.ROUNDED -> CornerRadius(cornerRadiusDp.dp.toPx(), cornerRadiusDp.dp.toPx())
            }

            // Button body background
            drawRoundRect(
                color = if (effectivePressed) {
                    color.copy(alpha = (opacity * 0.95f).coerceIn(0f, 1f))
                } else {
                    backgroundColor.copy(alpha = (opacity * 0.70f).coerceIn(0f, 1f))
                },
                cornerRadius = corner
            )

            // Glowing border
            drawRoundRect(
                color = if (effectivePressed) Color.White else borderColor.copy(alpha = opacity),
                cornerRadius = corner,
                style = Stroke(width = if (effectivePressed) 3.dp.toPx() else 2.dp.toPx())
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            val dynamicFontSize = if (fontSizeSp > 0f) {
                fontSizeSp.sp
            } else {
                if (label.length > 3) (sizeDp * 0.22f).sp else (sizeDp * 0.32f).sp
            }

            // Strictly show assigned input name
            Text(
                text = label,
                color = if (effectivePressed) Color.White else textColor.copy(alpha = opacity.coerceIn(0.6f, 1f)),
                fontSize = dynamicFontSize,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}
