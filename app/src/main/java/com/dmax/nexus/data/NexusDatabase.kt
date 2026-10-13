package com.dmax.nexus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        GameProfileEntity::class,
        ChatMessageEntity::class,
        WorkflowEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun nexusDao(): NexusDao

    companion object {
        @Volatile private var INSTANCE: NexusDatabase? = null

        fun get(context: Context): NexusDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                NexusDatabase::class.java,
                "dmax_nexus.db"
            ).build().also { INSTANCE = it }
        }
    }
}
