package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ButtonShape
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import com.example.model.KeyCategory
import com.example.model.KeyRegistry
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.StealthBorder
import com.example.ui.theme.StealthCard
import com.example.ui.theme.TacticalAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddKeyDialog(
    onDismiss: () -> Unit,
    onAddControlItem: (GamepadControlItem) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(
        KeyCategory.GAMEPAD,
        KeyCategory.KEYBOARD_BASIC,
        KeyCategory.KEYBOARD_LETTERS,
        KeyCategory.KEYBOARD_NUMBERS,
        KeyCategory.KEYBOARD_ARROWS,
        KeyCategory.GAME_ACTIONS,
        KeyCategory.CUSTOM
    )

    var customLabel by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("add_key_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StealthCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StealthBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxHeight()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TacticalAmber,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Yeni Tuş veya Joystick Ekle",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                "Ekrana bağımsız kontrol yerleştirin",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_key_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = StealthBackground,
                    contentColor = TacticalAmber,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                            color = TacticalAmber
                        )
                    }
                ) {
                    categories.forEachIndexed { index, cat ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = cat.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategoryIndex == index) TacticalAmber else Color.Gray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val currentCategory = categories[selectedCategoryIndex]
                if (currentCategory == KeyCategory.CUSTOM) {
                    // Custom key creation panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Özel Tuş Oluştur",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "İstediğiniz tuş etiketini girerek ekrana ekleyin (örn: E, F, TAB).",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = customLabel,
                            onValueChange = { customLabel = it },
                            label = { Text("Tuş Üzerindeki Giriş (örn: E, F, C, V)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("custom_key_label_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TacticalAmber,
                                focusedLabelColor = TacticalAmber,
                                unfocusedBorderColor = StealthBorder
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (customLabel.isNotBlank()) {
                                    val id = "custom_${System.currentTimeMillis() % 10000}"
                                    val newItem = GamepadControlItem(
                                        id = id,
                                        type = GamepadButtonType.BUTTON_A,
                                        label = customLabel.trim().uppercase(),
                                        subLabel = "",
                                        xPercent = 0.5f,
                                        yPercent = 0.5f,
                                        sizeDp = 60f,
                                        opacity = 0.75f,
                                        colorHex = 0xFFFF9800,
                                        textColorHex = 0xFFFFFFFF,
                                        borderColorHex = 0xFFFF9800,
                                        shape = ButtonShape.ROUNDED,
                                        boundKeyId = ""
                                    )
                                    onAddControlItem(newItem)
                                    onDismiss()
                                }
                            },
                            enabled = customLabel.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("create_custom_key_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Ekrana Ekle", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Filter keys for this category
                    val keys = KeyRegistry.ALL_KEYS.filter { it.category == currentCategory }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Joystick Add Banner when in Gamepad category
                        if (currentCategory == KeyCategory.GAMEPAD) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "JOYSTICKLER (1. VE 2. JOYSTICK)",
                                        color = TacticalAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val id = "joy_left_${System.currentTimeMillis() % 10000}"
                                                val newItem = GamepadControlItem(
                                                    id = id,
                                                    type = GamepadButtonType.JOYSTICK_LEFT,
                                                    label = "LS",
                                                    subLabel = "",
                                                    xPercent = 0.16f,
                                                    yPercent = 0.70f,
                                                    sizeDp = 140f,
                                                    opacity = 0.70f,
                                                    colorHex = 0xFFFF9800L,
                                                    shape = ButtonShape.CIRCLE,
                                                    boundKeyId = "GP_JOYSTICK_LEFT"
                                                )
                                                onAddControlItem(newItem)
                                                onDismiss()
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp).testTag("add_left_joystick_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, TacticalAmber),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TacticalAmber, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("+ 1. Sol Joystick (LS)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val id = "joy_right_${System.currentTimeMillis() % 10000}"
                                                val newItem = GamepadControlItem(
                                                    id = id,
                                                    type = GamepadButtonType.JOYSTICK_RIGHT,
                                                    label = "RS",
                                                    subLabel = "",
                                                    xPercent = 0.74f,
                                                    yPercent = 0.70f,
                                                    sizeDp = 140f,
                                                    opacity = 0.70f,
                                                    colorHex = 0xFF00E676L,
                                                    shape = ButtonShape.CIRCLE,
                                                    boundKeyId = "GP_JOYSTICK_RIGHT"
                                                )
                                                onAddControlItem(newItem)
                                                onDismiss()
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp).testTag("add_right_joystick_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2E24)),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricGreen),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("+ 2. Sağ Joystick (RS)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (keyDef in keys) {
                                    Surface(
                                        modifier = Modifier
                                            .clickable {
                                                val id = "item_${keyDef.id.lowercase()}_${System.currentTimeMillis() % 10000}"
                                                val isJoystick = keyDef.id.contains("JOYSTICK")
                                                val isTrigger = keyDef.id == "GP_LT" || keyDef.id == "GP_RT"
                                                val isDpad = keyDef.id == "GP_DPAD"

                                                val type = when {
                                                    keyDef.id == "GP_JOYSTICK_LEFT" -> GamepadButtonType.JOYSTICK_LEFT
                                                    keyDef.id == "GP_JOYSTICK_RIGHT" -> GamepadButtonType.JOYSTICK_RIGHT
                                                    keyDef.id == "GP_LT" -> GamepadButtonType.BUTTON_L2
                                                    keyDef.id == "GP_RT" -> GamepadButtonType.BUTTON_R2
                                                    keyDef.id.startsWith("GP_DPAD_") -> when (keyDef.id) {
                                                        "GP_DPAD_UP" -> GamepadButtonType.DPAD_UP
                                                        "GP_DPAD_DOWN" -> GamepadButtonType.DPAD_DOWN
                                                        "GP_DPAD_LEFT" -> GamepadButtonType.DPAD_LEFT
                                                        else -> GamepadButtonType.DPAD_RIGHT
                                                    }
                                                    keyDef.gamepadButtonType != null -> keyDef.gamepadButtonType
                                                    else -> GamepadButtonType.BUTTON_A
                                                }

                                                val size = when {
                                                    isJoystick -> 140f
                                                    isTrigger -> 68f
                                                    isDpad -> 115f
                                                    else -> 58f
                                                }

                                                val colorHex = when {
                                                    isJoystick -> 0xFFFF9800L
                                                    keyDef.id.contains("FIRE") || keyDef.id == "GP_RT" -> 0xFFF44336L
                                                    keyDef.id.contains("ZOOM") || keyDef.id == "GP_LT" -> 0xFF2196F3L
                                                    keyDef.id == "GP_A" -> 0xFF4CAF50L
                                                    keyDef.id == "GP_B" -> 0xFFEF5350L
                                                    keyDef.id == "GP_X" -> 0xFF00B0FFL
                                                    keyDef.id == "GP_Y" -> 0xFFFFCA28L
                                                    else -> 0xFF3DDC84L
                                                }

                                                val newItem = GamepadControlItem(
                                                    id = id,
                                                    type = type,
                                                    label = keyDef.label,
                                                    subLabel = "",
                                                    xPercent = if (keyDef.id.contains("LEFT") || keyDef.id == "GP_LB" || keyDef.id == "GP_LT") 0.18f else 0.82f,
                                                    yPercent = 0.5f,
                                                    sizeDp = size,
                                                    opacity = 0.75f,
                                                    colorHex = colorHex,
                                                    textColorHex = 0xFFFFFFFF,
                                                    borderColorHex = colorHex,
                                                    backgroundColorHex = 0xFF16191D,
                                                    shape = if (isJoystick) ButtonShape.CIRCLE else ButtonShape.ROUNDED,
                                                    boundKeyId = keyDef.id
                                                )
                                                onAddControlItem(newItem)
                                                onDismiss()
                                            }
                                            .testTag("key_chip_${keyDef.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E242C),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38404A))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = keyDef.label,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            if (keyDef.subLabel.isNotBlank()) {
                                                Text(
                                                    text = keyDef.subLabel,
                                                    color = Color(0xFF8B949E),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
