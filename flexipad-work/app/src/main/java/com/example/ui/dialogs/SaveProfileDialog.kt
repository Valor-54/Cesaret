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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.GamepadProfile
import com.example.ui.theme.StealthBorder
import com.example.ui.theme.StealthCard
import com.example.ui.theme.TacticalAmber

@Composable
fun SaveProfileDialog(
    currentProfile: GamepadProfile,
    onDismiss: () -> Unit,
    onSaveAsNew: (name: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("${currentProfile.name} (Özel)") }
    var desc by remember { mutableStateOf(currentProfile.description) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .testTag("save_profile_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StealthCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StealthBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
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
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Profili Kaydet",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_save_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profil Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalAmber,
                        focusedLabelColor = TacticalAmber,
                        unfocusedBorderColor = StealthBorder
                    )
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Açıklama") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalAmber,
                        focusedLabelColor = TacticalAmber,
                        unfocusedBorderColor = StealthBorder
                    )
                )

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveAsNew(name.trim(), desc.trim())
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_save_profile_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
