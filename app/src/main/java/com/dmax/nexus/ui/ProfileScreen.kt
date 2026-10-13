package com.dmax.nexus.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmax.nexus.settings.AppSettings

private val themeOptions = listOf("Emerald", "Purple Cyber", "Crimson", "Monochrome")

@Composable
fun ProfileScreen(
    settings: AppSettings,
    firebaseConfigured: Boolean,
    chatCount: Int,
    onSaveSettings: (String, String, Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onClearChatHistory: () -> Unit
) {
    val p = LocalNexusPalette.current
    var endpoint by rememberSaveable(settings.aiEndpoint) { mutableStateOf(settings.aiEndpoint) }
    var accessToken by rememberSaveable(settings.accessToken) { mutableStateOf(settings.accessToken) }
    var lowDataMode by rememberSaveable(settings.lowDataMode) { mutableStateOf(settings.lowDataMode) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var savedNotice by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
        ScreenHeader("PERSONAL SPACE", "Your profile", "Customize how NEXUS looks, connects and stores data.")
        NexusPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("NEXUS Explorer", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        MutedText("Local device profile")
                    }
                    StatusPill("ACTIVE")
                }
                MutedText("Local records remain on this device. Cloud community content is stored in the Firebase project you configure.")
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("APPEARANCE")
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            themeOptions.forEach { theme ->
                if (settings.theme == theme) AccentButton(theme, onClick = { onThemeChange(theme) })
                else QuietButton(theme, onClick = { onThemeChange(theme) })
            }
        }
        Spacer(Modifier.height(7.dp))
        MutedText("Theme choice is saved and should apply immediately.")

        Spacer(Modifier.height(20.dp))
        SectionTitle("AI BACKEND")
        Spacer(Modifier.height(9.dp))
        NexusPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Connect your own AI service", color = p.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                MutedText("Enter the full endpoint, for example https://your-service.example/api/chat. The provider API key belongs on your backend, never in the APK.")
                NexusTextField(endpoint, { endpoint = it }, "HTTPS chat endpoint", placeholder = "https://your-service.example/api/chat")
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it.take(512) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Optional backend access token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.accent,
                        unfocusedBorderColor = p.border,
                        focusedTextColor = p.text,
                        unfocusedTextColor = p.text,
                        cursorColor = p.accent
                    )
                )
                AccentButton("Save AI settings", Modifier.fillMaxWidth()) {
                    onSaveSettings(endpoint.trim(), accessToken.trim(), lowDataMode)
                    savedNotice = "AI settings saved."
                }
                if (savedNotice.isNotBlank()) MutedText(savedNotice)
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Low-data AI mode", color = p.text, fontWeight = FontWeight.SemiBold)
                        MutedText("Send fewer previous messages as context to reduce request size.")
                    }
                    Switch(checked = lowDataMode, onCheckedChange = {
                        lowDataMode = it
                        onSaveSettings(endpoint.trim(), accessToken.trim(), it)
                    })
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("COMMUNITY CONNECTION")
        Spacer(Modifier.height(9.dp))
        NexusPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                StatusPill(if (firebaseConfigured) "FIREBASE CONFIGURED" else "FIREBASE NOT CONFIGURED", firebaseConfigured)
                Text("Cloud accounts, channels and DMs", color = p.text, fontWeight = FontWeight.Bold)
                MutedText(if (firebaseConfigured) "The app found Firebase configuration. Enable Email/Password Authentication, create Firestore and publish the included rules to use cloud chat." else "Add app/google-services.json, enable Email/Password Authentication and publish firebase/firestore.rules to activate cloud chat.")
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("LOCAL DATA")
        Spacer(Modifier.height(9.dp))
        NexusPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("AI chat history", color = p.text, fontWeight = FontWeight.SemiBold)
                        MutedText("$chatCount messages stored in your local database")
                    }
                    TextButton(onClick = { showClearDialog = true }) { Text("Clear", color = p.accent) }
                }
                MutedText("Notes, tasks, game profiles and workflows are stored locally using Room. Uninstalling the app may remove local data. Export and sync can be added in a future version.")
            }
        }
        Spacer(Modifier.height(20.dp))
        MutedText("DMAX NEXUS v1.0.0 · Phase-complete starter build · Always use the Firebase security rules included with this project before enabling cloud access.")
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear AI history?") },
            text = { Text("This deletes all locally saved AI conversation messages. It does not delete your notes, tasks or game profiles.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearChatHistory()
                    showClearDialog = false
                }) { Text("Clear history") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}
