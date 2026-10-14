package com.dmax.nexus.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmax.nexus.data.ChatMessageEntity
import java.util.Locale

@Composable
fun AiStudioScreen(
    messages: List<ChatMessageEntity>,
    isSending: Boolean,
    endpointConfigured: Boolean,
    initialPrompt: String,
    promptKey: Int,
    onSend: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val p = LocalNexusPalette.current
    val context = LocalContext.current

    var input by rememberSaveable { mutableStateOf("") }
    var speechStatus by rememberSaveable { mutableStateOf("") }
    var lastPrompt by rememberSaveable { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()

            if (spoken.isNotBlank()) {
                input = listOf(input, spoken)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                speechStatus = "Voice text added. Review it before sending."
            } else {
                speechStatus = "No speech was recognized."
            }
        } else {
            speechStatus = "Voice input was cancelled or unavailable."
        }
    }

    val textToSpeech = androidx.compose.runtime.remember(context) {
        var engine: TextToSpeech? = null
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.getDefault()
            }
        }.also { engine = it }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(promptKey) {
        if (initialPrompt.isNotBlank()) {
            input = initialPrompt
        }
    }

    LaunchedEffect(messages.size, isSending) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    fun copyText(text: String) {
        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("NEXUS AI response", text)
        )
        speechStatus = "Copied to clipboard."
    }

    fun sendCurrentPrompt(prompt: String) {
        val outgoing = prompt.trim()
        if (outgoing.isNotBlank() && !isSending) {
            lastPrompt = outgoing
            onSend(outgoing)
            input = ""
            speechStatus = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        ScreenHeader(
            "INTELLIGENCE",
            "AI Studio",
            "Think, draft and solve problems with your personal assistant."
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                if (endpointConfigured) "BACKEND SET" else "LOCAL MODE",
                positive = endpointConfigured
            )
            MutedText(
                if (endpointConfigured)
                    "AI requests use your configured backend."
                else
                    "Configure an AI backend for live replies."
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuietButton("💻 Coding") {
                input = "Help me write, debug and improve code. Explain the solution step by step."
            }
            QuietButton("📚 Study") {
                input = "Help me study a topic. Explain it simply and give me practice questions."
            }
            QuietButton("⛏ Minecraft") {
                input = "Help me with Minecraft building, survival strategies, redstone or server setup."
            }
            QuietButton("✍ Writing") {
                input = "Help me create clear, original writing. Ask questions if you need more details."
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuietButton(
                text = "🔄 Retry",
                enabled = !isSending
            ) {
                val previous = lastPrompt.ifBlank {
                    messages.lastOrNull { it.role == "user" }?.content.orEmpty()
                }
                if (previous.isNotBlank()) {
                    sendCurrentPrompt(previous)
                } else {
                    speechStatus = "There is no previous prompt to retry."
                }
            }

            QuietButton(
                text = "⏹ Stop voice"
            ) {
                textToSpeech.stop()
                speechStatus = "Voice playback stopped."
            }

            QuietButton(
                text = "🧹 Clear chat",
                enabled = !isSending
            ) {
                textToSpeech.stop()
                onClearChat()
                lastPrompt = ""
                speechStatus = "Chat history cleared."
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                NexusPanel(Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "Your next idea starts here.",
                            color = p.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        MutedText(
                            "Ask a question or choose a quick prompt above."
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Tip: configure a secure HTTPS backend from Profile → AI backend.",
                            color = p.accent,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            messages.forEach { item ->
                ChatBubble(
                    message = item,
                    onCopy = { copyText(item.content) },
                    onSpeak = {
                        textToSpeech.stop()
                        val result = textToSpeech.speak(
                            item.content,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "nexus-${item.hashCode()}"
                        )
                        speechStatus = if (
                            result == TextToSpeech.ERROR
                        ) {
                            "Unable to start voice playback."
                        } else {
                            "Reading message aloud."
                        }
                    }
                )
            }

            if (isSending) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp,
                        color = p.accent
                    )
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
                val intent = Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_PROMPT,
                        "Speak your message"
                    )
                }

                runCatching {
                    voiceLauncher.launch(intent)
                }.onFailure {
                    speechStatus =
                        "Speech recognition is unavailable on this device."
                }
            }

            AccentButton(
                "Send",
                enabled = !isSending && input.isNotBlank()
            ) {
                sendCurrentPrompt(input)
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val p = LocalNexusPalette.current
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        NexusPanel(
            modifier = Modifier.fillMaxWidth(0.95f)
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

                Text(
                    message.content,
                    color = p.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuietButton("📋 Copy", onClick = onCopy)

                    if (!isUser) {
                        QuietButton("🔊 Read", onClick = onSpeak)
                    }
                }
            }
        }
    }
}
