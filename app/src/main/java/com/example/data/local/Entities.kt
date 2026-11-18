package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_items")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String = "Text",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
)

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "stickers",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packId")],
)
data class StickerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: Long,
    val label: String,
    val drawableResName: String? = null,
    val localFilePath: String? = null,
    val emojiFallback: String = "✨",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "learned_words")
data class LearnedWordEntity(
    @PrimaryKey val word: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val isUserAdded: Boolean = true,
)
