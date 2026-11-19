package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.SuggestionRepository
import com.example.engine.SuggestionEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SuggestionPatternRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: SuggestionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SuggestionRepository(
            learnedWordDao = db.learnedWordDao(),
            wordTransitionDao = db.wordTransitionDao(),
            typoPatternDao = db.typoPatternDao(),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun learnedWordsTransitionsAndTypos_persistInRoomAndReloadIntoTrie() = runBlocking {
        repo.learnWord("espresso", userAdded = true)
        repo.learnWord("barista", userAdded = false)
        repo.recordTransition("morning", "espresso")
        repo.recordTransition("morning", "espresso")
        repo.recordTypoPattern("espreso", "espresso")

        val engine = SuggestionEngine(repo)
        engine.refreshLearnedWords()

        val nextAfterMorning = engine.suggestNextWords("morning")
        assertEquals("espresso", nextAfterMorning.firstOrNull())

        val prefixRes = engine.suggestForComposing("esp", previousWord = "morning")
        assertEquals("espresso", prefixRes.suggestions.firstOrNull())

        val typoRes = engine.suggestForComposing("espreso", autocorrectEnabled = true)
        assertEquals("espresso", typoRes.autocorrectCandidate)

        repo.clearLearnedWords()
        engine.refreshLearnedWords()
        assertEquals(0, engine.learnedTransitionCount())
        assertEquals(0, engine.learnedTypoPatternCount())
        assertTrue(engine.trieWordCount() > 100)
    }
}
