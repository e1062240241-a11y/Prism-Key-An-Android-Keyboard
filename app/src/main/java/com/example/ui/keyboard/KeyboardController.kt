package com.example.ui.keyboard

import android.content.Context
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.StickerEntity
import com.example.data.local.StickerPackEntity
import com.example.data.preferences.KeyboardPreferencesRepository
import com.example.data.preferences.KeyboardSettings
import com.example.data.preferences.OneHandedMode
import com.example.data.repository.ClipboardRepository
import com.example.data.repository.StickerRepository
import com.example.engine.GestureDecoder
import com.example.engine.LanguageCatalog
import com.example.engine.LocalVoiceTypingEngine
import com.example.engine.OfflineTranslationEngine
import com.example.engine.SuggestionEngine
import com.example.engine.TranslationResult
import com.example.engine.VoiceTypingState
import com.example.ui.theme.KeyboardThemePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KeyboardPanelMode {
    QWERTY,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    CLIPBOARD,
    TRANSLATE,
    STICKERS,
    VOICE,
    THEMES_QUICK,
    RESIZE,
}

enum class ShiftState {
    OFF,
    SINGLE,
    CAPS_LOCK,
}

interface KeyboardTarget {
    fun commitText(text: String)
    fun setComposingText(text: String)
    fun finishComposingText()
    fun deleteBeforeCursor(count: Int)
    fun moveCursor(delta: Int)
    fun sendActionOrNewline()
    fun getTextBeforeCursor(maxChars: Int): String
    fun onStickerSelected(sticker: StickerEntity)
    fun switchToNextIme()
    fun requestMicPermission()
}

data class KeyboardUiState(
    val panelMode: KeyboardPanelMode = KeyboardPanelMode.QWERTY,
    val shiftState: ShiftState = ShiftState.SINGLE,
    val composingWord: String = "",
    val suggestions: List<String> = listOf("I", "The", "Thanks"),
    val autocorrectCandidate: String? = null,
    val gesturePreviewWord: String? = null,
    val recentEmojis: List<String> = listOf("😊", "😂", "❤️", "👍", "🔥", "🎉", "☕", "🚀"),
    val translateInput: String = "",
    val translationResult: TranslationResult? = null,
    val isTranslating: Boolean = false,
    val autoTranslateTyping: Boolean = false,
    val clipboardSearch: String = "",
    val clipboardPinnedOnly: Boolean = false,
    val selectedStickerPackId: Long? = null,
    val showQuickToolsDrawer: Boolean = false,
)

class KeyboardController(
    context: Context,
    private val scope: CoroutineScope,
    private val preferencesRepository: KeyboardPreferencesRepository,
    private val clipboardRepository: ClipboardRepository,
    private val stickerRepository: StickerRepository,
    private val suggestionEngine: SuggestionEngine,
    private val gestureDecoder: GestureDecoder,
    private val translationEngine: OfflineTranslationEngine,
    private val target: KeyboardTarget,
) {
    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    val settings: StateFlow<KeyboardSettings> = preferencesRepository.settingsFlow
        .stateIn(scope, SharingStarted.Eagerly, KeyboardSettings())

    val clipboardItems: StateFlow<List<ClipboardItemEntity>> = combine(
        clipboardRepository.items,
        _uiState,
    ) { items, ui ->
        items.filter { item ->
            val pinMatch = !ui.clipboardPinnedOnly || item.isPinned
            val queryMatch = ui.clipboardSearch.isBlank() ||
                item.content.contains(ui.clipboardSearch, ignoreCase = true)
            pinMatch && queryMatch
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val stickerPacks: StateFlow<List<StickerPackEntity>> = stickerRepository.packs
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allStickers: StateFlow<List<StickerEntity>> = stickerRepository.allStickers
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val downloadedModels: StateFlow<Set<String>> = translationEngine.downloadedModels

    private val voiceEngine = LocalVoiceTypingEngine(context) { spokenText ->
        commitWordOrPhraseFromVoice(spokenText)
    }
    val voiceState: StateFlow<VoiceTypingState> = voiceEngine.state

    private var lastCommittedWord: String? = null
    private var lastAutocorrectUndo: Pair<String, String>? = null // rawTyped -> committedWithSpace
    private var translateJob: Job? = null

    fun setPanelMode(mode: KeyboardPanelMode) {
        if (mode != KeyboardPanelMode.VOICE && voiceState.value is VoiceTypingState.Listening) {
            voiceEngine.stopListening()
        }
        if (mode == KeyboardPanelMode.CLIPBOARD) {
            scope.launch { clipboardRepository.captureSystemClipboardIfPresent() }
        }
        if (mode == KeyboardPanelMode.TRANSLATE && _uiState.value.translateInput.isBlank()) {
            val recent = target.getTextBeforeCursor(80).trim()
            if (recent.isNotEmpty()) {
                updateTranslateInput(recent)
            }
        }
        _uiState.update { it.copy(panelMode = mode, showQuickToolsDrawer = false) }
    }

    fun toggleQuickToolsDrawer() {
        _uiState.update { it.copy(showQuickToolsDrawer = !it.showQuickToolsDrawer) }
    }

    fun toggleShift() {
        _uiState.update { state ->
            val next = when (state.shiftState) {
                ShiftState.OFF -> ShiftState.SINGLE
                ShiftState.SINGLE -> ShiftState.CAPS_LOCK
                ShiftState.CAPS_LOCK -> ShiftState.OFF
            }
            state.copy(shiftState = next)
        }
    }

    fun onKeyTyped(rawChar: String) {
        val state = _uiState.value
        val ch = if (state.panelMode == KeyboardPanelMode.QWERTY && state.shiftState != ShiftState.OFF) {
            rawChar.uppercase()
        } else {
            rawChar
        }

        if (state.panelMode == KeyboardPanelMode.TRANSLATE) {
            updateTranslateInput(state.translateInput + ch)
            return
        }

        val isWordChar = ch.length == 1 && (ch[0].isLetter() || ch == "'")
        if (isWordChar) {
            lastAutocorrectUndo = null
            val updated = state.composingWord + ch
            target.setComposingText(updated)
            val suggestionRes = suggestionEngine.suggestForComposing(
                composing = updated,
                previousWord = lastCommittedWord,
                autocorrectEnabled = settings.value.autocorrectEnabled,
            )
            val nextShift = if (state.shiftState == ShiftState.SINGLE) ShiftState.OFF else state.shiftState
            _uiState.update {
                it.copy(
                    composingWord = updated,
                    suggestions = suggestionRes.suggestions,
                    autocorrectCandidate = suggestionRes.autocorrectCandidate,
                    shiftState = nextShift,
                    gesturePreviewWord = null,
                )
            }
        } else {
            lastAutocorrectUndo = null
            commitPendingWordIfAny()
            target.commitText(ch)
            if (ch in listOf(".", "!", "?") && settings.value.autoCapitalize) {
                _uiState.update { it.copy(shiftState = ShiftState.SINGLE) }
            }
        }
    }

    fun onSpacePressed() {
        val state = _uiState.value
        if (state.panelMode == KeyboardPanelMode.TRANSLATE) {
            updateTranslateInput(state.translateInput + " ")
            return
        }

        if (state.composingWord.isNotEmpty()) {
            val prevWord = lastCommittedWord
            val rawTyped = state.composingWord
            val wasAutocorrected = settings.value.autocorrectEnabled &&
                state.autocorrectCandidate != null &&
                !state.autocorrectCandidate.equals(rawTyped, ignoreCase = false)
            val chosen = if (wasAutocorrected) state.autocorrectCandidate!! else rawTyped

            val committedText = "$chosen "
            target.commitText(committedText)
            lastCommittedWord = chosen
            lastAutocorrectUndo = if (wasAutocorrected) rawTyped to committedText else null
            suggestionEngine.learnWordAsync(
                word = chosen,
                previousWord = prevWord,
                rawTyped = if (wasAutocorrected) rawTyped else null,
            )
            val nextSuggestions = suggestionEngine.suggestNextWords(chosen)
            _uiState.update {
                it.copy(
                    composingWord = "",
                    suggestions = nextSuggestions,
                    autocorrectCandidate = null,
                    gesturePreviewWord = null,
                )
            }
        } else {
            lastAutocorrectUndo = null
            target.commitText(" ")
            val before = target.getTextBeforeCursor(40)
            if (before.endsWith(". ") || before.endsWith("! ") || before.endsWith("? ")) {
                if (settings.value.autoCapitalize) {
                    _uiState.update { it.copy(shiftState = ShiftState.SINGLE) }
                }
            }
        }
    }

    fun onBackspacePressed() {
        val state = _uiState.value
        if (state.panelMode == KeyboardPanelMode.TRANSLATE) {
            if (state.translateInput.isNotEmpty()) {
                updateTranslateInput(state.translateInput.dropLast(1))
            }
            return
        }

        if (state.composingWord.isNotEmpty()) {
            lastAutocorrectUndo = null
            val updated = state.composingWord.dropLast(1)
            if (updated.isEmpty()) {
                target.setComposingText("")
                target.finishComposingText()
                _uiState.update {
                    it.copy(
                        composingWord = "",
                        suggestions = suggestionEngine.suggestNextWords(lastCommittedWord),
                        autocorrectCandidate = null,
                    )
                }
            } else {
                target.setComposingText(updated)
                val res = suggestionEngine.suggestForComposing(
                    composing = updated,
                    previousWord = lastCommittedWord,
                    autocorrectEnabled = settings.value.autocorrectEnabled,
                )
                _uiState.update {
                    it.copy(
                        composingWord = updated,
                        suggestions = res.suggestions,
                        autocorrectCandidate = res.autocorrectCandidate,
                    )
                }
            }
        } else {
            val undoPair = lastAutocorrectUndo
            if (undoPair != null) {
                val (rawTyped, committedWithSpace) = undoPair
                val before = target.getTextBeforeCursor(committedWithSpace.length + 2)
                if (before.endsWith(committedWithSpace)) {
                    lastAutocorrectUndo = null
                    target.deleteBeforeCursor(committedWithSpace.length)
                    target.setComposingText(rawTyped)
                    suggestionEngine.recordUndoAutocorrectAsync(
                        correctedWord = committedWithSpace.trimEnd(),
                        originalTyped = rawTyped,
                    )
                    val res = suggestionEngine.suggestForComposing(
                        composing = rawTyped,
                        previousWord = lastCommittedWord,
                        autocorrectEnabled = false,
                    )
                    _uiState.update {
                        it.copy(
                            composingWord = rawTyped,
                            suggestions = (listOf(rawTyped) + res.suggestions).distinct().take(3),
                            autocorrectCandidate = null,
                        )
                    }
                    return
                }
            }
            lastAutocorrectUndo = null
            target.deleteBeforeCursor(1)
        }
    }

    fun onDeletePreviousWord() {
        val state = _uiState.value
        if (state.composingWord.isNotEmpty()) {
            target.setComposingText("")
            target.finishComposingText()
            _uiState.update {
                it.copy(
                    composingWord = "",
                    suggestions = suggestionEngine.suggestNextWords(lastCommittedWord),
                    autocorrectCandidate = null,
                )
            }
            return
        }
        val before = target.getTextBeforeCursor(48)
        if (before.isEmpty()) return
        var deleteCount = 0
        var i = before.length - 1
        while (i >= 0 && before[i].isWhitespace()) {
            deleteCount++
            i--
        }
        while (i >= 0 && !before[i].isWhitespace()) {
            deleteCount++
            i--
        }
        if (deleteCount > 0) {
            target.deleteBeforeCursor(deleteCount)
        }
    }

    fun onEnterPressed() {
        val state = _uiState.value
        if (state.panelMode == KeyboardPanelMode.TRANSLATE) {
            insertCurrentTranslation()
            return
        }
        commitPendingWordIfAny()
        target.sendActionOrNewline()
        if (settings.value.autoCapitalize) {
            _uiState.update { it.copy(shiftState = ShiftState.SINGLE) }
        }
    }

    fun onSuggestionTapped(word: String) {
        val state = _uiState.value
        val prevWord = lastCommittedWord
        val rawTyped = state.composingWord.takeIf { it.isNotEmpty() }
        lastAutocorrectUndo = null
        val formatted = if (state.shiftState != ShiftState.OFF && word.isNotEmpty()) {
            word.replaceFirstChar { it.uppercase() }
        } else {
            word
        }
        target.commitText("$formatted ")
        lastCommittedWord = formatted
        suggestionEngine.learnWordAsync(
            word = formatted,
            previousWord = prevWord,
            rawTyped = rawTyped,
        )
        val next = suggestionEngine.suggestNextWords(formatted)
        _uiState.update {
            it.copy(
                composingWord = "",
                suggestions = next,
                autocorrectCandidate = null,
                gesturePreviewWord = null,
                shiftState = if (it.shiftState == ShiftState.SINGLE) ShiftState.OFF else it.shiftState,
            )
        }
    }

    fun onGestureMove(normalizedPoints: List<Pair<Float, Float>>) {
        if (!settings.value.gestureTypingEnabled) return
        val candidates = gestureDecoder.decodeGesture(normalizedPoints, maxCandidates = 3)
        val best = candidates.firstOrNull()
        _uiState.update {
            it.copy(
                gesturePreviewWord = best,
                suggestions = if (candidates.isNotEmpty()) candidates else it.suggestions,
            )
        }
    }

    fun onGestureEnd(normalizedPoints: List<Pair<Float, Float>>) {
        if (!settings.value.gestureTypingEnabled) {
            _uiState.update { it.copy(gesturePreviewWord = null) }
            return
        }
        val candidates = gestureDecoder.decodeGesture(normalizedPoints, maxCandidates = 3)
        val best = candidates.firstOrNull()
        if (best != null) {
            val state = _uiState.value
            val formatted = if (state.shiftState != ShiftState.OFF) {
                best.replaceFirstChar { it.uppercase() }
            } else {
                best
            }
            if (state.panelMode == KeyboardPanelMode.TRANSLATE) {
                val sep = if (state.translateInput.isNotEmpty() && !state.translateInput.endsWith(" ")) " " else ""
                updateTranslateInput(state.translateInput + sep + formatted)
                _uiState.update { it.copy(gesturePreviewWord = null) }
            } else {
                commitPendingWordIfAny()
                val prevWord = lastCommittedWord
                lastAutocorrectUndo = null
                val prefix = target.getTextBeforeCursor(2)
                val leadingSpace = if (prefix.isNotEmpty() && !prefix.last().isWhitespace()) " " else ""
                target.commitText("$leadingSpace$formatted ")
                lastCommittedWord = formatted
                suggestionEngine.learnWordAsync(word = formatted, previousWord = prevWord)
                _uiState.update {
                    it.copy(
                        composingWord = "",
                        gesturePreviewWord = null,
                        suggestions = candidates,
                        autocorrectCandidate = null,
                        shiftState = if (it.shiftState == ShiftState.SINGLE) ShiftState.OFF else it.shiftState,
                    )
                }
            }
        } else {
            _uiState.update { it.copy(gesturePreviewWord = null) }
        }
    }

    fun onEmojiTapped(emoji: String) {
        commitPendingWordIfAny()
        target.commitText(emoji)
        _uiState.update { state ->
            val updated = (listOf(emoji) + state.recentEmojis).distinct().take(12)
            state.copy(recentEmojis = updated)
        }
    }

    fun onMoveCursor(delta: Int) {
        commitPendingWordIfAny()
        target.moveCursor(delta)
    }

    // Clipboard actions
    fun updateClipboardSearch(query: String) {
        _uiState.update { it.copy(clipboardSearch = query) }
    }

    fun toggleClipboardPinnedFilter() {
        _uiState.update { it.copy(clipboardPinnedOnly = !it.clipboardPinnedOnly) }
    }

    fun pasteClipboardItem(item: ClipboardItemEntity) {
        commitPendingWordIfAny()
        target.commitText(item.content)
        scope.launch {
            clipboardRepository.markUsedAndCopyToSystem(item)
        }
    }

    fun toggleClipboardPin(item: ClipboardItemEntity) {
        scope.launch { clipboardRepository.togglePin(item) }
    }

    fun deleteClipboardItem(item: ClipboardItemEntity) {
        scope.launch { clipboardRepository.deleteClip(item.id) }
    }

    fun clearUnpinnedClipboard() {
        scope.launch { clipboardRepository.clearUnpinned() }
    }

    fun saveTextToClipboard(text: String) {
        if (text.isBlank()) return
        scope.launch { clipboardRepository.saveClip(text, pinned = false) }
    }

    // Offline Translation actions
    fun updateTranslateInput(text: String) {
        _uiState.update { it.copy(translateInput = text) }
        translateJob?.cancel()
        if (text.isBlank()) {
            _uiState.update { it.copy(translationResult = null, isTranslating = false) }
            return
        }
        translateJob = scope.launch {
            delay(140)
            _uiState.update { it.copy(isTranslating = true) }
            val currSettings = settings.value
            val result = translationEngine.translate(
                text = text,
                sourceCode = currSettings.sourceLangCode,
                targetCode = currSettings.targetLangCode,
            )
            _uiState.update { it.copy(translationResult = result, isTranslating = false) }
        }
    }

    fun setTranslationLanguages(sourceCode: String, targetCode: String) {
        scope.launch {
            preferencesRepository.setLanguages(sourceCode, targetCode)
            val currentInput = _uiState.value.translateInput
            if (currentInput.isNotBlank()) {
                val res = translationEngine.translate(currentInput, sourceCode, targetCode)
                _uiState.update { it.copy(translationResult = res) }
            }
        }
    }

    fun swapTranslationLanguages() {
        val curr = settings.value
        val result = _uiState.value.translationResult
        val oldResultText = result?.translatedText
        val newTarget = if (curr.sourceLangCode == LanguageCatalog.AUTO_DETECT_CODE) {
            result?.detectedSourceCode ?: "en"
        } else {
            curr.sourceLangCode
        }
        scope.launch {
            preferencesRepository.setLanguages(curr.targetLangCode, newTarget)
            if (!oldResultText.isNullOrBlank()) {
                updateTranslateInput(oldResultText)
            }
        }
    }

    fun insertCurrentTranslation() {
        val translated = _uiState.value.translationResult?.translatedText
        val textToInsert = if (!translated.isNullOrBlank()) translated else _uiState.value.translateInput
        if (textToInsert.isNotBlank()) {
            commitPendingWordIfAny()
            target.commitText(textToInsert)
            _uiState.update {
                it.copy(
                    translateInput = "",
                    translationResult = null,
                    panelMode = KeyboardPanelMode.QWERTY,
                )
            }
        }
    }

    // Voice typing actions
    fun startVoiceTyping() {
        if (!voiceEngine.hasAudioPermission()) {
            target.requestMicPermission()
        }
        val langTag = if (settings.value.sourceLangCode == LanguageCatalog.AUTO_DETECT_CODE) {
            "en"
        } else {
            LanguageCatalog.findByCode(settings.value.sourceLangCode).code
        }
        voiceEngine.startListening(langTag)
    }

    fun stopVoiceTyping() {
        voiceEngine.stopListening()
    }

    private fun commitWordOrPhraseFromVoice(spokenText: String) {
        commitPendingWordIfAny()
        target.commitText(spokenText)
        val words = spokenText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        words.forEach { w ->
            val prev = lastCommittedWord
            lastCommittedWord = w
            suggestionEngine.learnWordAsync(word = w, previousWord = prev)
        }
    }

    // Stickers
    fun selectStickerPack(packId: Long) {
        _uiState.update { it.copy(selectedStickerPackId = packId) }
    }

    fun onStickerTapped(sticker: StickerEntity) {
        commitPendingWordIfAny()
        target.onStickerSelected(sticker)
    }

    // Quick settings / Theme
    fun setQuickTheme(preset: KeyboardThemePreset) {
        scope.launch { preferencesRepository.setThemePreset(preset) }
    }

    fun toggleNumberRow() {
        scope.launch { preferencesRepository.setShowNumberRow(!settings.value.showNumberRow) }
    }

    fun cycleOneHandedMode() {
        val next = when (settings.value.oneHandedMode) {
            OneHandedMode.NONE -> OneHandedMode.RIGHT
            OneHandedMode.RIGHT -> OneHandedMode.LEFT
            OneHandedMode.LEFT -> OneHandedMode.NONE
        }
        scope.launch { preferencesRepository.setOneHandedMode(next) }
    }

    fun setKeyboardHeightScale(scale: Float) {
        scope.launch { preferencesRepository.setKeyboardHeightScale(scale) }
    }

    fun setKeyboardWidthScale(scale: Float) {
        scope.launch { preferencesRepository.setKeyboardWidthScale(scale) }
    }

    fun setKeyboardBottomOffsetDp(offsetDp: Int) {
        scope.launch { preferencesRepository.setKeyboardBottomOffsetDp(offsetDp) }
    }

    fun resetKeyboardSize() {
        scope.launch { preferencesRepository.resetKeyboardSize() }
    }

    fun handleBackToQwerty(): Boolean {
        val mode = _uiState.value.panelMode
        return if (mode != KeyboardPanelMode.QWERTY) {
            setPanelMode(KeyboardPanelMode.QWERTY)
            true
        } else {
            false
        }
    }

    fun switchToNextIme() {
        target.switchToNextIme()
    }

    fun resetComposingForNewField() {
        lastAutocorrectUndo = null
        lastCommittedWord = null
        _uiState.update {
            it.copy(
                composingWord = "",
                autocorrectCandidate = null,
                gesturePreviewWord = null,
                shiftState = if (settings.value.autoCapitalize) ShiftState.SINGLE else ShiftState.OFF,
            )
        }
    }

    fun release() {
        voiceEngine.release()
    }

    private fun commitPendingWordIfAny() {
        val word = _uiState.value.composingWord
        if (word.isNotEmpty()) {
            val prevWord = lastCommittedWord
            target.finishComposingText()
            lastCommittedWord = word
            suggestionEngine.learnWordAsync(word = word, previousWord = prevWord)
            _uiState.update {
                it.copy(
                    composingWord = "",
                    autocorrectCandidate = null,
                    suggestions = suggestionEngine.suggestNextWords(word),
                )
            }
        }
    }
}
