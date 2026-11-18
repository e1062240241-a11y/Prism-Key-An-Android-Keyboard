package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PrismKeysApp
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.LearnedWordEntity
import com.example.data.local.StickerEntity
import com.example.data.local.StickerPackEntity
import com.example.data.preferences.KeyboardSettings
import com.example.data.preferences.OneHandedMode
import com.example.engine.TranslationResult
import com.example.engine.VoiceTypingState
import com.example.ui.keyboard.KeyboardController
import com.example.ui.keyboard.KeyboardTarget
import com.example.ui.theme.KeyboardKeyShape
import com.example.ui.theme.KeyboardThemePreset
import com.example.ui.theme.TrailColorPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImeSystemStatus(
    val isEnabledInSettings: Boolean = false,
    val isCurrentDefaultIme: Boolean = false,
    val activeDefaultImeLabel: String? = null,
)

data class PlaygroundState(
    val committedBeforeCursor: String = "Say hello! Tap keys or swipe across letters below. ",
    val composingText: String = "",
    val committedAfterCursor: String = "",
    val sentStickers: List<StickerEntity> = emptyList(),
    val statusNotice: String? = null,
) {
    val fullText: String get() = committedBeforeCursor + composingText + committedAfterCursor
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrismKeysApp

    val preferencesRepository = app.preferencesRepository
    val clipboardRepository = app.clipboardRepository
    val stickerRepository = app.stickerRepository
    val suggestionRepository = app.suggestionRepository
    val translationEngine = app.translationEngine

    val settings: StateFlow<KeyboardSettings> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, KeyboardSettings())

    val clipboardItems: StateFlow<List<ClipboardItemEntity>> = clipboardRepository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stickerPacks: StateFlow<List<StickerPackEntity>> = stickerRepository.packs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStickers: StateFlow<List<StickerEntity>> = stickerRepository.allStickers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedWords: StateFlow<List<LearnedWordEntity>> = suggestionRepository.learnedWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedModels: StateFlow<Set<String>> = translationEngine.downloadedModels
    val downloadingModels: StateFlow<Set<String>> = translationEngine.downloadingModels
    val translationStatusMessage: StateFlow<String?> = translationEngine.lastStatusMessage
    val isMlKitReady: StateFlow<Boolean> = translationEngine.isMlKitReady

    private val _playground = MutableStateFlow(PlaygroundState())
    val playground: StateFlow<PlaygroundState> = _playground.asStateFlow()

    private val _imeStatus = MutableStateFlow(ImeSystemStatus())
    val imeStatus: StateFlow<ImeSystemStatus> = _imeStatus.asStateFlow()

    private val _workbenchResult = MutableStateFlow<TranslationResult?>(null)
    val workbenchResult: StateFlow<TranslationResult?> = _workbenchResult.asStateFlow()

    private val _micPermissionRequestedEvent = MutableStateFlow(0)
    val micPermissionRequestedEvent: StateFlow<Int> = _micPermissionRequestedEvent.asStateFlow()

    private val playgroundTarget = object : KeyboardTarget {
        override fun commitText(text: String) {
            _playground.update { state ->
                state.copy(
                    committedBeforeCursor = state.committedBeforeCursor + text,
                    composingText = "",
                )
            }
        }

        override fun setComposingText(text: String) {
            _playground.update { state ->
                state.copy(composingText = text)
            }
        }

        override fun finishComposingText() {
            _playground.update { state ->
                state.copy(
                    committedBeforeCursor = state.committedBeforeCursor + state.composingText,
                    composingText = "",
                )
            }
        }

        override fun deleteBeforeCursor(count: Int) {
            _playground.update { state ->
                if (state.composingText.isNotEmpty()) {
                    state.copy(composingText = state.composingText.dropLast(count.coerceAtMost(state.composingText.length)))
                } else {
                    val drop = count.coerceAtMost(state.committedBeforeCursor.length)
                    state.copy(committedBeforeCursor = state.committedBeforeCursor.dropLast(drop))
                }
            }
        }

        override fun moveCursor(delta: Int) {
            _playground.update { state ->
                val full = state.committedBeforeCursor + state.composingText + state.committedAfterCursor
                val currentPos = state.committedBeforeCursor.length + state.composingText.length
                val nextPos = (currentPos + delta).coerceIn(0, full.length)
                state.copy(
                    committedBeforeCursor = full.substring(0, nextPos),
                    composingText = "",
                    committedAfterCursor = full.substring(nextPos),
                )
            }
        }

        override fun sendActionOrNewline() {
            commitText("\n")
        }

        override fun getTextBeforeCursor(maxChars: Int): String {
            return _playground.value.committedBeforeCursor.takeLast(maxChars)
        }

        override fun onStickerSelected(sticker: StickerEntity) {
            _playground.update { state ->
                state.copy(
                    sentStickers = (listOf(sticker) + state.sentStickers).take(6),
                    statusNotice = "Sent sticker \"${sticker.label}\"!",
                )
            }
        }

        override fun switchToNextIme() {
            val imm = getApplication<Application>().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }

        override fun requestMicPermission() {
            _micPermissionRequestedEvent.update { it + 1 }
        }
    }

    val keyboardController = KeyboardController(
        context = application,
        scope = viewModelScope,
        preferencesRepository = preferencesRepository,
        clipboardRepository = clipboardRepository,
        stickerRepository = stickerRepository,
        suggestionEngine = app.suggestionEngine,
        gestureDecoder = app.gestureDecoder,
        translationEngine = translationEngine,
        target = playgroundTarget,
    )

    val voiceState: StateFlow<VoiceTypingState> = keyboardController.voiceState

    private val systemClipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (settings.value.clipboardAutoCapture) {
            viewModelScope.launch {
                clipboardRepository.captureSystemClipboardIfPresent()
            }
        }
    }

    init {
        refreshImeStatus()
        systemClipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    fun refreshImeStatus() {
        val context = getApplication<Application>()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val pkg = context.packageName
        val enabled = imm?.enabledInputMethodList?.any { it.packageName == pkg } == true
        val defaultId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        }.getOrNull()
        val currentDefault = defaultId?.startsWith("$pkg/") == true
        val activeLabel = defaultId?.let { id ->
            imm?.inputMethodList?.firstOrNull { it.id == id }
                ?.loadLabel(context.packageManager)
                ?.toString()
        }
        _imeStatus.value = ImeSystemStatus(
            isEnabledInSettings = enabled,
            isCurrentDefaultIme = currentDefault,
            activeDefaultImeLabel = activeLabel,
        )
        if (settings.value.clipboardAutoCapture) {
            viewModelScope.launch {
                clipboardRepository.captureSystemClipboardIfPresent()
            }
        }
    }

    fun clearPlayground() {
        _playground.value = PlaygroundState(committedBeforeCursor = "", sentStickers = emptyList())
        keyboardController.resetComposingForNewField()
    }

    fun copyPlaygroundToClipboard() {
        val text = _playground.value.fullText.trim()
        if (text.isEmpty()) return
        val context = getApplication<Application>()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("PrismKeys", text))
        viewModelScope.launch {
            clipboardRepository.saveClip(text)
            _playground.update { it.copy(statusNotice = "Copied & saved to your Clipboard!") }
        }
    }

    fun dismissPlaygroundNotice() {
        _playground.update { it.copy(statusNotice = null) }
    }

    // Translation Hub
    fun translateWorkbench(text: String) {
        viewModelScope.launch {
            val curr = settings.value
            _workbenchResult.value = translationEngine.translate(
                text = text,
                sourceCode = curr.sourceLangCode,
                targetCode = curr.targetLangCode,
            )
        }
    }

    fun setLanguages(sourceCode: String, targetCode: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguages(sourceCode, targetCode)
        }
    }

    fun swapLanguages() {
        val curr = settings.value
        val newTarget = if (curr.sourceLangCode == com.example.engine.LanguageCatalog.AUTO_DETECT_CODE) {
            _workbenchResult.value?.detectedSourceCode ?: "en"
        } else {
            curr.sourceLangCode
        }
        setLanguages(curr.targetLangCode, newTarget)
    }

    fun downloadLanguagePack(code: String) {
        viewModelScope.launch {
            translationEngine.downloadLanguagePack(code, settings.value.strictOfflineMode)
        }
    }

    fun deleteLanguagePack(code: String) {
        viewModelScope.launch {
            translationEngine.deleteLanguagePack(code)
        }
    }

    fun dismissTranslationStatus() {
        translationEngine.clearStatusMessage()
    }

    // Clipboard Vault
    fun addClipboardSnippet(text: String, pinned: Boolean) {
        viewModelScope.launch { clipboardRepository.saveClip(text, pinned) }
    }

    fun toggleClipboardPin(item: ClipboardItemEntity) {
        viewModelScope.launch { clipboardRepository.togglePin(item) }
    }

    fun copyClipboardItem(item: ClipboardItemEntity) {
        viewModelScope.launch { clipboardRepository.markUsedAndCopyToSystem(item) }
    }

    fun deleteClipboardItem(id: Long) {
        viewModelScope.launch { clipboardRepository.deleteClip(id) }
    }

    fun clearUnpinnedClipboard() {
        viewModelScope.launch { clipboardRepository.clearUnpinned() }
    }

    // Custom Sticker Studio
    fun createCustomStickerPack(name: String, description: String) {
        viewModelScope.launch { stickerRepository.createCustomPack(name, description) }
    }

    fun importSticker(packId: Long, uri: Uri, label: String) {
        viewModelScope.launch { stickerRepository.importStickerFromUri(packId, uri, label) }
    }

    fun addEmojiSticker(packId: Long, emoji: String, label: String) {
        viewModelScope.launch { stickerRepository.addEmojiSticker(packId, emoji, label) }
    }

    fun deleteCustomPack(packId: Long) {
        viewModelScope.launch { stickerRepository.deleteCustomPack(packId) }
    }

    fun deleteSticker(sticker: StickerEntity) {
        viewModelScope.launch { stickerRepository.deleteSticker(sticker) }
    }

    // Personal Dictionary
    fun addPersonalWord(word: String) {
        viewModelScope.launch {
            suggestionRepository.learnWord(word, userAdded = true)
            app.suggestionEngine.refreshLearnedWords()
        }
    }

    fun removePersonalWord(word: String) {
        viewModelScope.launch {
            suggestionRepository.removeWord(word)
            app.suggestionEngine.refreshLearnedWords()
        }
    }

    fun clearPersonalDictionary() {
        viewModelScope.launch {
            suggestionRepository.clearLearnedWords()
            app.suggestionEngine.refreshLearnedWords()
        }
    }

    // Preferences
    fun setThemePreset(preset: KeyboardThemePreset) = viewModelScope.launch { preferencesRepository.setThemePreset(preset) }
    fun setKeyShape(shape: KeyboardKeyShape) = viewModelScope.launch { preferencesRepository.setKeyShape(shape) }
    fun setKeyCornerRadius(dp: Int) = viewModelScope.launch { preferencesRepository.setKeyCornerRadius(dp) }
    fun setKeyboardHeightScale(scale: Float) = viewModelScope.launch { preferencesRepository.setKeyboardHeightScale(scale) }
    fun setKeyboardWidthScale(scale: Float) = viewModelScope.launch { preferencesRepository.setKeyboardWidthScale(scale) }
    fun setKeyboardBottomOffsetDp(offsetDp: Int) = viewModelScope.launch { preferencesRepository.setKeyboardBottomOffsetDp(offsetDp) }
    fun resetKeyboardSize() = viewModelScope.launch { preferencesRepository.resetKeyboardSize() }
    fun setShowNumberRow(show: Boolean) = viewModelScope.launch { preferencesRepository.setShowNumberRow(show) }
    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setHapticFeedback(enabled) }
    fun setKeyPopups(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setKeyPopups(enabled) }
    fun setGestureTyping(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setGestureTyping(enabled) }
    fun setTrailColorPreset(preset: TrailColorPreset) = viewModelScope.launch { preferencesRepository.setTrailColorPreset(preset) }
    fun setAutocorrect(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setAutocorrect(enabled) }
    fun setSuggestions(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setSuggestions(enabled) }
    fun setAutoCapitalize(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setAutoCapitalize(enabled) }
    fun setOneHandedMode(mode: OneHandedMode) = viewModelScope.launch { preferencesRepository.setOneHandedMode(mode) }
    fun setStrictOfflineMode(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setStrictOfflineMode(enabled) }
    fun setClipboardAutoCapture(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setClipboardAutoCapture(enabled) }
    fun setAppDarkModeOverride(mode: String) = viewModelScope.launch { preferencesRepository.setAppDarkModeOverride(mode) }

    override fun onCleared() {
        systemClipboardManager?.removePrimaryClipChangedListener(clipListener)
        keyboardController.release()
        super.onCleared()
    }
}
