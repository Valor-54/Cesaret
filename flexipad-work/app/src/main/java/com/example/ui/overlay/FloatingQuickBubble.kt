package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CombatRed
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.TacticalAmber

/**
 * Draggable Floating Control Button & Multi-Profile Control Window:
 * - Small, sleek floating button draggable across the entire screen.
 * - Single tap: Opens the floating control window ("Yüzen Kontrol Penceresi").
 * - Control window provides instant switching between 1. Düzen, 2. Düzen, and 3. Düzen!
 * - Controls show/hide toggle leaving zero touch obstruction when hidden.
 */
@Composable
fun FloatingQuickBubble(
    modifier: Modifier = Modifier,
    isEditMode: Boolean,
    isHudVisible: Boolean,
    activeProfileId: Long = 1L,
    onToggleEditMode: () -> Unit,
    onToggleHudVisibility: () -> Unit,
    onSelectProfile: (Long) -> Unit = {},
    onAddNewKey: () -> Unit = {},
    onOpenApp: () -> Unit,
    onStopService: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit = { _, _ -> }
) {
    var isWindowOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // Draggable Floating Bubble
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(2.dp, if (isEditMode) TacticalAmber else if (isHudVisible) ScopeCyan else Color.Gray, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    )
                }
                .clickable {
                    // Tap opens / closes the floating control window
                    isWindowOpen = !isWindowOpen
                }
                .testTag("floating_quick_bubble"),
            color = if (isEditMode) TacticalAmber else Color(0xEE161B22),
            shadowElevation = 8.dp,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.SportsEsports,
                    contentDescription = "FlexiPad Floating Button",
                    tint = if (isEditMode) Color.Black else if (isHudVisible) Color.White else Color(0xFF888888),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Floating Control Window (Yüzen Kontrol Penceresi)
        AnimatedVisibility(
            visible = isWindowOpen,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(18.dp))
                    .testTag("floating_control_window"),
                color = Color(0xF50D1117),
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isHudVisible) ElectricGreen else CombatRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "FlexiPad Kontroller",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = { isWindowOpen = false },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Layout Profile Selector Row: 1. Düzen, 2. Düzen, 3. Düzen
                    Text(
                        "KONTROL DÜZENİ",
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val profiles = listOf(
                            Triple(1L, "1. Düzen", "Varsayılan"),
                            Triple(2L, "2. Düzen", "Xbox Pad"),
                            Triple(3L, "3. Düzen", "FPS Nişan")
                        )
                        for ((id, title, _) in profiles) {
                            val isSelected = activeProfileId == id
                            Button(
                                onClick = {
                                    onSelectProfile(id)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("layout_btn_$id"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) TacticalAmber else Color(0xFF21262D)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    title,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Controls Visibility Toggle (GÖSTER / GİZLE)
                    Button(
                        onClick = {
                            onToggleHudVisibility()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("toggle_hud_visibility_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHudVisible) Color(0xFF1F242C) else ElectricGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHudVisible) ScopeCyan else ElectricGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isHudVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isHudVisible) ScopeCyan else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (isHudVisible) "Tuş ve Joystickleri Gizle" else "Tuşları Ekranda Göster",
                                color = if (isHudVisible) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // Action Grid (Edit / Add / App / Stop)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Edit Mode Toggle
                        Button(
                            onClick = {
                                onToggleEditMode()
                                isWindowOpen = false
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditMode) TacticalAmber else Color(0xFF21262D)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Lock else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isEditMode) Color.Black else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isEditMode) "Kilitle" else "Özelleştir",
                                color = if (isEditMode) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Add Key
                        Button(
                            onClick = {
                                onAddNewKey()
                                isWindowOpen = false
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TacticalAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tuş Ekle", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Open App
                        Button(
                            onClick = {
                                onOpenApp()
                                isWindowOpen = false
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ScopeCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uygulama", color = Color.White, fontSize = 10.sp)
                        }

                        // Stop Service
                        Button(
                            onClick = {
                                onStopService()
                                isWindowOpen = false
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F44336)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = CombatRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Durdur", color = CombatRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
