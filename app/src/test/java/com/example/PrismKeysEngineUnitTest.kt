package com.example

import com.example.data.local.LearnedWordEntity
import com.example.data.local.LearnedWordDao
import com.example.data.repository.SuggestionRepository
import com.example.engine.BundledOfflineLexicon
import com.example.engine.GestureDecoder
import com.example.engine.LanguageCatalog
import com.example.engine.SuggestionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismKeysEngineUnitTest {

    private val fakeDao = object : LearnedWordDao {
        override fun observeAll(): Flow<List<LearnedWordEntity>> = flowOf(emptyList())
        override suspend fun getAllSnapshot(): List<LearnedWordEntity> = emptyList()
        override suspend fun findWord(word: String): LearnedWordEntity? = null
        override suspend fun upsert(entry: LearnedWordEntity) {}
        override suspend fun deleteWord(word: String) {}
        override suspend fun clearAll() {}
    }

    @Test
    fun offlineLexicon_translatesPhrasesAcrossMultipleLanguages() {
        val es = BundledOfflineLexicon.translate("Good morning! Thank you.", "en", "es")
        assertTrue(es.contains("Buenos días", ignoreCase = true))
        assertTrue(es.contains("gracias", ignoreCase = true))

        val fr = BundledOfflineLexicon.translate("How are you", "en", "fr")
        assertTrue(fr.contains("Comment allez-vous", ignoreCase = true))

        val reverse = BundledOfflineLexicon.translate("Gracias", "es", "en")
        assertTrue(reverse.contains("thanks", ignoreCase = true) || reverse.contains("thank you", ignoreCase = true))

        val crossLang = BundledOfflineLexicon.translate("Hola", "es", "ja")
        assertTrue(crossLang.contains("こんにちは"))
    }

    @Test
    fun languageCatalog_coversAll59Languages() {
        assertEquals(59, LanguageCatalog.allLanguages.size)
    }

    @Test
    fun suggestionEngine_providesPrefixAndAutocorrect() {
        val engine = SuggestionEngine(SuggestionRepository(fakeDao))
        val res = engine.suggestForComposing("hel")
        assertTrue(res.suggestions.any { it.startsWith("hel") })

        val typo = engine.suggestForComposing("helo", autocorrectEnabled = true)
        assertTrue(typo.suggestions.contains("hello") || typo.autocorrectCandidate == "hello")
    }

    @Test
    fun gestureDecoder_decodesGlidePathForHello() {
        val engine = SuggestionEngine(SuggestionRepository(fakeDao))
        val decoder = GestureDecoder(engine)

        // Trace h -> e -> l -> l -> o using normalized key centers
        val h = SuggestionEngine.KEY_COORDS['h']!!
        val e = SuggestionEngine.KEY_COORDS['e']!!
        val l = SuggestionEngine.KEY_COORDS['l']!!
        val o = SuggestionEngine.KEY_COORDS['o']!!

        val stroke = listOf(
            h,
            ((h.first + e.first) / 2f) to ((h.second + e.second) / 2f),
            e,
            ((e.first + l.first) / 2f) to ((e.second + l.second) / 2f),
            l,
            ((l.first + o.first) / 2f) to ((l.second + o.second) / 2f),
            o,
        )

        val candidates = decoder.decodeGesture(stroke, maxCandidates = 3)
        assertTrue("Expected 'hello' in $candidates", candidates.contains("hello"))
    }

    @Test
    fun languageDetection_identifiesScriptsAndPhrasesOffline() {
        val ja = com.example.engine.OfflineTranslationEngine.detectLanguageOfflineFallback("おはようございます")
        assertEquals("ja", ja.languageCode)

        val es = com.example.engine.OfflineTranslationEngine.detectLanguageOfflineFallback("Buenos días, gracias")
        assertEquals("es", es.languageCode)

        val fr = com.example.engine.OfflineTranslationEngine.detectLanguageOfflineFallback("Bonjour, merci beaucoup")
        assertEquals("fr", fr.languageCode)
    }

    @Test
    fun materialThemes_onlyContainMaterialPalettes() {
        val presets = com.example.ui.theme.KeyboardThemePreset.entries
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.all { it.displayName.contains("Material", ignoreCase = true) })
    }
}
