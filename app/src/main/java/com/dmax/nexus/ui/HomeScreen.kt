package com.dmax.nexus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmax.nexus.data.GameProfileEntity
import com.dmax.nexus.data.NoteEntity
import com.dmax.nexus.data.TaskEntity
import com.dmax.nexus.data.WorkflowEntity

@Composable
fun HomeScreen(
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    games: List<GameProfileEntity>,
    workflows: List<WorkflowEntity>,
    onOpenAi: () -> Unit,
    onOpenHub: (String) -> Unit,
    onOpenCommunity: () -> Unit
) {
    val p = LocalNexusPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
        BrandHeader()
        Spacer(Modifier.height(28.dp))
        Text("Welcome to your universe.", color = p.text, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        MutedText("One calm workspace for your ideas, tools and gaming life.")
        Spacer(Modifier.height(19.dp))
        NexusTextField(
            value = query,
            onValueChange = { query = it },
            label = "Global search",
            placeholder = "Search notes, tasks, games, workflows..."
        )
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            val noteHits = notes.filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) }
            val taskHits = tasks.filter { it.title.lowercase().contains(q) || it.details.lowercase().contains(q) }
            val gameHits = games.filter { it.name.lowercase().contains(q) || it.host.lowercase().contains(q) || it.version.lowercase().contains(q) }
            val flowHits = workflows.filter { it.title.lowercase().contains(q) || it.prompt.lowercase().contains(q) }
            Spacer(Modifier.height(12.dp))
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Search results", "${noteHits.size + taskHits.size + gameHits.size + flowHits.size} matches")
                    noteHits.take(3).forEach { Text("▤  Note · ${it.title}", color = p.text, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                    taskHits.take(3).forEach { Text("☑  Task · ${it.title}", color = p.text, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                    gameHits.take(3).forEach { Text("▣  Game · ${it.name} (${it.host})", color = p.text, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                    flowHits.take(3).forEach { Text("⌘  Workflow · ${it.title}", color = p.text, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                    if (noteHits.isEmpty() && taskHits.isEmpty() && gameHits.isEmpty() && flowHits.isEmpty()) {
                        MutedText("No local results found.")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        NexusPanel(Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✧", color = p.accent, fontSize = 27.sp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("NEXUS AI", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("YOUR CREATIVE COPILOT", color = p.accent, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                    StatusPill("READY")
                }
                Spacer(Modifier.height(13.dp))
                Text("What are we building today?", color = p.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText("Chat history is saved locally. Add your own secure AI backend in Profile to enable live replies.")
                Spacer(Modifier.height(14.dp))
                AccentButton("Open AI Studio  →", Modifier.fillMaxWidth(), onClick = onOpenAi)
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle("WORKSPACE OVERVIEW", "LIVE LOCAL DATA")
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "NOTES", notes.size.toString(), "Personal knowledge", onClick = { onOpenHub("Notes") })
            StatCard(Modifier.weight(1f), "TASKS", tasks.count { !it.completed }.toString(), "Open tasks", onClick = { onOpenHub("Tasks") })
            StatCard(Modifier.weight(1f), "GAMES", games.size.toString(), "Saved profiles", onClick = { onOpenHub("Games") })
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("QUICK ACCESS")
        Spacer(Modifier.height(10.dp))
        NexusPanel(Modifier.fillMaxWidth(), onClick = { onOpenHub("Games") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▣", fontSize = 25.sp, color = p.accent)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("GameSphere", color = p.text, fontWeight = FontWeight.Bold)
                    MutedText("Minecraft server profiles, versions and notes")
                }
                Text("→", color = p.accent, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        NexusPanel(Modifier.fillMaxWidth(), onClick = { onOpenHub("Flows") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⌘", fontSize = 25.sp, color = p.accent)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Flow Studio", color = p.text, fontWeight = FontWeight.Bold)
                    MutedText("Saved prompts and reusable workflows · ${workflows.size} saved")
                }
                Text("→", color = p.accent, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        NexusPanel(Modifier.fillMaxWidth(), onClick = onOpenCommunity) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◎", fontSize = 25.sp, color = p.accent)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("NEXUS Community", color = p.text, fontWeight = FontWeight.Bold)
                    MutedText("Cloud channels and direct messages need Firebase setup")
                }
                Text("→", color = p.accent, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        MutedText("DMAX NEXUS · Built around your workflow, not the other way around.")
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, caption: String, onClick: () -> Unit) {
    val p = LocalNexusPalette.current
    NexusPanel(modifier, onClick) {
        Column {
            Text(label, color = p.muted, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(value, color = p.accent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(3.dp))
            Text(caption, color = p.muted, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}
