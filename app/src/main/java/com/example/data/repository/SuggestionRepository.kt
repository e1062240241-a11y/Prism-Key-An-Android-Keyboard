package com.example.data.repository

import com.example.data.local.LearnedWordDao
import com.example.data.local.LearnedWordEntity
import kotlinx.coroutines.flow.Flow

class SuggestionRepository(
    private val learnedWordDao: LearnedWordDao,
) {
    val learnedWords: Flow<List<LearnedWordEntity>> = learnedWordDao.observeAll()

    suspend fun getSnapshot(): List<LearnedWordEntity> = learnedWordDao.getAllSnapshot()

    suspend fun learnWord(word: String, userAdded: Boolean = false) {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || !clean.all { it.isLetter() || it == '\'' }) return
        val existing = learnedWordDao.findWord(clean)
        if (existing != null) {
            learnedWordDao.upsert(
                existing.copy(
                    frequency = existing.frequency + 1,
                    lastUsed = System.currentTimeMillis(),
                    isUserAdded = existing.isUserAdded || userAdded,
                ),
            )
        } else {
            learnedWordDao.upsert(
                LearnedWordEntity(
                    word = clean,
                    frequency = if (userAdded) 15 else 1,
                    lastUsed = System.currentTimeMillis(),
                    isUserAdded = userAdded,
                ),
            )
        }
    }

    suspend fun removeWord(word: String) {
        learnedWordDao.deleteWord(word)
    }

    suspend fun clearLearnedWords() {
        learnedWordDao.clearAll()
    }
}
