package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.GamepadInputManager
import com.example.model.ButtonShape
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import com.example.model.GamepadProfile
import com.example.ui.dialogs.AddKeyDialog
import com.example.ui.dialogs.ButtonDesignDialog
import com.example.ui.dialogs.JoystickSettingsDialog
import com.example.ui.dialogs.KeyBindDialog
import com.example.ui.theme.CombatRed
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.TacticalAmber
import kotlin.math.roundToInt

@Composable
fun DualZoneOverlayHud(
    modifier: Modifier = Modifier,
    profile: GamepadProfile,
    inputManager: GamepadInputManager,
    isEditMode: Boolean,
    isHudVisible: Boolean,
    onToggleEditMode: () -> Unit,
    onToggleHudVisibility: () -> Unit,
    onOpenApp: () -> Unit,
    onStopService: () -> Unit,
    onUpdateControlItem: (GamepadControlItem) -> Unit = {},
    onAddControlItem: (GamepadControlItem) -> Unit = {},
    onDeleteControlItem: (String) -> Unit = {},
    onUpdateProfile: (GamepadProfile) -> Unit = {},
    onLiveOffsetsChanged: (Map<String, Pair<Float, Float>>) -> Unit = {}
) {
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<GamepadControlItem?>(null) }
    var showDesignDialog by remember { mutableStateOf(false) }
    var showKeyBindDialog by remember { mutableStateOf(false) }
    var showJoystickDialog by remember { mutableStateOf(false) }

    var liveDragOffsets by remember { mutableStateOf<Map<String, Pair<Float, Float>>>(emptyMap()) }
    var debugDragStatus by remember { mutableStateOf("DÜZENLEME: Taşımak için herhangi bir tuş veya joystick'e dokunup sürükleyin") }

    if (!isHudVisible) {
        // Only show the tiny floating bubble when HUD is hidden
        Box(modifier = modifier.fillMaxSize()) {
            FloatingQuickBubble(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                isEditMode = isEditMode,
                isHudVisible = false,
                onToggleEditMode = onToggleEditMode,
                onToggleHudVisibility = onToggleHudVisibility,
                onAddNewKey = { showAddKeyDialog = true },
                onOpenApp = onOpenApp,
                onStopService = onStopService
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("overlay_hud_root")) {
        val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Center Aim Boundary Guide when in Edit Mode
        if (isEditMode && profile.centerAimFreeZone) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .fillMaxHeight(0.70f)
                    .align(Alignment.Center)
            ) {
                val corner = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                drawRoundRect(
                    color = ScopeCyan.copy(alpha = 0.04f),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = ScopeCyan.copy(alpha = 0.25f),
                    cornerRadius = corner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Live Drag & Touch Debug Status Bar
        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .testTag("drag_debug_banner"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEE161B22),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (debugDragStatus.contains("HAREKET")) ScopeCyan else TacticalAmber
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (debugDragStatus.contains("HAREKET")) ElectricGreen
                                else if (debugDragStatus.contains("BAŞLADI")) TacticalAmber
                                else ScopeCyan,
                                CircleShape
                            )
                    )
                    Text(
                        text = debugDragStatus,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // Render each active control button / joystick
        for (item in profile.controls) {
            if (item.isVisible || isEditMode) {
                key(item.id) {
                    val effectiveXPercent = liveDragOffsets[item.id]?.first ?: item.xPercent
                    val effectiveYPercent = liveDragOffsets[item.id]?.second ?: item.yPercent
                    val isPill = item.shape == ButtonShape.PILL
                    val widthDp = if (isPill) item.sizeDp * 1.35f else item.sizeDp
                    val heightDp = item.sizeDp
                    val xOffset = (screenWidth * effectiveXPercent) - (widthDp.dp / 2f)
                    val yOffset = (screenHeight * effectiveYPercent) - (heightDp.dp / 2f)

                    val isSelected = selectedItemForEdit?.id == item.id

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    xOffset.toPx().roundToInt(),
                                    yOffset.toPx().roundToInt()
                                )
                            }
                            .size(widthDp.dp, heightDp.dp)
                            .then(
                                if (isEditMode && isSelected) {
                                    Modifier.border(2.dp, TacticalAmber, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (isEditMode) {
                                    // SÜRÜKLEME VE DÜZENLEME: Tüm tuşlar ve joystickler taşınabilir!
                                    Modifier.pointerInput(item.id, screenWidthPx, screenHeightPx) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            down.consume()
                                            val pointerId = down.id
                                            var currentX = effectiveXPercent
                                            var currentY = effectiveYPercent
                                            var hasMoved = false
                                            selectedItemForEdit = item
                                            debugDragStatus = "SÜRÜKLEME BAŞLADI: [${item.label}] X: ${(currentX * 100).roundToInt()}% Y: ${(currentY * 100).roundToInt()}%"

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                                if (!change.pressed) {
                                                    change.consume()
                                                    debugDragStatus = "KONUMLANDI: [${item.label}] (X: ${(currentX * 100).roundToInt()}%, Y: ${(currentY * 100).roundToInt()}%)"
                                                    if (hasMoved) {
                                                        val updatedOffsets = liveDragOffsets - item.id
                                                        liveDragOffsets = updatedOffsets
                                                        onLiveOffsetsChanged(updatedOffsets)
                                                        val updated = item.copy(xPercent = currentX, yPercent = currentY)
                                                        selectedItemForEdit = updated
                                                        onUpdateControlItem(updated)
                                                    }
                                                    break
                                                }
                                                val delta = change.positionChange()
                                                if (delta != Offset.Zero) {
                                                    change.consume()
                                                    hasMoved = true
                                                    currentX = (currentX + (delta.x / screenWidthPx)).coerceIn(0.02f, 0.98f)
                                                    currentY = (currentY + (delta.y / screenHeightPx)).coerceIn(0.02f, 0.98f)
                                                    debugDragStatus = "HAREKET EDİYOR: [${item.label}] X: ${(currentX * 100).roundToInt()}% Y: ${(currentY * 100).roundToInt()}%"
                                                    val updatedOffsets = liveDragOffsets + (item.id to Pair(currentX, currentY))
                                                    liveDragOffsets = updatedOffsets
                                                    onLiveOffsetsChanged(updatedOffsets)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        when (item.type) {
                            GamepadButtonType.JOYSTICK_LEFT -> {
                                VirtualJoystick(
                                    sizeDp = item.sizeDp,
                                    label = item.label,
                                    color = Color(item.colorHex),
                                    opacity = item.opacity,
                                    sensitivity = profile.leftStickSensitivity,
                                    deadzone = profile.leftStickDeadzone,
                                    maxTravelFactor = profile.leftStickMaxTravel,
                                    dynamicCenter = profile.dynamicJoystickCenter,
                                    isEditMode = isEditMode,
                                    onMove = { x, y ->
                                        inputManager.updateJoystick(isLeft = true, rawX = x, rawY = y)
                                    },
                                    onClickThumb = {
                                        inputManager.setButtonPressed(GamepadButtonType.BUTTON_L3, true)
                                        inputManager.setButtonPressed(GamepadButtonType.BUTTON_L3, false)
                                    }
                                )
                            }
                            GamepadButtonType.JOYSTICK_RIGHT -> {
                                VirtualJoystick(
                                    sizeDp = item.sizeDp,
                                    label = item.label,
                                    color = Color(item.colorHex),
                                    opacity = item.opacity,
                                    sensitivity = profile.rightStickSensitivity,
                                    deadzone = profile.rightStickDeadzone,
                                    maxTravelFactor = profile.rightStickMaxTravel,
                                    dynamicCenter = profile.dynamicJoystickCenter,
                                    isEditMode = isEditMode,
                                    onMove = { x, y ->
                                        inputManager.updateJoystick(isLeft = false, rawX = x, rawY = y)
                                    },
                                    onClickThumb = {
                                        inputManager.setButtonPressed(GamepadButtonType.BUTTON_R3, true)
                                        inputManager.setButtonPressed(GamepadButtonType.BUTTON_R3, false)
                                    }
                                )
                            }
                            GamepadButtonType.DPAD -> {
                                VirtualDpad(
                                    sizeDp = item.sizeDp,
                                    color = Color(item.colorHex),
                                    opacity = item.opacity,
                                    isEditMode = isEditMode,
                                    onDirectionChanged = { dir, pressed ->
                                        inputManager.setButtonPressed(dir, pressed)
                                    }
                                )
                            }
                            GamepadButtonType.BUTTON_L2, GamepadButtonType.BUTTON_R2 -> {
                                VirtualTrigger(
                                    sizeDp = item.sizeDp,
                                    label = item.label,
                                    subLabel = "",
                                    color = Color(item.colorHex),
                                    opacity = item.opacity,
                                    isEditMode = isEditMode,
                                    onTriggerChanged = { pressure ->
                                        val isLeft = item.type == GamepadButtonType.BUTTON_L2
                                        inputManager.updateTrigger(isLeft = isLeft, value = pressure)
                                    }
                                )
                            }
                            else -> {
                                VirtualButton(
                                    sizeDp = item.sizeDp,
                                    label = item.label,
                                    subLabel = "",
                                    color = Color(item.colorHex),
                                    textColor = Color(item.textColorHex),
                                    borderColor = Color(item.borderColorHex),
                                    backgroundColor = Color(item.backgroundColorHex),
                                    shape = item.shape,
                                    fontSizeSp = item.fontSizeSp,
                                    cornerRadiusDp = item.cornerRadiusDp,
                                    opacity = item.opacity,
                                    isEditMode = isEditMode,
                                    onPressChanged = { pressed ->
                                        inputManager.setCustomControlPressed(item, pressed)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Right Save & Lock Action Bar in Edit Mode
        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .testTag("save_and_lock_overlay_btn"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEE161B22),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TacticalAmber),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            onUpdateProfile(profile)
                            onToggleEditMode()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("KAYDET VE KİTLE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // Floating Quick Access Bubble (Always on top)
        FloatingQuickBubble(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            isEditMode = isEditMode,
            isHudVisible = isHudVisible,
            onToggleEditMode = onToggleEditMode,
            onToggleHudVisibility = onToggleHudVisibility,
            onAddNewKey = { showAddKeyDialog = true },
            onOpenApp = onOpenApp,
            onStopService = onStopService
        )

        // Selected Item Rich Action Bar in Edit Mode
        if (isEditMode && selectedItemForEdit != null) {
            val item = selectedItemForEdit!!
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .testTag("overlay_selected_edit_bar"),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFA12161A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Text(
                            text = "Seçilen: ${item.label}",
                            color = TacticalAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${item.sizeDp.toInt()}dp | ${(item.opacity * 100).toInt()}%",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Bind Key
                        IconButton(
                            onClick = { showKeyBindDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Tuş Ata", tint = ElectricGreen, modifier = Modifier.size(20.dp))
                        }

                        // Design (Color / Shape / Size)
                        IconButton(
                            onClick = { showDesignDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Tasarım", tint = TacticalAmber, modifier = Modifier.size(20.dp))
                        }

                        // Joystick Specific Settings
                        if (item.type == GamepadButtonType.JOYSTICK_LEFT || item.type == GamepadButtonType.JOYSTICK_RIGHT) {
                            IconButton(
                                onClick = { showJoystickDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Joystick Ayarları", tint = TacticalAmber, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Duplicate Button
                        IconButton(
                            onClick = {
                                val copy = item.copy(
                                    id = "item_${System.currentTimeMillis() % 10000}",
                                    xPercent = (item.xPercent + 0.05f).coerceIn(0.05f, 0.95f),
                                    yPercent = (item.yPercent + 0.05f).coerceIn(0.05f, 0.95f)
                                )
                                onAddControlItem(copy)
                                selectedItemForEdit = copy
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = ScopeCyan, modifier = Modifier.size(20.dp))
                        }

                        // Size Decrease
                        IconButton(
                            onClick = {
                                val isJoy = item.type == GamepadButtonType.JOYSTICK_LEFT || item.type == GamepadButtonType.JOYSTICK_RIGHT
                                val step = if (isJoy) 10f else 6f
                                val minLimit = if (isJoy) 70f else 35f
                                val updated = item.copy(sizeDp = (item.sizeDp - step).coerceAtLeast(minLimit))
                                selectedItemForEdit = updated
                                onUpdateControlItem(updated)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Küçült", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Size Increase
                        IconButton(
                            onClick = {
                                val isJoy = item.type == GamepadButtonType.JOYSTICK_LEFT || item.type == GamepadButtonType.JOYSTICK_RIGHT
                                val step = if (isJoy) 10f else 6f
                                val maxLimit = if (isJoy) 240f else 160f
                                val updated = item.copy(sizeDp = (item.sizeDp + step).coerceAtMost(maxLimit))
                                selectedItemForEdit = updated
                                onUpdateControlItem(updated)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Büyüt", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Visibility Toggle
                        IconButton(
                            onClick = {
                                val updated = item.copy(isVisible = !item.isVisible)
                                selectedItemForEdit = updated
                                onUpdateControlItem(updated)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Gizle/Göster",
                                tint = if (item.isVisible) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete Button
                        IconButton(
                            onClick = {
                                onDeleteControlItem(item.id)
                                selectedItemForEdit = null
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = CombatRed, modifier = Modifier.size(20.dp))
                        }

                        // Save Item Button
                        Button(
                            onClick = {
                                onUpdateControlItem(item)
                                onUpdateProfile(profile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("save_selected_item_btn")
                        ) {
                            Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddKeyDialog) {
        AddKeyDialog(
            onDismiss = { showAddKeyDialog = false },
            onAddControlItem = { newItem ->
                onAddControlItem(newItem)
                selectedItemForEdit = newItem
            }
        )
    }

    if (showKeyBindDialog && selectedItemForEdit != null) {
        KeyBindDialog(
            item = selectedItemForEdit!!,
            onDismiss = { showKeyBindDialog = false },
            onBindKey = { keyDef ->
                val cur = selectedItemForEdit!!
                val updated = cur.copy(
                    label = keyDef.label,
                    subLabel = "",
                    boundKeyId = keyDef.id
                )
                selectedItemForEdit = updated
                onUpdateControlItem(updated)
            }
        )
    }

    if (showDesignDialog && selectedItemForEdit != null) {
        ButtonDesignDialog(
            item = selectedItemForEdit!!,
            onDismiss = { showDesignDialog = false },
            onSaveDesign = { updated ->
                selectedItemForEdit = updated
                onUpdateControlItem(updated)
            }
        )
    }

    if (showJoystickDialog) {
        JoystickSettingsDialog(
            profile = profile,
            selectedJoystick = selectedItemForEdit,
            onDismiss = { showJoystickDialog = false },
            onSaveSettings = { updatedProf, updatedJoy ->
                if (updatedJoy != null) {
                    selectedItemForEdit = updatedJoy
                    onUpdateControlItem(updatedJoy)
                }
                onUpdateProfile(updatedProf)
            }
        )
    }
}
