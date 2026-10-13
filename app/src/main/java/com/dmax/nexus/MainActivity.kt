package com.dmax.nexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.dmax.nexus.ai.AiClient
import com.dmax.nexus.cloud.CommunityService
import com.dmax.nexus.data.ChatMessageEntity
import com.dmax.nexus.data.NexusDatabase
import com.dmax.nexus.data.NexusRepository
import com.dmax.nexus.settings.AppPreferences
import com.dmax.nexus.settings.AppSettings
import com.dmax.nexus.ui.AiStudioScreen
import com.dmax.nexus.ui.CommunityScreen
import com.dmax.nexus.ui.HomeScreen
import com.dmax.nexus.ui.LocalNexusPalette
import com.dmax.nexus.ui.NexusTheme
import com.dmax.nexus.ui.ProfileScreen
import com.dmax.nexus.ui.WorkspaceScreen
import kotlinx.coroutines.launch

private data class MainTab(val label: String, val glyph: String)
private val mainTabs = listOf(
    MainTab("Home", "⌂"),
    MainTab("AI", "✧"),
    MainTab("Hub", "▦"),
    MainTab("Community", "◎"),
    MainTab("Profile", "◉")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusApplication() }
    }
}

@Composable
private fun NexusApplication() {
    val context = LocalContext.current.applicationContext
    val repository = remember { NexusRepository(NexusDatabase.get(context).nexusDao()) }
    val preferences = remember { AppPreferences(context) }
    val communityService = remember { CommunityService(context) }
    val settings by preferences.settings.collectAsState(initial = AppSettings())
    val notes by repository.notes().collectAsState(initial = emptyList())
    val tasks by repository.tasks().collectAsState(initial = emptyList())
    val games by repository.games().collectAsState(initial = emptyList())
    val chatMessages by repository.chat().collectAsState(initial = emptyList())
    val workflows by repository.workflows().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var hubSection by rememberSaveable { mutableStateOf("Games") }
    var aiBusy by rememberSaveable { mutableStateOf(false) }
    var pendingPrompt by rememberSaveable { mutableStateOf("") }
    var promptVersion by rememberSaveable { mutableIntStateOf(0) }

    val sendAiMessage: (String) -> Unit = { outgoing ->
        if (outgoing.isNotBlank() && !aiBusy) {
            scope.launch {
                aiBusy = true
                try {
                    repository.addChat("user", outgoing)
                    val contextLimit = if (settings.lowDataMode) 4 else 12
                    val history = (chatMessages.filter { it.role == "user" || it.role == "assistant" }
                        .takeLast((contextLimit - 1).coerceAtLeast(0)) +
                        ChatMessageEntity(role = "user", content = outgoing)).takeLast(contextLimit)
                    val reply = AiClient.sendMessage(settings.aiEndpoint, settings.accessToken, history)
                    repository.addChat("assistant", reply)
                } catch (exception: Exception) {
                    val message = exception.localizedMessage?.take(700)
                        ?: "The request failed. Check your endpoint and connection."
                    repository.addChat("assistant", "I couldn't complete that request: $message")
                } finally {
                    aiBusy = false
                }
            }
        }
    }

    NexusTheme(settings.theme) {
        val palette = LocalNexusPalette.current
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.background,
            bottomBar = {
                NavigationBar(containerColor = palette.surface, contentColor = palette.text) {
                    mainTabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Text(
                                    tab.glyph,
                                    color = if (selectedTab == index) palette.accent else palette.muted,
                                    fontSize = 20.sp
                                )
                            },
                            label = { Text(tab.label, fontSize = 9.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                indicatorColor = palette.elevated,
                                unselectedIconColor = palette.muted,
                                unselectedTextColor = palette.muted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        notes = notes,
                        tasks = tasks,
                        games = games,
                        workflows = workflows,
                        onOpenAi = { selectedTab = 1 },
                        onOpenHub = { section -> hubSection = section; selectedTab = 2 },
                        onOpenCommunity = { selectedTab = 3 }
                    )
                    1 -> AiStudioScreen(
                        messages = chatMessages,
                        isSending = aiBusy,
                        endpointConfigured = InputRules.isSafeHttpsEndpoint(settings.aiEndpoint),
                        initialPrompt = pendingPrompt,
                        promptKey = promptVersion,
                        onSend = sendAiMessage
                    )
                    2 -> WorkspaceScreen(
                        initialSection = hubSection,
                        notes = notes,
                        tasks = tasks,
                        games = games,
                        workflows = workflows,
                        onAddNote = { title, body -> scope.launch { repository.saveNote(title, body) } },
                        onDeleteNote = { id -> scope.launch { repository.deleteNote(id) } },
                        onAddTask = { title, details -> scope.launch { repository.addTask(title, details) } },
                        onToggleTask = { id, complete -> scope.launch { repository.toggleTask(id, complete) } },
                        onDeleteTask = { id -> scope.launch { repository.deleteTask(id) } },
                        onAddGame = { name, host, port, version, notesText ->
                            scope.launch { repository.addGame(name, host, port, version, notesText) }
                        },
                        onDeleteGame = { id -> scope.launch { repository.deleteGame(id) } },
                        onAddWorkflow = { title, prompt -> scope.launch { repository.addWorkflow(title, prompt) } },
                        onDeleteWorkflow = { id -> scope.launch { repository.deleteWorkflow(id) } },
                        onRunWorkflow = { prompt ->
                            pendingPrompt = prompt
                            promptVersion++
                            selectedTab = 1
                        }
                    )
                    3 -> CommunityScreen(service = communityService)
                    else -> ProfileScreen(
                        settings = settings,
                        firebaseConfigured = communityService.isConfigured,
                        chatCount = chatMessages.size,
                        onSaveSettings = { endpoint, token, lowData ->
                            scope.launch {
                                preferences.setAiEndpoint(endpoint)
                                preferences.setAccessToken(token)
                                preferences.setLowDataMode(lowData)
                            }
                        },
                        onThemeChange = { theme -> scope.launch { preferences.setTheme(theme) } },
                        onClearChatHistory = { scope.launch { repository.clearChat() } }
                    )
                }
            }
        }
    }
}
