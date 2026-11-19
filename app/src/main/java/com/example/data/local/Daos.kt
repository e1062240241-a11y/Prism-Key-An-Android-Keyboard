package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC")
    fun observeAll(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_items WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): ClipboardItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItemEntity): Long

    @Query("UPDATE clipboard_items SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE clipboard_items SET timestamp = :timestamp, usageCount = usageCount + 1 WHERE id = :id")
    suspend fun markUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun count(): Int
}

@Dao
interface StickerDao {
    @Query("SELECT * FROM sticker_packs ORDER BY isBuiltIn DESC, createdAt ASC")
    fun observePacks(): Flow<List<StickerPackEntity>>

    @Query("SELECT * FROM stickers ORDER BY createdAt ASC")
    fun observeAllStickers(): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE packId = :packId ORDER BY createdAt ASC")
    fun observeStickersByPack(packId: Long): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StickerPackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity): Long

    @Query("DELETE FROM sticker_packs WHERE id = :packId AND isBuiltIn = 0")
    suspend fun deleteCustomPack(packId: Long)

    @Query("DELETE FROM stickers WHERE id = :stickerId")
    suspend fun deleteSticker(stickerId: Long)

    @Query("SELECT COUNT(*) FROM sticker_packs")
    suspend fun packCount(): Int

    @Transaction
    suspend fun createPackWithStickers(pack: StickerPackEntity, stickers: List<StickerEntity>): Long {
        val id = insertPack(pack)
        stickers.forEach { insertSticker(it.copy(packId = id)) }
        return id
    }
}

@Dao
interface LearnedWordDao {
    @Query("SELECT * FROM learned_words ORDER BY frequency DESC, lastUsed DESC")
    fun observeAll(): Flow<List<LearnedWordEntity>>

    @Query("SELECT * FROM learned_words")
    suspend fun getAllSnapshot(): List<LearnedWordEntity>

    @Query("SELECT * FROM learned_words WHERE word = :word LIMIT 1")
    suspend fun findWord(word: String): LearnedWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LearnedWordEntity)

    @Query("DELETE FROM learned_words WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM learned_words")
    suspend fun clearAll()
}

@Dao
interface WordTransitionDao {
    @Query("SELECT * FROM word_transitions ORDER BY frequency DESC, lastUsed DESC")
    suspend fun getAllSnapshot(): List<WordTransitionEntity>

    @Query("SELECT * FROM word_transitions WHERE prevWord = :prevWord AND nextWord = :nextWord LIMIT 1")
    suspend fun find(prevWord: String, nextWord: String): WordTransitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WordTransitionEntity)

    @Query("DELETE FROM word_transitions")
    suspend fun clearAll()
}

@Dao
interface TypoPatternDao {
    @Query("SELECT * FROM typo_patterns ORDER BY frequency DESC, lastUsed DESC")
    suspend fun getAllSnapshot(): List<TypoPatternEntity>

    @Query("SELECT * FROM typo_patterns WHERE typedInput = :typedInput AND targetWord = :targetWord LIMIT 1")
    suspend fun find(typedInput: String, targetWord: String): TypoPatternEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TypoPatternEntity)

    @Query("DELETE FROM typo_patterns")
    suspend fun clearAll()
}

