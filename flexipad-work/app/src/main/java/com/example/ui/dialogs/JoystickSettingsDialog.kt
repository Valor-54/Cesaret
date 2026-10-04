package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.GamepadControlItem
import com.example.model.GamepadProfile
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.StealthBorder
import com.example.ui.theme.StealthCard
import com.example.ui.theme.TacticalAmber

@Composable
fun JoystickSettingsDialog(
    profile: GamepadProfile,
    selectedJoystick: GamepadControlItem? = null,
    onDismiss: () -> Unit,
    onSaveSettings: (GamepadProfile, GamepadControlItem?) -> Unit
) {
    var joystickSizeDp by remember { mutableFloatStateOf(selectedJoystick?.sizeDp ?: 140f) }
    var leftSensitivity by remember { mutableFloatStateOf(profile.leftStickSensitivity) }
    var leftDeadzone by remember { mutableFloatStateOf(profile.leftStickDeadzone) }
    var leftMaxTravel by remember { mutableFloatStateOf(profile.leftStickMaxTravel) }
    var rightSensitivity by remember { mutableFloatStateOf(profile.rightStickSensitivity) }
    var rightDeadzone by remember { mutableFloatStateOf(profile.rightStickDeadzone) }
    var rightMaxTravel by remember { mutableFloatStateOf(profile.rightStickMaxTravel) }
    var centerAimFreeZone by remember { mutableStateOf(profile.centerAimFreeZone) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .testTag("joystick_settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StealthCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StealthBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Joystick ve Analog Ayarları",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_joystick_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                // Joystick Size Slider (if joystick selected)
                if (selectedJoystick != null) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Joystick Boyutu (${selectedJoystick.label})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${joystickSizeDp.toInt()} dp", color = TacticalAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = joystickSizeDp,
                            onValueChange = { joystickSizeDp = it },
                            valueRange = 70f..220f,
                            colors = SliderDefaults.colors(thumbColor = TacticalAmber, activeTrackColor = TacticalAmber)
                        )
                    }
                }

                // Left Stick Sensitivity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sol Analog (LS) Hassasiyeti", color = Color.White, fontSize = 13.sp)
                        Text("${String.format("%.1f", leftSensitivity)}x", color = TacticalAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = leftSensitivity,
                        onValueChange = { leftSensitivity = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = TacticalAmber, activeTrackColor = TacticalAmber)
                    )
                }

                // Left Stick Deadzone
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sol Analog Ölü Bölge (Deadzone)", color = Color.White, fontSize = 13.sp)
                        Text("${(leftDeadzone * 100).toInt()}%", color = ScopeCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = leftDeadzone,
                        onValueChange = { leftDeadzone = it },
                        valueRange = 0.01f..0.25f,
                        colors = SliderDefaults.colors(thumbColor = ScopeCyan, activeTrackColor = ScopeCyan)
                    )
                }

                // Left Stick Max Travel
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sol Analog Maksimum Hareket Mesafesi", color = Color.White, fontSize = 13.sp)
                        Text("${(leftMaxTravel * 100).toInt()}%", color = TacticalAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = leftMaxTravel,
                        onValueChange = { leftMaxTravel = it },
                        valueRange = 0.40f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = TacticalAmber, activeTrackColor = TacticalAmber)
                    )
                }

                // Right Stick Sensitivity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sağ Analog (RS) Hassasiyeti", color = Color.White, fontSize = 13.sp)
                        Text("${String.format("%.1f", rightSensitivity)}x", color = TacticalAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = rightSensitivity,
                        onValueChange = { rightSensitivity = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = TacticalAmber, activeTrackColor = TacticalAmber)
                    )
                }

                // Right Stick Deadzone
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sağ Analog Ölü Bölge (Deadzone)", color = Color.White, fontSize = 13.sp)
                        Text("${(rightDeadzone * 100).toInt()}%", color = ScopeCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = rightDeadzone,
                        onValueChange = { rightDeadzone = it },
                        valueRange = 0.01f..0.25f,
                        colors = SliderDefaults.colors(thumbColor = ScopeCyan, activeTrackColor = ScopeCyan)
                    )
                }

                // Right Stick Max Travel
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sağ Analog Maksimum Hareket Mesafesi", color = Color.White, fontSize = 13.sp)
                        Text("${(rightMaxTravel * 100).toInt()}%", color = TacticalAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = rightMaxTravel,
                        onValueChange = { rightMaxTravel = it },
                        valueRange = 0.40f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = TacticalAmber, activeTrackColor = TacticalAmber)
                    )
                }

                // Center Aim Free Zone Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ekranın Ortasını Boş Bırak (Nişan / Kamera)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Nişan alma ve serbest kamera kaydırma için temiz alan", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = centerAimFreeZone,
                        onCheckedChange = { centerAimFreeZone = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TacticalAmber, checkedTrackColor = TacticalAmber.copy(alpha = 0.5f))
                    )
                }

                // Save button
                Button(
                    onClick = {
                        val updatedProf = profile.copy(
                            leftStickSensitivity = leftSensitivity,
                            leftStickDeadzone = leftDeadzone,
                            leftStickMaxTravel = leftMaxTravel,
                            rightStickSensitivity = rightSensitivity,
                            rightStickDeadzone = rightDeadzone,
                            rightStickMaxTravel = rightMaxTravel,
                            dynamicJoystickCenter = false,
                            centerAimFreeZone = centerAimFreeZone
                        )
                        val updatedJoy = selectedJoystick?.copy(sizeDp = joystickSizeDp)
                        onSaveSettings(updatedProf, updatedJoy)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_joystick_settings_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ayarları Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
