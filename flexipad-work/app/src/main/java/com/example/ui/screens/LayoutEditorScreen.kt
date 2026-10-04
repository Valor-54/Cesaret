package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.overlay.VirtualButton
import com.example.ui.overlay.VirtualDpad
import com.example.ui.overlay.VirtualJoystick
import com.example.ui.overlay.VirtualTrigger
import com.example.ui.theme.CombatRed
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.TacticalAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    initialProfile: GamepadProfile,
    onSaveProfile: (GamepadProfile) -> Unit,
    onResetProfile: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val inputManager = remember { GamepadInputManager(context) }
    var currentProfile by remember { mutableStateOf(initialProfile) }
    var selectedItem by remember { mutableStateOf<GamepadControlItem?>(null) }
    var isTestInteraction by remember { mutableStateOf(false) }

    var liveDragOffsets by remember { mutableStateOf<Map<String, Pair<Float, Float>>>(emptyMap()) }
    var debugDragStatus by remember { mutableStateOf("DÜZENLEME: Taşımak için herhangi bir tuş veya joystick'e dokunup sürükleyin") }

    var showAddKeyDialog by remember { mutableStateOf(false) }
    var showDesignDialog by remember { mutableStateOf(false) }
    var showKeyBindDialog by remember { mutableStateOf(false) }
    var showJoystickDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Düzen: ${currentProfile.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTestInteraction) "Test Modu (Tuşları Dene)" else "Düzenleme Modu (Tuşları Sürükle)",
                            fontSize = 11.sp,
                            color = if (isTestInteraction) ElectricGreen else TacticalAmber
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Add new key
                    IconButton(
                        onClick = { showAddKeyDialog = true },
                        modifier = Modifier.testTag("editor_add_key_btn")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Yeni Tuş Ekle",
                            tint = TacticalAmber
                        )
                    }
                    // Toggle Test / Drag mode
                    IconButton(
                        onClick = { isTestInteraction = !isTestInteraction },
                        modifier = Modifier.testTag("editor_toggle_test_btn")
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = "Test Modu",
                            tint = if (isTestInteraction) ElectricGreen else Color.Gray
                        )
                    }
                    // Reset button
                    IconButton(
                        onClick = {
                            onResetProfile()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_reset_btn")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sıfırla",
                            tint = CombatRed
                        )
                    }
                    // Save button
                    IconButton(
                        onClick = {
                            onSaveProfile(currentProfile)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_save_btn")
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Kaydet",
                            tint = TacticalAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StealthBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = StealthBackground
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F1318))
        ) {
            val screenW = maxWidth
            val screenH = maxHeight
            val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            // Center aim zone visual boundary guide
            if (currentProfile.centerAimFreeZone) {
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

            // Touch Drag Live Debug Status (User test indicator)
            if (!isTestInteraction) {
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

            // Controls on canvas
            for (item in currentProfile.controls) {
                if (item.isVisible || selectedItem?.id == item.id) {
                    key(item.id) {
                        val effectiveXPercent = liveDragOffsets[item.id]?.first ?: item.xPercent
                        val effectiveYPercent = liveDragOffsets[item.id]?.second ?: item.yPercent
                        val isPill = item.shape == ButtonShape.PILL
                        val widthDp = if (isPill) item.sizeDp * 1.35f else item.sizeDp
                        val heightDp = item.sizeDp
                        val xPos = (screenW * effectiveXPercent) - (widthDp.dp / 2f)
                        val yPos = (screenH * effectiveYPercent) - (heightDp.dp / 2f)

                        val isSelected = selectedItem?.id == item.id

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(xPos.toPx().roundToInt(), yPos.toPx().roundToInt())
                                }
                                .size(widthDp.dp, heightDp.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, TacticalAmber, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (!isTestInteraction) {
                                        // SÜRÜKLEME: Hem butonlar hem 1. ve 2. Joystick düzenleme modunda sürüklenebilir!
                                        Modifier.pointerInput(item.id, screenWidthPx, screenHeightPx) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                down.consume()
                                                val pointerId = down.id
                                                var currentX = effectiveXPercent
                                                var currentY = effectiveYPercent
                                                var hasMoved = false
                                                selectedItem = item
                                                debugDragStatus = "SÜRÜKLEME BAŞLADI: [${item.label}] X: ${(currentX * 100).roundToInt()}% Y: ${(currentY * 100).roundToInt()}%"

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                                    if (!change.pressed) {
                                                        change.consume()
                                                        debugDragStatus = "SÜRÜKLEME BİTTİ: [${item.label}] (X: ${(currentX * 100).roundToInt()}%, Y: ${(currentY * 100).roundToInt()}%)"
                                                        if (hasMoved) {
                                                            liveDragOffsets = liveDragOffsets - item.id
                                                            val finalItem = item.copy(xPercent = currentX, yPercent = currentY)
                                                            selectedItem = finalItem
                                                            val list = currentProfile.controls.map {
                                                                if (it.id == finalItem.id) finalItem else it
                                                            }
                                                            currentProfile = currentProfile.copy(controls = list)
                                                        }
                                                        break
                                                    }
                                                    val delta = change.positionChange()
                                                    if (delta != Offset.Zero) {
                                                        change.consume()
                                                        hasMoved = true
                                                        currentX = (currentX + (delta.x / screenWidthPx)).coerceIn(0.02f, 0.98f)
                                                        currentY = (currentY + (delta.y / screenHeightPx)).coerceIn(0.02f, 0.98f)
                                                        debugDragStatus = "HAREKET ALGILANDI: [${item.label}] X: ${(currentX * 100).roundToInt()}% Y: ${(currentY * 100).roundToInt()}%"
                                                        liveDragOffsets = liveDragOffsets + (item.id to Pair(currentX, currentY))
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
                                        sensitivity = currentProfile.leftStickSensitivity,
                                        deadzone = currentProfile.leftStickDeadzone,
                                        maxTravelFactor = currentProfile.leftStickMaxTravel,
                                        dynamicCenter = currentProfile.dynamicJoystickCenter,
                                        isEditMode = !isTestInteraction,
                                        onMove = { x, y ->
                                            inputManager.updateJoystick(true, x, y)
                                        }
                                    )
                                }
                                GamepadButtonType.JOYSTICK_RIGHT -> {
                                    VirtualJoystick(
                                        sizeDp = item.sizeDp,
                                        label = item.label,
                                        color = Color(item.colorHex),
                                        opacity = item.opacity,
                                        sensitivity = currentProfile.rightStickSensitivity,
                                        deadzone = currentProfile.rightStickDeadzone,
                                        maxTravelFactor = currentProfile.rightStickMaxTravel,
                                        dynamicCenter = currentProfile.dynamicJoystickCenter,
                                        isEditMode = !isTestInteraction,
                                        onMove = { x, y ->
                                            inputManager.updateJoystick(false, x, y)
                                        }
                                    )
                                }
                                GamepadButtonType.DPAD -> {
                                    VirtualDpad(
                                        sizeDp = item.sizeDp,
                                        color = Color(item.colorHex),
                                        opacity = item.opacity,
                                        isEditMode = !isTestInteraction,
                                        onDirectionChanged = { dir, pr -> inputManager.setButtonPressed(dir, pr) }
                                    )
                                }
                                GamepadButtonType.BUTTON_L2, GamepadButtonType.BUTTON_R2 -> {
                                    VirtualTrigger(
                                        sizeDp = item.sizeDp,
                                        label = item.label,
                                        subLabel = "",
                                        color = Color(item.colorHex),
                                        opacity = item.opacity,
                                        isEditMode = !isTestInteraction,
                                        onTriggerChanged = { p ->
                                            val isLeft = item.type == GamepadButtonType.BUTTON_L2
                                            inputManager.updateTrigger(isLeft, p)
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
                                        isEditMode = !isTestInteraction,
                                        onPressChanged = { pr -> inputManager.setCustomControlPressed(item, pr) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar for selected item
            selectedItem?.let { sel ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .testTag("editor_selected_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFA161B22)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seçilen: ${sel.label}",
                                fontWeight = FontWeight.Bold,
                                color = TacticalAmber,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "${sel.sizeDp.toInt()}dp | ${(sel.opacity * 100).toInt()}%",
                                color = Color.Gray,
                                fontSize = 11.5.sp
                            )
                        }

                        // Action Buttons Row (Tuş Ata, Tasarım, Joystick, Kopyala, Sil, Gizle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tuş Ata Button
                            OutlinedButton(
                                onClick = { showKeyBindDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tuş Ata", fontSize = 11.5.sp)
                            }

                            // Tasarım Button
                            OutlinedButton(
                                onClick = { showDesignDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalAmber),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalAmber.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tasarım", fontSize = 11.5.sp)
                            }

                            // Joystick Button
                            if (sel.type == GamepadButtonType.JOYSTICK_LEFT || sel.type == GamepadButtonType.JOYSTICK_RIGHT) {
                                OutlinedButton(
                                    onClick = { showJoystickDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalAmber),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalAmber.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Joystick", fontSize = 11.5.sp)
                                }
                            }

                            // Boyut Artır / Azalt
                            IconButton(
                                onClick = {
                                    val isJoy = sel.type == GamepadButtonType.JOYSTICK_LEFT || sel.type == GamepadButtonType.JOYSTICK_RIGHT
                                    val step = if (isJoy) 10f else 6f
                                    val minLimit = if (isJoy) 70f else 35f
                                    val updated = sel.copy(sizeDp = (sel.sizeDp - step).coerceAtLeast(minLimit))
                                    selectedItem = updated
                                    val list = currentProfile.controls.map { if (it.id == updated.id) updated else it }
                                    currentProfile = currentProfile.copy(controls = list)
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Küçült", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    val isJoy = sel.type == GamepadButtonType.JOYSTICK_LEFT || sel.type == GamepadButtonType.JOYSTICK_RIGHT
                                    val step = if (isJoy) 10f else 6f
                                    val maxLimit = if (isJoy) 240f else 160f
                                    val updated = sel.copy(sizeDp = (sel.sizeDp + step).coerceAtMost(maxLimit))
                                    selectedItem = updated
                                    val list = currentProfile.controls.map { if (it.id == updated.id) updated else it }
                                    currentProfile = currentProfile.copy(controls = list)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Büyüt", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Kopyala Button
                            IconButton(
                                onClick = {
                                    val copy = sel.copy(
                                        id = "item_${System.currentTimeMillis() % 10000}",
                                        xPercent = (sel.xPercent + 0.05f).coerceIn(0.05f, 0.95f),
                                        yPercent = (sel.yPercent + 0.05f).coerceIn(0.05f, 0.95f)
                                    )
                                    val list = currentProfile.controls + copy
                                    currentProfile = currentProfile.copy(controls = list)
                                    selectedItem = copy
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = ScopeCyan, modifier = Modifier.size(20.dp))
                            }

                            // Visibility Toggle
                            IconButton(
                                onClick = {
                                    val updated = sel.copy(isVisible = !sel.isVisible)
                                    selectedItem = updated
                                    val list = currentProfile.controls.map { if (it.id == updated.id) updated else it }
                                    currentProfile = currentProfile.copy(controls = list)
                                }
                            ) {
                                Icon(
                                    imageVector = if (sel.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Gizle/Göster",
                                    tint = if (sel.isVisible) Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Sil Button
                            IconButton(
                                onClick = {
                                    val list = currentProfile.controls.filterNot { it.id == sel.id }
                                    currentProfile = currentProfile.copy(controls = list)
                                    selectedItem = null
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = CombatRed, modifier = Modifier.size(20.dp))
                            }
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
                currentProfile = currentProfile.copy(controls = currentProfile.controls + newItem)
                selectedItem = newItem
            }
        )
    }

    if (showKeyBindDialog && selectedItem != null) {
        KeyBindDialog(
            item = selectedItem!!,
            onDismiss = { showKeyBindDialog = false },
            onBindKey = { keyDef ->
                val cur = selectedItem!!
                val updated = cur.copy(
                    label = keyDef.label,
                    subLabel = "",
                    boundKeyId = keyDef.id
                )
                selectedItem = updated
                val list = currentProfile.controls.map { if (it.id == updated.id) updated else it }
                currentProfile = currentProfile.copy(controls = list)
            }
        )
    }

    if (showDesignDialog && selectedItem != null) {
        ButtonDesignDialog(
            item = selectedItem!!,
            onDismiss = { showDesignDialog = false },
            onSaveDesign = { updated ->
                selectedItem = updated
                val list = currentProfile.controls.map { if (it.id == updated.id) updated else it }
                currentProfile = currentProfile.copy(controls = list)
            }
        )
    }

    if (showJoystickDialog) {
        JoystickSettingsDialog(
            profile = currentProfile,
            selectedJoystick = selectedItem,
            onDismiss = { showJoystickDialog = false },
            onSaveSettings = { updatedProf, updatedJoy ->
                var prof = updatedProf
                if (updatedJoy != null) {
                    selectedItem = updatedJoy
                    val list = prof.controls.map { if (it.id == updatedJoy.id) updatedJoy else it }
                    prof = prof.copy(controls = list)
                }
                currentProfile = prof
            }
        )
    }
}
