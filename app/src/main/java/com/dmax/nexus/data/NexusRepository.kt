package com.dmax.nexus.data

import kotlinx.coroutines.flow.Flow

class NexusRepository(private val dao: NexusDao) {
    fun notes(): Flow<List<NoteEntity>> = dao.observeNotes()
    suspend fun saveNote(title: String, body: String, id: Long = 0) =
        dao.saveNote(NoteEntity(id = id, title = title.trim(), body = body.trim()))
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    fun tasks(): Flow<List<TaskEntity>> = dao.observeTasks()
    suspend fun addTask(title: String, details: String) =
        dao.saveTask(TaskEntity(title = title.trim(), details = details.trim()))
    suspend fun toggleTask(id: Long, completed: Boolean) = dao.updateTaskCompletion(id, completed)
    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    fun games(): Flow<List<GameProfileEntity>> = dao.observeGameProfiles()
    suspend fun addGame(name: String, host: String, port: Int, version: String, notes: String) =
        dao.saveGameProfile(GameProfileEntity(
            name = name.trim(), host = host.trim(), port = port,
            version = version.trim().ifBlank { "Unknown" }, notes = notes.trim()
        ))
    suspend fun deleteGame(id: Long) = dao.deleteGameProfile(id)

    fun chat(): Flow<List<ChatMessageEntity>> = dao.observeChatMessages()
    suspend fun addChat(role: String, content: String) {
        dao.saveChatMessage(ChatMessageEntity(role = role, content = content.trim()))
        dao.trimChatHistory()
    }
    suspend fun clearChat() = dao.clearChatMessages()

    fun workflows(): Flow<List<WorkflowEntity>> = dao.observeWorkflows()
    suspend fun addWorkflow(title: String, prompt: String) =
        dao.saveWorkflow(WorkflowEntity(title = title.trim(), prompt = prompt.trim()))
    suspend fun deleteWorkflow(id: Long) = dao.deleteWorkflow(id)
}
