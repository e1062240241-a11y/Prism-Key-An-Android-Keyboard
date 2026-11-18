package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClipboardItemEntity::class,
        StickerPackEntity::class,
        StickerEntity::class,
        LearnedWordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
    abstract fun stickerDao(): StickerDao
    abstract fun learnedWordDao(): LearnedWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prismkeys_local_vault.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
