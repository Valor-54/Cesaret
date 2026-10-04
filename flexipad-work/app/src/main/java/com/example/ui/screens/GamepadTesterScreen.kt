package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.GamepadInputManager
import com.example.model.GamepadButtonType
import com.example.service.FlexiPadAccessibilityService
import com.example.ui.overlay.VirtualButton
import com.example.ui.overlay.VirtualDpad
import com.example.ui.overlay.VirtualJoystick
import com.example.ui.overlay.VirtualTrigger
import com.example.ui.theme.CombatRed
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.StealthBorder
import com.example.ui.theme.StealthCard
import com.example.ui.theme.TacticalAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadTesterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val inputManager = remember { GamepadInputManager(context) }
    val state by inputManager.currentState.collectAsState()
    val isA11yActive by FlexiPadAccessibilityService.isServiceActive.collectAsState()
    val isHidConnected by inputManager.bluetoothDriver.isConnected.collectAsState()
    val isRootActive by inputManager.adbDriver.isRootAvailable.collectAsState()
    val isShizukuActive by inputManager.adbDriver.isShizukuAvailable.collectAsState()
    val recentLogs by inputManager.recentLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Canlı Gamepad & Multi-Touch Test Cihazı", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("tester_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pipeline Diagnostic Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = StealthCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Giriş Dağıtım & İzin Zinciri (GForce Pipeline)",
                        fontWeight = FontWeight.Bold,
                        color = TacticalAmber,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Accessibility Service Badge
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isA11yActive) Color(0x3300E676) else Color(0x33EF5350),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isA11yActive) ElectricGreen else CombatRed)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isA11yActive) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isA11yActive) ElectricGreen else CombatRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Erişilebilirlik", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    if (isA11yActive) "AKTİF (Touch)" else "KAPALI",
                                    fontSize = 10.sp,
                                    color = if (isA11yActive) ElectricGreen else CombatRed
                                )
                            }
                        }

                        // Bluetooth HID Badge
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isHidConnected) Color(0x3300B0FF) else Color(0xFF1E242C),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isHidConnected) ScopeCyan else Color.Gray)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isHidConnected) ScopeCyan else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BT HID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    if (isHidConnected) "BAĞLI (Xbox)" else "STANDBY",
                                    fontSize = 10.sp,
                                    color = if (isHidConnected) ScopeCyan else Color.Gray
                                )
                            }
                        }

                        // Shizuku / Root Badge
                        val isPrivileged = isRootActive || isShizukuActive
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPrivileged) Color(0x3300E676) else Color(0xFF1E242C),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPrivileged) ElectricGreen else Color.Gray)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = if (isPrivileged) ElectricGreen else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Shizuku/Root", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    if (isPrivileged) "HAZIR (Kernel)" else "YOK (Sandbox)",
                                    fontSize = 10.sp,
                                    color = if (isPrivileged) ElectricGreen else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Live Telemetry Monitor Box
            Card(
                colors = CardDefaults.cardColors(containerColor = StealthCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Anlık Giriş Telemetrisi (XInput / HID / Multi-Touch)",
                        fontWeight = FontWeight.Bold,
                        color = TacticalAmber,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("1. Sol Çubuk (LS):", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "X: ${String.format("%.2f", state.leftStickX)} | Y: ${String.format("%.2f", state.leftStickY)}",
                                color = if (state.isLeftStickActive) ElectricGreen else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("2. Sağ Çubuk (RS):", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "X: ${String.format("%.2f", state.rightStickX)} | Y: ${String.format("%.2f", state.rightStickY)}",
                                color = if (state.isRightStickActive) ElectricGreen else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LT Sol Tetik:", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "${(state.l2Trigger * 100).toInt()}%",
                                color = if (state.l2Trigger > 0.05f) ScopeCyan else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("RT Sağ Tetik:", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "${(state.r2Trigger * 100).toInt()}%",
                                color = if (state.r2Trigger > 0.05f) CombatRed else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Aktif Tuş Sayısı:", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "${state.pressedButtons.size}",
                                color = if (state.pressedButtons.isNotEmpty()) TacticalAmber else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (state.pressedButtons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (btn in state.pressedButtons) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TacticalAmber.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = btn.defaultLabel,
                                        color = TacticalAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Event Logger (Terminal View)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090D12)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Son Giriş Olayları (DOWN -> HOLD -> UP)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${recentLogs.size} Olay", color = TacticalAmber, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (recentLogs.isEmpty()) {
                        Text(
                            "Henüz olay yok. Aşağıdaki tuşlara veya joysticklere aynı anda dokunun.",
                            color = Color(0xFF484F58),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    } else {
                        recentLogs.takeLast(6).forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("DOWN")) ElectricGreen else if (log.contains("UP")) ScopeCyan else Color(0xFFC9D1D9),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            // Interactive Multi-Touch Playground
            Text(
                "Canlı Çoklu Dokunma (Multi-Touch) Test Alanı:",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14181E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Bumpers & Triggers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VirtualTrigger(
                            sizeDp = 64f,
                            label = "LT",
                            subLabel = "",
                            color = ScopeCyan,
                            testTag = "test_trigger_lt",
                            onTriggerChanged = { p -> inputManager.updateTrigger(true, p) }
                        )

                        VirtualButton(
                            sizeDp = 54f,
                            label = "LB",
                            subLabel = "",
                            color = Color(0xFF78909C),
                            testTag = "test_btn_lb",
                            onPressChanged = { pr -> inputManager.setButtonPressed(GamepadButtonType.BUTTON_L1, pr) }
                        )

                        VirtualButton(
                            sizeDp = 54f,
                            label = "RB",
                            subLabel = "",
                            color = Color(0xFF78909C),
                            testTag = "test_btn_rb",
                            onPressChanged = { pr -> inputManager.setButtonPressed(GamepadButtonType.BUTTON_R1, pr) }
                        )

                        VirtualTrigger(
                            sizeDp = 64f,
                            label = "RT",
                            subLabel = "",
                            color = CombatRed,
                            testTag = "test_trigger_rt",
                            onTriggerChanged = { p -> inputManager.updateTrigger(false, p) }
                        )
                    }

                    // Main Controllers Row (Both Joysticks + D-Pad + Buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Stick & D-Pad
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VirtualJoystick(
                                sizeDp = 120f,
                                label = "LS",
                                color = TacticalAmber,
                                onMove = { x, y -> inputManager.updateJoystick(true, x, y) },
                                onClickThumb = {
                                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_L3, true)
                                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_L3, false)
                                }
                            )

                            VirtualDpad(
                                sizeDp = 100f,
                                onDirectionChanged = { dir, pr -> inputManager.setButtonPressed(dir, pr) }
                            )
                        }

                        // Right Stick & Face Buttons (A, B, X, Y)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VirtualJoystick(
                                sizeDp = 120f,
                                label = "RS",
                                color = ElectricGreen,
                                onMove = { x, y -> inputManager.updateJoystick(false, x, y) },
                                onClickThumb = {
                                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_R3, true)
                                    inputManager.setButtonPressed(GamepadButtonType.BUTTON_R3, false)
                                }
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VirtualButton(
                                    sizeDp = 48f,
                                    label = "Y",
                                    subLabel = "",
                                    color = Color(0xFFFFCA28),
                                    onPressChanged = { pr -> inputManager.setButtonPressed(GamepadButtonType.BUTTON_Y, pr) }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VirtualButton(
                                        sizeDp = 48f,
                                        label = "X",
                                        subLabel = "",
                                        color = Color(0xFF29B6F6),
                                        onPressChanged = { pr -> inputManager.setButtonPressed(GamepadButtonType.BUTTON_X, pr) }
                                    )
                                    VirtualButton(
                                        sizeDp = 48f,
                                        label = "B",
                                        subLabel = "",
                                        color = CombatRed,
                                        onPressChanged = { pr -> inputManager.setButtonPressed(GamepadButtonType.BUTTON_B, pr) }
                                    )
                                }
                                VirtualButton(
                                    sizeDp = 48f,
                                    label = "A",
                                    subLabel = "",
                                    color = ElectricGreen,
                                    onPressChanged = { pr -> inputManager.setButtonPressed(GamepadButtonType.BUTTON_A, pr) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
