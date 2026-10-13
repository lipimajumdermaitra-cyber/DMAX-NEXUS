package com.dmax.nexus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NexusDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM tasks ORDER BY completed ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTask(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM game_profiles ORDER BY createdAt DESC")
    fun observeGameProfiles(): Flow<List<GameProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameProfile(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE id = :id")
    suspend fun deleteGameProfile(id: Long)

    @Query("SELECT * FROM ai_messages ORDER BY createdAt ASC LIMIT 200")
    fun observeChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM ai_messages")
    suspend fun clearChatMessages()

    @Query("DELETE FROM ai_messages WHERE id NOT IN (SELECT id FROM ai_messages ORDER BY createdAt DESC LIMIT 200)")
    suspend fun trimChatHistory()

    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun observeWorkflows(): Flow<List<WorkflowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorkflow(workflow: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteWorkflow(id: Long)
}
