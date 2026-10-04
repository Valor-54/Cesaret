package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.model.GamepadControlItem
import com.example.model.PresetColorThemes
import com.example.ui.overlay.VirtualButton
import com.example.ui.theme.CombatRed
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.StealthBorder
import com.example.ui.theme.StealthCard
import com.example.ui.theme.TacticalAmber

@Composable
fun ButtonDesignDialog(
    item: GamepadControlItem,
    onDismiss: () -> Unit,
    onSaveDesign: (GamepadControlItem) -> Unit
) {
    var label by remember { mutableStateOf(item.label) }
    var sizeDp by remember { mutableFloatStateOf(item.sizeDp) }
    var opacity by remember { mutableFloatStateOf(item.opacity) }
    var colorHex by remember { mutableLongStateOf(item.colorHex) }
    var textColorHex by remember { mutableLongStateOf(item.textColorHex) }
    var borderColorHex by remember { mutableLongStateOf(item.borderColorHex) }
    var backgroundColorHex by remember { mutableLongStateOf(item.backgroundColorHex) }
    var shape by remember { mutableStateOf(item.shape) }
    var fontSizeSp by remember { mutableFloatStateOf(item.fontSizeSp) }
    var cornerRadiusDp by remember { mutableFloatStateOf(item.cornerRadiusDp) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("button_design_dialog"),
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
                                Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Tuş Tasarımı: $label",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                "Renk, şekil, şeffaflık ve boyut ayarları",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_design_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF0F1318), RoundedCornerShape(14.dp))
                            .border(1.dp, StealthBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        VirtualButton(
                            sizeDp = sizeDp.coerceAtMost(85f),
                            label = label,
                            subLabel = "",
                            color = Color(colorHex),
                            textColor = Color(textColorHex),
                            borderColor = Color(borderColorHex),
                            backgroundColor = Color(backgroundColorHex),
                            shape = shape,
                            fontSizeSp = fontSizeSp,
                            cornerRadiusDp = cornerRadiusDp,
                            opacity = opacity,
                            isEditMode = true
                        )
                    }

                    // Label Editing (Strictly input code)
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it.trim().uppercase() },
                        label = { Text("Tuş Giriş İsmi (örn: A, B, RT, LT, SPACE)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("design_label_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalAmber,
                            focusedLabelColor = TacticalAmber,
                            unfocusedBorderColor = StealthBorder
                        )
                    )

                    // Ready-Made Color Themes
                    Column {
                        Text(
                            "Hazır Renk Temaları",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(PresetColorThemes.THEMES) { theme ->
                                Surface(
                                    modifier = Modifier
                                        .clickable {
                                            colorHex = theme.primaryColorHex
                                            borderColorHex = theme.borderColorHex
                                            backgroundColorHex = theme.backgroundColorHex
                                            textColorHex = theme.textColorHex
                                            opacity = theme.defaultOpacity
                                        }
                                        .testTag("theme_chip_${theme.name}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(theme.backgroundColorHex),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(theme.primaryColorHex))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color(theme.primaryColorHex), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            theme.name,
                                            color = Color(theme.textColorHex),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Color Picker Palette (Main Color)
                    Column {
                        Text(
                            "Renk Seçici (Ana / Vurgu Rengi)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(PresetColorThemes.COLOR_PALETTE) { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(hex), CircleShape)
                                        .border(
                                            width = if (colorHex == hex) 2.5.dp else 1.dp,
                                            color = if (colorHex == hex) Color.White else Color(0xFF38404A),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            colorHex = hex
                                            borderColorHex = hex
                                        }
                                        .testTag("color_picker_$hex"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorHex == hex) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Button Shapes (Yuvarlak, Köşeli Yuvarlak, Kare, Kapsül)
                    Column {
                        Text(
                            "Tuş Şekli",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val shapes = listOf(
                                ButtonShape.ROUNDED to "Köşeli",
                                ButtonShape.CIRCLE to "Yuvarlak",
                                ButtonShape.SQUARE to "Kare",
                                ButtonShape.PILL to "Kapsül"
                            )
                            for ((s, name) in shapes) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { shape = s }
                                        .testTag("shape_btn_${s.name}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (shape == s) TacticalAmber.copy(alpha = 0.25f) else Color(0xFF1E242C),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (shape == s) TacticalAmber else StealthBorder
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name,
                                            color = if (shape == s) TacticalAmber else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sliders: Size, Opacity, Font Size, Corner Radius
                    // Size
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Boyut", color = Color.White, fontSize = 12.5.sp)
                            Text("${sizeDp.toInt()}dp", color = TacticalAmber, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = sizeDp,
                            onValueChange = { sizeDp = it },
                            valueRange = 40f..180f,
                            colors = SliderDefaults.colors(thumbColor = TacticalAmber, activeTrackColor = TacticalAmber)
                        )
                    }

                    // Opacity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Şeffaflık", color = Color.White, fontSize = 12.5.sp)
                            Text("${(opacity * 100).toInt()}%", color = ScopeCyan, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.15f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = ScopeCyan, activeTrackColor = ScopeCyan)
                        )
                    }

                    // Font Size
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Yazı Boyutu", color = Color.White, fontSize = 12.5.sp)
                            Text("${fontSizeSp.toInt()}sp", color = ElectricGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fontSizeSp,
                            onValueChange = { fontSizeSp = it },
                            valueRange = 9f..28f,
                            colors = SliderDefaults.colors(thumbColor = ElectricGreen, activeTrackColor = ElectricGreen)
                        )
                    }

                    // Corner Radius (if rounded)
                    if (shape == ButtonShape.ROUNDED) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Köşe Yuvarlaklığı", color = Color.White, fontSize = 12.5.sp)
                                Text("${cornerRadiusDp.toInt()}dp", color = Color.Yellow, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cornerRadiusDp,
                                onValueChange = { cornerRadiusDp = it },
                                valueRange = 2f..36f,
                                colors = SliderDefaults.colors(thumbColor = Color.Yellow, activeTrackColor = Color.Yellow)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Save Button
                Button(
                    onClick = {
                        val updated = item.copy(
                            label = label.trim().uppercase(),
                            subLabel = "",
                            sizeDp = sizeDp,
                            opacity = opacity,
                            colorHex = colorHex,
                            textColorHex = textColorHex,
                            borderColorHex = borderColorHex,
                            backgroundColorHex = backgroundColorHex,
                            shape = shape,
                            fontSizeSp = fontSizeSp,
                            cornerRadiusDp = cornerRadiusDp
                        )
                        onSaveDesign(updated)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_button_design_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tasarımı Kaydet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
