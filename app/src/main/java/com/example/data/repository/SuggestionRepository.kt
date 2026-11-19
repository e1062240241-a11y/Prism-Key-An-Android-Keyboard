package com.example.data.repository

import com.example.data.local.LearnedWordDao
import com.example.data.local.LearnedWordEntity
import com.example.data.local.TypoPatternDao
import com.example.data.local.TypoPatternEntity
import com.example.data.local.WordTransitionDao
import com.example.data.local.WordTransitionEntity
import kotlinx.coroutines.flow.Flow

class SuggestionRepository(
    private val learnedWordDao: LearnedWordDao,
    private val wordTransitionDao: WordTransitionDao? = null,
    private val typoPatternDao: TypoPatternDao? = null,
) {
    val learnedWords: Flow<List<LearnedWordEntity>> = learnedWordDao.observeAll()

    suspend fun getSnapshot(): List<LearnedWordEntity> = learnedWordDao.getAllSnapshot()

    suspend fun getTransitionsSnapshot(): List<WordTransitionEntity> =
        wordTransitionDao?.getAllSnapshot().orEmpty()

    suspend fun getTypoPatternsSnapshot(): List<TypoPatternEntity> =
        typoPatternDao?.getAllSnapshot().orEmpty()

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

    suspend fun recordTransition(prevWord: String, nextWord: String) {
        val dao = wordTransitionDao ?: return
        val prev = prevWord.trim().lowercase().trimEnd('.', ',', '!', '?')
        val next = nextWord.trim().lowercase().trimEnd('.', ',', '!', '?')
        if (prev.isEmpty() || next.length < 2) return
        val existing = dao.find(prev, next)
        if (existing != null) {
            dao.upsert(
                existing.copy(
                    frequency = existing.frequency + 1,
                    lastUsed = System.currentTimeMillis(),
                ),
            )
        } else {
            dao.upsert(
                WordTransitionEntity(
                    prevWord = prev,
                    nextWord = next,
                    frequency = 1,
                    lastUsed = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun recordTypoPattern(typedInput: String, targetWord: String) {
        val dao = typoPatternDao ?: return
        val typed = typedInput.trim().lowercase()
        val target = targetWord.trim().lowercase()
        if (typed.length < 2 || target.length < 2 || typed == target) return
        val existing = dao.find(typed, target)
        if (existing != null) {
            dao.upsert(
                existing.copy(
                    frequency = existing.frequency + 1,
                    lastUsed = System.currentTimeMillis(),
                ),
            )
        } else {
            dao.upsert(
                TypoPatternEntity(
                    typedInput = typed,
                    targetWord = target,
                    frequency = 1,
                    lastUsed = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun removeWord(word: String) {
        learnedWordDao.deleteWord(word)
    }

    suspend fun clearLearnedWords() {
        learnedWordDao.clearAll()
        wordTransitionDao?.clearAll()
        typoPatternDao?.clearAll()
    }
}

