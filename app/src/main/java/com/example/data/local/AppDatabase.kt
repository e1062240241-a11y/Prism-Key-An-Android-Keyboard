package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClipboardItemEntity::class,
        StickerPackEntity::class,
        StickerEntity::class,
        LearnedWordEntity::class,
        WordTransitionEntity::class,
        TypoPatternEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
    abstract fun stickerDao(): StickerDao
    abstract fun learnedWordDao(): LearnedWordDao
    abstract fun wordTransitionDao(): WordTransitionDao
    abstract fun typoPatternDao(): TypoPatternDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `word_transitions` (" +
                        "`prevWord` TEXT NOT NULL, " +
                        "`nextWord` TEXT NOT NULL, " +
                        "`frequency` INTEGER NOT NULL, " +
                        "`lastUsed` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`prevWord`, `nextWord`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `typo_patterns` (" +
                        "`typedInput` TEXT NOT NULL, " +
                        "`targetWord` TEXT NOT NULL, " +
                        "`frequency` INTEGER NOT NULL, " +
                        "`lastUsed` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`typedInput`, `targetWord`))",
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prismkeys_local_vault.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

