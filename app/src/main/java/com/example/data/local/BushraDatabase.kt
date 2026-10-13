package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ActionHistoryDao
import com.example.data.local.dao.ApiProfileDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.entity.ActionHistoryEntity
import com.example.data.local.entity.ApiProfileEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        NoteEntity::class,
        ReminderEntity::class,
        ActionHistoryEntity::class,
        ApiProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BushraDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun actionHistoryDao(): ActionHistoryDao
    abstract fun apiProfileDao(): ApiProfileDao

    companion object {
        @Volatile
        private var INSTANCE: BushraDatabase? = null

        fun getInstance(context: Context): BushraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BushraDatabase::class.java,
                    "bushra_ai_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
