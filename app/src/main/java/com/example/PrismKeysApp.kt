package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.KeyboardPreferencesRepository
import com.example.data.repository.ClipboardRepository
import com.example.data.repository.StickerRepository
import com.example.data.repository.SuggestionRepository
import com.example.engine.GestureDecoder
import com.example.engine.OfflineTranslationEngine
import com.example.engine.SuggestionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrismKeysApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var preferencesRepository: KeyboardPreferencesRepository
        private set
    lateinit var clipboardRepository: ClipboardRepository
        private set
    lateinit var stickerRepository: StickerRepository
        private set
    lateinit var suggestionRepository: SuggestionRepository
        private set
    lateinit var suggestionEngine: SuggestionEngine
        private set
    lateinit var gestureDecoder: GestureDecoder
        private set
    lateinit var translationEngine: OfflineTranslationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferencesRepository = KeyboardPreferencesRepository(this)
        clipboardRepository = ClipboardRepository(this, database.clipboardDao())
        stickerRepository = StickerRepository(this, database.stickerDao())
        suggestionRepository = SuggestionRepository(
            learnedWordDao = database.learnedWordDao(),
            wordTransitionDao = database.wordTransitionDao(),
            typoPatternDao = database.typoPatternDao(),
        )
        suggestionEngine = SuggestionEngine(suggestionRepository)
        gestureDecoder = GestureDecoder(suggestionEngine)
        translationEngine = OfflineTranslationEngine(this)

        appScope.launch {
            clipboardRepository.ensureSeeded()
            stickerRepository.ensureSeeded()
            suggestionEngine.refreshLearnedWords()
            translationEngine.initialize()
        }
    }
}
