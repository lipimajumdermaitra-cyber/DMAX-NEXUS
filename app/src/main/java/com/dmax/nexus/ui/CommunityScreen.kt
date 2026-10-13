package com.dmax.nexus.ui

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmax.nexus.InputRules
import com.dmax.nexus.cloud.CloudMessage
import com.dmax.nexus.cloud.CommunityService
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val communityChannels = listOf("general", "gaming", "showcase")

@Composable
fun CommunityScreen(service: CommunityService) {
    val p = LocalNexusPalette.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var authVersion by rememberSaveable { mutableStateOf(0) }
    var status by rememberSaveable { mutableStateOf("") }
    var working by rememberSaveable { mutableStateOf(false) }
    var channel by rememberSaveable { mutableStateOf("general") }
    var dmInput by rememberSaveable { mutableStateOf("") }
    var activeDmUid by rememberSaveable { mutableStateOf("") }
    var directMode by rememberSaveable { mutableStateOf(false) }
    var messageInput by rememberSaveable { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<CloudMessage>>(emptyList()) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val user = service.currentUser

    DisposableEffect(service, authVersion, channel, directMode, activeDmUid) {
        val listener = if (service.currentUser != null && (!directMode || activeDmUid.isNotBlank())) {
            service.observeMessages(
                channel = channel,
                otherUid = if (directMode) activeDmUid else null,
                onMessages = { messages = it; status = "" },
                onError = { status = it }
            )
        } else null
        onDispose { listener?.remove() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
        ScreenHeader("CONNECT", "Community", "Channels and direct messages, with account-controlled access.")

        if (!service.isConfigured) {
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatusPill("SETUP REQUIRED", positive = false)
                    Text("Connect your Firebase project", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    MutedText("Cloud messaging is implemented but remains disabled until you add your Firebase configuration.")
                    MutedText("1. Create a Firebase project and register Android app ID: com.dmax.nexus")
                    MutedText("2. Enable Email/Password in Authentication and create a Cloud Firestore database.")
                    MutedText("3. Download google-services.json into the app/ folder.")
                    MutedText("4. Publish the firebase/firestore.rules file from this project to Firestore Rules.")
                    MutedText("5. Push the changed project to GitHub and build again.")
                }
            }
        } else if (user == null) {
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text(if (createAccount) "Create your NEXUS account" else "Sign in to NEXUS", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MutedText("Use an email and a unique password. Do not reuse a password from another account.")
                    NexusTextField(email, { email = it }, "Email address")
                    androidx.compose.material3.OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (status.isNotBlank()) MutedText(status)
                    AccentButton(
                        if (createAccount) "Create account" else "Sign in",
                        Modifier.fillMaxWidth(),
                        enabled = !working && InputRules.isValidEmail(email) && password.isNotBlank()
                    ) {
                        scope.launch {
                            working = true
                            status = ""
                            try {
                                if (createAccount) {
                                    if (!InputRules.isValidPassword(password)) error("Use a password between 8 and 128 characters.")
                                    service.createAccount(email, password)
                                } else {
                                    service.signIn(email, password)
                                }
                                authVersion++
                                status = "Signed in successfully."
                            } catch (e: Exception) {
                                status = e.localizedMessage ?: "Authentication failed. Check your connection and Firebase settings."
                            } finally {
                                working = false
                            }
                        }
                    }
                    TextButton(onClick = { createAccount = !createAccount; status = "" }) {
                        Text(if (createAccount) "Already have an account? Sign in" else "New here? Create an account", color = p.accent)
                    }
                }
            }
        } else {
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SIGNED IN", color = p.accent, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                            Text(user.email.orEmpty(), color = p.text, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = { confirmSignOut = true }) { Text("Sign out", color = p.muted) }
                    }
                    MutedText("Your user ID is the safe way to start a direct conversation. Share it only with people you trust.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.uid, color = p.muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { clipboard.setText(AnnotatedString(user.uid)); status = "User ID copied." }) { Text("Copy ID", color = p.accent) }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentButton("Channels", Modifier.weight(1f), enabled = directMode) { directMode = false; status = "" }
                AccentButton("Direct message", Modifier.weight(1f), enabled = !directMode) { directMode = true; status = "" }
            }
            Spacer(Modifier.height(10.dp))
            if (!directMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    communityChannels.forEach { value ->
                        if (value == channel) AccentButton("# $value", onClick = { channel = value })
                        else QuietButton("# $value", onClick = { channel = value })
                    }
                }
            } else {
                NexusPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Open direct message", color = p.text, fontWeight = FontWeight.Bold)
                        NexusTextField(dmInput, { dmInput = it.take(128) }, "Other user's Firebase UID")
                        AccentButton("Open conversation", Modifier.fillMaxWidth(), enabled = dmInput.isNotBlank()) {
                            scope.launch {
                                try {
                                    activeDmUid = service.prepareDirectConversation(dmInput)
                                    status = "Conversation opened. The other person must sign in and open your UID to join."
                                } catch (e: Exception) {
                                    status = e.localizedMessage ?: "Could not open conversation."
                                }
                            }
                        }
                        if (activeDmUid.isNotBlank()) MutedText("Current conversation with UID: $activeDmUid")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (status.isNotBlank()) {
                NexusPanel(Modifier.fillMaxWidth()) { MutedText(status) }
                Spacer(Modifier.height(9.dp))
            }
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(if (directMode) "Direct messages" else "# $channel", "LIVE CLOUD CHAT")
                    if (directMode && activeDmUid.isBlank()) {
                        MutedText("Enter another person's UID above to open a direct conversation.")
                    } else if (messages.isEmpty()) {
                        MutedText("No messages yet. Start the conversation with a respectful message.")
                    } else {
                        messages.forEach { message ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(message.authorName, color = if (message.authorId == user.uid) p.accent else p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (message.createdAtMillis > 0L) {
                                        Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAtMillis)), color = p.muted, fontSize = 9.sp)
                                    }
                                }
                                Text(message.text, color = p.text, fontSize = 13.sp, lineHeight = 18.sp)
                                if (message.authorId != user.uid) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            status = runCatching {
                                                service.reportMessage(message, if (directMode) "direct" else channel)
                                                "Report submitted for moderator review."
                                            }.getOrElse { it.localizedMessage ?: "Report failed." }
                                        }
                                    }) { Text("Report message", color = p.muted, fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            NexusTextField(messageInput, { messageInput = it.take(2000) }, "Write a message", singleLine = false)
            Spacer(Modifier.height(8.dp))
            AccentButton("Send message", Modifier.fillMaxWidth(), enabled = messageInput.isNotBlank() && (!directMode || activeDmUid.isNotBlank())) {
                val outgoing = messageInput.trim()
                scope.launch {
                    try {
                        service.sendMessage(channel, if (directMode) activeDmUid else null, outgoing)
                        messageInput = ""
                        status = ""
                    } catch (e: Exception) {
                        status = e.localizedMessage ?: "Message could not be sent."
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            MutedText("Be respectful. Block people outside the app if needed. Reports are stored for the project owner; this starter has no staffed moderation team.")
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access cloud messages.") },
            confirmButton = {
                TextButton(onClick = {
                    service.signOut()
                    authVersion++
                    messages = emptyList()
                    status = "Signed out."
                    confirmSignOut = false
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}
