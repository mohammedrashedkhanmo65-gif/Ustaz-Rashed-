package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ContactRequestEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        CallHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RMCallDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactRequestDao(): ContactRequestDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun callHistoryDao(): CallHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: RMCallDatabase? = null

        fun getInstance(context: Context): RMCallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RMCallDatabase::class.java,
                    "rm_call_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
