package com.dmax.nexus.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmax.nexus.data.ChatMessageEntity

@Composable
fun AiStudioScreen(
    messages: List<ChatMessageEntity>,
    isSending: Boolean,
    endpointConfigured: Boolean,
    initialPrompt: String,
    promptKey: Int,
    onSend: (String) -> Unit
) {
    val p = LocalNexusPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var speechStatus by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                input = listOf(input, spoken).filter { it.isNotBlank() }.joinToString(" ")
                speechStatus = "Voice text added. Review it before sending."
            } else {
                speechStatus = "No speech was recognized."
            }
        } else {
            speechStatus = "Voice input was cancelled or is unavailable on this device."
        }
    }

    LaunchedEffect(promptKey) {
        if (initialPrompt.isNotBlank()) input = initialPrompt
    }
    LaunchedEffect(messages.size, isSending) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        ScreenHeader("INTELLIGENCE", "AI Studio", "Think, draft and solve problems with your personal assistant.")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(if (endpointConfigured) "BACKEND SET" else "LOCAL MODE", positive = endpointConfigured)
            MutedText(if (endpointConfigured) "AI requests use your configured backend." else "Messages save locally until you configure an AI backend.")
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                NexusPanel(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Your next idea starts here.", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        MutedText("Try a study plan, ask for help with Kotlin, or save a reusable prompt in Flow Studio.")
                        Spacer(Modifier.height(10.dp))
                        Text("Tip: configure a secure HTTPS backend from Profile → AI backend to enable live replies.", color = p.accent, fontSize = 12.sp)
                    }
                }
            }
            messages.forEach { item ->
                ChatBubble(item)
            }
            if (isSending) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp, color = p.accent)
                    MutedText("NEXUS is working…")
                }
            }
        }
        if (speechStatus.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            MutedText(speechStatus)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            NexusTextField(
                value = input,
                onValueChange = { input = it.take(4000) },
                label = "Message NEXUS",
                placeholder = "Ask, plan or create…",
                modifier = Modifier.weight(1f),
                singleLine = false
            )
            QuietButton("Mic") {
                speechStatus = ""
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
                }
                runCatching { voiceLauncher.launch(intent) }
                    .onFailure { speechStatus = "Speech recognition is not available on this device." }
            }
            AccentButton("Send", enabled = !isSending && input.isNotBlank()) {
                val outgoing = input.trim()
                if (outgoing.isNotBlank()) {
                    onSend(outgoing)
                    input = ""
                    speechStatus = ""
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageEntity) {
    val p = LocalNexusPalette.current
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        NexusPanel(
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            Column {
                Text(
                    if (isUser) "YOU" else "NEXUS AI",
                    color = if (isUser) p.muted else p.accent,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(message.content, color = p.text, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}
