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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dmax.nexus.InputRules
import com.dmax.nexus.data.GameProfileEntity
import com.dmax.nexus.data.NoteEntity
import com.dmax.nexus.data.TaskEntity
import com.dmax.nexus.data.WorkflowEntity

private val workspaceSections = listOf("Games", "Notes", "Tasks", "Flows")

@Composable
fun WorkspaceScreen(
    initialSection: String,
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    games: List<GameProfileEntity>,
    workflows: List<WorkflowEntity>,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onAddTask: (String, String) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onAddGame: (String, String, Int, String, String) -> Unit,
    onDeleteGame: (Long) -> Unit,
    onAddWorkflow: (String, String) -> Unit,
    onDeleteWorkflow: (Long) -> Unit,
    onRunWorkflow: (String) -> Unit
) {
    var selectedSection by rememberSaveable { mutableStateOf(initialSection) }
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showGameDialog by rememberSaveable { mutableStateOf(false) }
    var showWorkflowDialog by rememberSaveable { mutableStateOf(false) }
    val p = LocalNexusPalette.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        ScreenHeader("YOUR TOOLKIT", "Workspace", "Your local-first tools, saved on this device.")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            workspaceSections.forEach { section ->
                val active = selectedSection == section
                Button(
                    onClick = { selectedSection = section },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) p.accent else p.surface,
                        contentColor = if (active) p.background else p.text
                    )
                ) { Text(section, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(16.dp))
        when (selectedSection) {
            "Notes" -> NotesSection(notes, { showNoteDialog = true }, onDeleteNote)
            "Tasks" -> TasksSection(tasks, { showTaskDialog = true }, onToggleTask, onDeleteTask)
            "Flows" -> WorkflowsSection(workflows, { showWorkflowDialog = true }, onDeleteWorkflow, onRunWorkflow)
            else -> GamesSection(games, { showGameDialog = true }, onDeleteGame)
        }
    }

    if (showNoteDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var body by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Create note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NexusTextField(title, { title = it }, "Title")
                    NexusTextField(body, { body = it }, "Note content", singleLine = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onAddNote(title, body)
                        showNoteDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTaskDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var details by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTaskDialog = false },
            title = { Text("Add task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NexusTextField(title, { title = it }, "Task title")
                    NexusTextField(details, { details = it }, "Details (optional)", singleLine = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onAddTask(title, details)
                        showTaskDialog = false
                    }
                }) { Text("Add task") }
            },
            dismissButton = { TextButton(onClick = { showTaskDialog = false }) { Text("Cancel") } }
        )
    }

    if (showGameDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var host by rememberSaveable { mutableStateOf("") }
        var port by rememberSaveable { mutableStateOf("25565") }
        var version by rememberSaveable { mutableStateOf("1.21.x") }
        var notesText by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGameDialog = false },
            title = { Text("Save game server") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NexusTextField(name, { name = it }, "Profile name")
                    NexusTextField(host, { host = it }, "Host / server address")
                    NexusTextField(port, { port = it.filter(Char::isDigit).take(5) }, "Port")
                    NexusTextField(version, { version = it }, "Game version")
                    NexusTextField(notesText, { notesText = it }, "Notes (optional)", singleLine = false)
                    if (error.isNotBlank()) MutedText(error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsedPort = InputRules.parsePort(port)
                    error = when {
                        name.isBlank() -> "Enter a profile name."
                        host.isBlank() -> "Enter the server address."
                        parsedPort == null -> "Port must be between 1 and 65535."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        onAddGame(name, host, parsedPort!!, version, notesText)
                        showGameDialog = false
                    }
                }) { Text("Save server") }
            },
            dismissButton = { TextButton(onClick = { showGameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showWorkflowDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var prompt by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWorkflowDialog = false },
            title = { Text("Create workflow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NexusTextField(title, { title = it }, "Workflow name")
                    NexusTextField(prompt, { prompt = it }, "Reusable prompt / instructions", singleLine = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && prompt.isNotBlank()) {
                        onAddWorkflow(title, prompt)
                        showWorkflowDialog = false
                    }
                }) { Text("Save workflow") }
            },
            dismissButton = { TextButton(onClick = { showWorkflowDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NotesSection(notes: List<NoteEntity>, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("My notes", "${notes.size} saved")
        AccentButton("+ New note", Modifier.fillMaxWidth(), onClick = onAdd)
        if (notes.isEmpty()) EmptyState("No notes yet", "Save ideas, study notes or snippets here.")
        notes.forEach { note ->
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(note.title, color = LocalNexusPalette.current.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    MutedText(note.body, maxLines = 6)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDelete(note.id) }) { Text("Delete", color = LocalNexusPalette.current.muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksSection(
    tasks: List<TaskEntity>, onAdd: () -> Unit,
    onToggle: (Long, Boolean) -> Unit, onDelete: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("My tasks", "${tasks.count { !it.completed }} open")
        AccentButton("+ Add task", Modifier.fillMaxWidth(), onClick = onAdd)
        if (tasks.isEmpty()) EmptyState("Your list is clear", "Add a small next step to get started.")
        tasks.forEach { task ->
            NexusPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.completed,
                        onCheckedChange = { onToggle(task.id, it) },
                        colors = CheckboxDefaults.colors(checkedColor = LocalNexusPalette.current.accent)
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 7.dp)) {
                        Text(task.title, color = LocalNexusPalette.current.text, fontWeight = FontWeight.SemiBold)
                        if (task.details.isNotBlank()) MutedText(task.details, maxLines = 3)
                    }
                    TextButton(onClick = { onDelete(task.id) }) { Text("×", color = LocalNexusPalette.current.muted, fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun GamesSection(games: List<GameProfileEntity>, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("GameSphere", "${games.size} saved servers")
        MutedText("Keep connection details, version notes and your personal survival references in one place.")
        AccentButton("+ Add server profile", Modifier.fillMaxWidth(), onClick = onAdd)
        if (games.isEmpty()) EmptyState("No server profiles", "Add a server address and port to keep the details handy.")
        games.forEach { game ->
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▣", color = LocalNexusPalette.current.accent, fontSize = 22.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(game.name, color = LocalNexusPalette.current.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            MutedText("${game.host}:${game.port}")
                        }
                        StatusPill(game.version)
                    }
                    if (game.notes.isNotBlank()) MutedText(game.notes, maxLines = 4)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDelete(game.id) }) { Text("Delete profile", color = LocalNexusPalette.current.muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowsSection(
    workflows: List<WorkflowEntity>, onAdd: () -> Unit,
    onDelete: (Long) -> Unit, onRun: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Flow Studio", "${workflows.size} saved")
        MutedText("Create reusable prompts for study plans, coding reviews, brainstorming or project planning.")
        AccentButton("+ Create workflow", Modifier.fillMaxWidth(), onClick = onAdd)
        if (workflows.isEmpty()) EmptyState("No workflows yet", "Add a reusable prompt and send it to AI Studio whenever you need it.")
        workflows.forEach { workflow ->
            NexusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(workflow.title, color = LocalNexusPalette.current.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    MutedText(workflow.prompt, maxLines = 4)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentButton("Run in AI", Modifier.weight(1f), onClick = { onRun(workflow.prompt) })
                        TextButton(onClick = { onDelete(workflow.id) }) { Text("Delete", color = LocalNexusPalette.current.muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    NexusPanel(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text("✧", color = LocalNexusPalette.current.accent, fontSize = 25.sp)
            Spacer(Modifier.height(7.dp))
            Text(title, color = LocalNexusPalette.current.text, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            MutedText(description)
        }
    }
}
