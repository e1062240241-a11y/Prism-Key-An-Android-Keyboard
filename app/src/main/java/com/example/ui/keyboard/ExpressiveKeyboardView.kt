package com.example.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.KeyboardSettings
import com.example.data.preferences.OneHandedMode
import com.example.data.repository.StickerRepository
import com.example.engine.LanguageCatalog
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KeyboardKeyShape
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.TrailColorPreset
import com.example.ui.theme.resolveKeyboardPalette
import kotlinx.coroutines.delay

@Composable
fun ExpressiveKeyboardView(
    controller: KeyboardController,
    stickerRepository: StickerRepository,
    modifier: Modifier = Modifier,
    showImeSwitcherKey: Boolean = true,
) {
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    val settings by controller.settings.collectAsStateWithLifecycle()
    val clipboardItems by controller.clipboardItems.collectAsStateWithLifecycle()
    val stickerPacks by controller.stickerPacks.collectAsStateWithLifecycle()
    val allStickers by controller.allStickers.collectAsStateWithLifecycle()
    val downloadedModels by controller.downloadedModels.collectAsStateWithLifecycle()
    val voiceState by controller.voiceState.collectAsStateWithLifecycle()

    val palette = resolveKeyboardPalette(settings.themePreset, MaterialTheme.colorScheme)
    val haptics = LocalHapticFeedback.current

    fun performKeyHaptic() {
        if (settings.hapticFeedback) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val effectiveWidthFraction = when (settings.oneHandedMode) {
        OneHandedMode.NONE -> settings.keyboardWidthScale.coerceIn(0.82f, 1.0f)
        OneHandedMode.LEFT -> minOf(0.86f, settings.keyboardWidthScale)
        OneHandedMode.RIGHT -> minOf(0.86f, settings.keyboardWidthScale)
    }

    val outerAlignment = when (settings.oneHandedMode) {
        OneHandedMode.LEFT -> Alignment.Start
        OneHandedMode.RIGHT -> Alignment.End
        OneHandedMode.NONE -> Alignment.CenterHorizontally
    }

    val spacebarLanguageLabel = remember(settings.sourceLangCode) {
        if (settings.sourceLangCode == LanguageCatalog.AUTO_DETECT_CODE) {
            "English · Auto-Detect"
        } else {
            LanguageCatalog.findByCode(settings.sourceLangCode).englishName
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.boardBackground)
            .padding(bottom = settings.keyboardBottomOffsetDp.dp),
        horizontalAlignment = outerAlignment,
    ) {
        // Floating gesture word preview badge
        AnimatedVisibility(visible = !uiState.gesturePreviewWord.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.actionKeyBackground,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = uiState.gesturePreviewWord.orEmpty(),
                        color = palette.actionKeyText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            }
        }

        // Gboard Top Suggestion & Utility Strip
        GboardToolbarAndSuggestions(
            uiState = uiState,
            settings = settings,
            palette = palette,
            onSuggestionTapped = {
                performKeyHaptic()
                controller.onSuggestionTapped(it)
            },
            onSelectMode = { mode ->
                performKeyHaptic()
                if (mode == KeyboardPanelMode.VOICE) {
                    controller.setPanelMode(KeyboardPanelMode.VOICE)
                    controller.startVoiceTyping()
                } else {
                    controller.setPanelMode(
                        if (uiState.panelMode == mode) KeyboardPanelMode.QWERTY else mode,
                    )
                }
            },
            onToggleQuickTools = {
                performKeyHaptic()
                controller.toggleQuickToolsDrawer()
            },
        )

        // Gboard Quick Tools Grid Strip (Resize, Themes, 1-Hand, Numbers)
        AnimatedVisibility(visible = uiState.showQuickToolsDrawer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.boardBackgroundAccent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickToolChip("Resize", Icons.Filled.Height, palette, "ime_tool_resize") {
                    controller.setPanelMode(KeyboardPanelMode.RESIZE)
                }
                QuickToolChip("Themes", Icons.Filled.Palette, palette, "ime_tool_themes") {
                    controller.setPanelMode(KeyboardPanelMode.THEMES_QUICK)
                }
                QuickToolChip(
                    if (settings.oneHandedMode == OneHandedMode.NONE) "1-Hand" else settings.oneHandedMode.label,
                    Icons.Filled.SwapHoriz,
                    palette,
                    "ime_tool_onehanded",
                ) {
                    controller.cycleOneHandedMode()
                }
                QuickToolChip("Numbers", Icons.Filled.Dialpad, palette, "ime_tool_numbers") {
                    controller.toggleNumberRow()
                }
            }
        }

        // Inline Live Translate Bar stays above QWERTY keys when in TRANSLATE mode
        if (uiState.panelMode == KeyboardPanelMode.TRANSLATE) {
            InlineTranslateBar(
                settings = settings,
                translateInput = uiState.translateInput,
                translationResult = uiState.translationResult,
                isTranslating = uiState.isTranslating,
                downloadedModels = downloadedModels,
                palette = palette,
                onSwapLanguages = { controller.swapTranslationLanguages() },
                onSelectLanguages = { s, t -> controller.setTranslationLanguages(s, t) },
                onClearInput = { controller.updateTranslateInput("") },
                onInsertTranslation = { controller.insertCurrentTranslation() },
                onClose = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
            )
        }

        // Body Panel (scales with width & height)
        Box(modifier = Modifier.fillMaxWidth(effectiveWidthFraction)) {
            when (uiState.panelMode) {
                KeyboardPanelMode.CLIPBOARD -> ClipboardPanel(
                    items = clipboardItems,
                    searchQuery = uiState.clipboardSearch,
                    pinnedOnly = uiState.clipboardPinnedOnly,
                    heightScale = settings.keyboardHeightScale,
                    palette = palette,
                    onSearchChange = { controller.updateClipboardSearch(it) },
                    onTogglePinnedFilter = { controller.toggleClipboardPinnedFilter() },
                    onPasteItem = {
                        performKeyHaptic()
                        controller.pasteClipboardItem(it)
                    },
                    onTogglePin = { controller.toggleClipboardPin(it) },
                    onDeleteItem = { controller.deleteClipboardItem(it) },
                    onClearUnpinned = { controller.clearUnpinnedClipboard() },
                    onClose = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
                )

                KeyboardPanelMode.EMOJI -> EmojiPanel(
                    recentEmojis = uiState.recentEmojis,
                    heightScale = settings.keyboardHeightScale,
                    palette = palette,
                    onEmojiTapped = {
                        performKeyHaptic()
                        controller.onEmojiTapped(it)
                    },
                    onBackspace = {
                        performKeyHaptic()
                        controller.onBackspacePressed()
                    },
                    onBackToKeys = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
                )

                KeyboardPanelMode.STICKERS -> StickersPanel(
                    packs = stickerPacks,
                    stickers = allStickers,
                    selectedPackId = uiState.selectedStickerPackId,
                    heightScale = settings.keyboardHeightScale,
                    stickerRepository = stickerRepository,
                    palette = palette,
                    onSelectPack = { controller.selectStickerPack(it) },
                    onStickerTapped = {
                        performKeyHaptic()
                        controller.onStickerTapped(it)
                    },
                    onClose = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
                )

                KeyboardPanelMode.VOICE -> VoiceTypingPanel(
                    voiceState = voiceState,
                    heightScale = settings.keyboardHeightScale,
                    palette = palette,
                    onStartListening = { controller.startVoiceTyping() },
                    onStopListening = { controller.stopVoiceTyping() },
                    onClose = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
                )

                KeyboardPanelMode.THEMES_QUICK -> QuickThemesPanel(
                    settings = settings,
                    palette = palette,
                    onSelectTheme = { controller.setQuickTheme(it) },
                    onToggleNumberRow = { controller.toggleNumberRow() },
                    onCycleOneHanded = { controller.cycleOneHandedMode() },
                    onOpenResize = { controller.setPanelMode(KeyboardPanelMode.RESIZE) },
                    onClose = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
                )

                KeyboardPanelMode.SYMBOLS_1, KeyboardPanelMode.SYMBOLS_2 -> SymbolsKeyboardBody(
                    isExtended = uiState.panelMode == KeyboardPanelMode.SYMBOLS_2,
                    settings = settings,
                    palette = palette,
                    showImeSwitcherKey = showImeSwitcherKey,
                    spacebarLabel = spacebarLanguageLabel,
                    onKey = {
                        performKeyHaptic()
                        controller.onKeyTyped(it)
                    },
                    onBackspace = {
                        performKeyHaptic()
                        controller.onBackspacePressed()
                    },
                    onSpace = {
                        performKeyHaptic()
                        controller.onSpacePressed()
                    },
                    onMoveCursor = { controller.onMoveCursor(it) },
                    onEnter = {
                        performKeyHaptic()
                        controller.onEnterPressed()
                    },
                    onSwitchIme = { controller.switchToNextIme() },
                    onToggleExtended = {
                        performKeyHaptic()
                        controller.setPanelMode(
                            if (uiState.panelMode == KeyboardPanelMode.SYMBOLS_2) {
                                KeyboardPanelMode.SYMBOLS_1
                            } else {
                                KeyboardPanelMode.SYMBOLS_2
                            },
                        )
                    },
                    onBackToAbc = {
                        performKeyHaptic()
                        controller.setPanelMode(KeyboardPanelMode.QWERTY)
                    },
                )

                KeyboardPanelMode.QWERTY, KeyboardPanelMode.TRANSLATE, KeyboardPanelMode.RESIZE -> {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        QwertyKeyboardBody(
                            shiftState = uiState.shiftState,
                            settings = settings,
                            palette = palette,
                            showImeSwitcherKey = showImeSwitcherKey,
                            isTranslateMode = uiState.panelMode == KeyboardPanelMode.TRANSLATE,
                            spacebarLabel = spacebarLanguageLabel,
                            onKey = {
                                performKeyHaptic()
                                controller.onKeyTyped(it)
                            },
                            onShift = {
                                performKeyHaptic()
                                controller.toggleShift()
                            },
                            onBackspace = {
                                performKeyHaptic()
                                controller.onBackspacePressed()
                            },
                            onDeleteWord = {
                                performKeyHaptic()
                                controller.onDeletePreviousWord()
                            },
                            onSpace = {
                                performKeyHaptic()
                                controller.onSpacePressed()
                            },
                            onMoveCursor = { controller.onMoveCursor(it) },
                            onEnter = {
                                performKeyHaptic()
                                controller.onEnterPressed()
                            },
                            onSymbols = {
                                performKeyHaptic()
                                controller.setPanelMode(KeyboardPanelMode.SYMBOLS_1)
                            },
                            onEmoji = {
                                performKeyHaptic()
                                controller.setPanelMode(KeyboardPanelMode.EMOJI)
                            },
                            onSwitchIme = { controller.switchToNextIme() },
                            onGestureMove = { controller.onGestureMove(it) },
                            onGestureEnd = { controller.onGestureEnd(it) },
                        )

                        // Interactive Gboard-style live Resize Frame overlaid on QWERTY
                        if (uiState.panelMode == KeyboardPanelMode.RESIZE) {
                            LiveResizeOverlay(
                                settings = settings,
                                palette = palette,
                                onHeightScaleChange = { controller.setKeyboardHeightScale(it) },
                                onWidthScaleChange = { controller.setKeyboardWidthScale(it) },
                                onBottomOffsetChange = { controller.setKeyboardBottomOffsetDp(it) },
                                onReset = { controller.resetKeyboardSize() },
                                onDone = { controller.setPanelMode(KeyboardPanelMode.QWERTY) },
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GboardToolbarAndSuggestions(
    uiState: KeyboardUiState,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    onSuggestionTapped: (String) -> Unit,
    onSelectMode: (KeyboardPanelMode) -> Unit,
    onToggleQuickTools: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(palette.toolbarBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Leftmost Gboard 4-dot Apps/Tools circle button
        Surface(
            shape = CircleShape,
            color = if (uiState.showQuickToolsDrawer) palette.accentContainer else palette.specialKeyBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleQuickTools)
                .testTag("ime_btn_quick_tools"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = "Keyboard tools",
                    tint = if (uiState.showQuickToolsDrawer) palette.onAccentContainer else palette.specialKeyText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val showSuggestions = settings.suggestionsEnabled &&
            (uiState.composingWord.isNotEmpty() || uiState.gesturePreviewWord != null) &&
            uiState.panelMode == KeyboardPanelMode.QWERTY

        if (showSuggestions) {
            val candidates = uiState.suggestions.take(3)
            candidates.forEachIndexed { idx, word ->
                val isBest = idx == 0 || word == uiState.autocorrectCandidate
                if (idx > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(palette.divider),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBest) palette.accentContainer.copy(alpha = 0.65f) else Color.Transparent)
                        .clickable { onSuggestionTapped(word) }
                        .testTag("ime_suggestion_$idx"),
                ) {
                    Text(
                        text = word,
                        color = if (isBest) palette.onAccentContainer else palette.keyText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(2.dp))
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.StickyNote2,
                label = "Stickers",
                active = uiState.panelMode == KeyboardPanelMode.STICKERS,
                palette = palette,
                testTag = "ime_btn_stickers",
                onClick = { onSelectMode(KeyboardPanelMode.STICKERS) },
            )
            ToolbarIconButton(
                icon = Icons.Filled.ContentPaste,
                label = "Clipboard",
                active = uiState.panelMode == KeyboardPanelMode.CLIPBOARD,
                palette = palette,
                testTag = "ime_btn_clipboard",
                onClick = { onSelectMode(KeyboardPanelMode.CLIPBOARD) },
            )
            ToolbarIconButton(
                icon = Icons.Filled.Translate,
                label = "Translate",
                active = uiState.panelMode == KeyboardPanelMode.TRANSLATE,
                palette = palette,
                testTag = "ime_btn_translate",
                onClick = { onSelectMode(KeyboardPanelMode.TRANSLATE) },
            )
            ToolbarIconButton(
                icon = Icons.Filled.Height,
                label = "Resize keyboard",
                active = uiState.panelMode == KeyboardPanelMode.RESIZE,
                palette = palette,
                testTag = "ime_btn_resize",
                onClick = { onSelectMode(KeyboardPanelMode.RESIZE) },
            )
            ToolbarIconButton(
                icon = Icons.Filled.Palette,
                label = "Themes",
                active = uiState.panelMode == KeyboardPanelMode.THEMES_QUICK,
                palette = palette,
                testTag = "ime_btn_themes",
                onClick = { onSelectMode(KeyboardPanelMode.THEMES_QUICK) },
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = if (uiState.panelMode == KeyboardPanelMode.VOICE) palette.actionKeyBackground else palette.specialKeyBackground.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onSelectMode(KeyboardPanelMode.VOICE) }
                    .testTag("ime_btn_voice"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice typing",
                        tint = if (uiState.panelMode == KeyboardPanelMode.VOICE) palette.actionKeyText else palette.specialKeyText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    palette: KeyboardPalette,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (active) palette.accentContainer else Color.Transparent,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) palette.onAccentContainer else palette.specialKeyText,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun QuickToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    palette: KeyboardPalette,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.keyBackground,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, null, tint = palette.accent, modifier = Modifier.size(15.dp))
            Text(label, color = palette.keyText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LiveResizeOverlay(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    onHeightScaleChange: (Float) -> Unit,
    onWidthScaleChange: (Float) -> Unit,
    onBottomOffsetChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentScale by remember(settings.keyboardHeightScale) {
        mutableFloatStateOf(settings.keyboardHeightScale)
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.boardBackground.copy(alpha = 0.84f))
            .border(2.dp, palette.accent, RoundedCornerShape(16.dp))
            .testTag("ime_resize_overlay"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Draggable Gboard Resize Bar
            Surface(
                shape = CircleShape,
                color = palette.accent,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                // Dragging up (negative delta) increases height
                                val next = (currentScale - dragAmount / 320f).coerceIn(0.75f, 1.35f)
                                currentScale = next
                                onHeightScaleChange(next)
                            },
                        )
                    }
                    .testTag("ime_resize_drag_handle"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Drag up or down to resize keyboard height",
                        tint = palette.actionKeyText,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Drag up or down · ${(currentScale * 100).toInt()}%",
                        color = palette.actionKeyText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            // Height Presets + Width + Lift Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Short" to 0.80f,
                        "Normal" to 1.00f,
                        "Tall" to 1.16f,
                        "Max" to 1.32f,
                    ).forEach { (label, target) ->
                        val active = kotlin.math.abs(settings.keyboardHeightScale - target) < 0.05f
                        Surface(
                            shape = CircleShape,
                            color = if (active) palette.actionKeyBackground else palette.keyBackground,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onHeightScaleChange(target) },
                        ) {
                            Text(
                                text = label,
                                color = if (active) palette.actionKeyText else palette.keyText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Width:",
                        color = palette.keySubText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    listOf(0.85f to "85%", 0.93f to "93%", 1.0f to "100%").forEach { (w, lbl) ->
                        val active = kotlin.math.abs(settings.keyboardWidthScale - w) < 0.03f
                        Surface(
                            shape = CircleShape,
                            color = if (active) palette.accentContainer else palette.keyBackground,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onWidthScaleChange(w) },
                        ) {
                            Text(
                                text = lbl,
                                color = if (active) palette.onAccentContainer else palette.keyText,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (settings.keyboardBottomOffsetDp > 0) palette.accentContainer else palette.keyBackground,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val nextLift = if (settings.keyboardBottomOffsetDp >= 24) 0 else settings.keyboardBottomOffsetDp + 12
                                onBottomOffsetChange(nextLift)
                            },
                    ) {
                        Text(
                            text = "Space ${settings.keyboardBottomOffsetDp}dp",
                            color = if (settings.keyboardBottomOffsetDp > 0) palette.onAccentContainer else palette.keyText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Bottom Reset & Done Actions (matching Gboard's resize action buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.specialKeyBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onReset)
                        .testTag("ime_resize_reset"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.RestartAlt, null, tint = palette.specialKeyText, modifier = Modifier.size(16.dp))
                        Text("Reset", color = palette.specialKeyText, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = palette.actionKeyBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDone)
                        .testTag("ime_resize_done"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Check, null, tint = palette.actionKeyText, modifier = Modifier.size(16.dp))
                        Text("Done", color = palette.actionKeyText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun QwertyKeyboardBody(
    shiftState: ShiftState,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    showImeSwitcherKey: Boolean,
    isTranslateMode: Boolean,
    spacebarLabel: String,
    onKey: (String) -> Unit,
    onShift: () -> Unit,
    onBackspace: () -> Unit,
    onDeleteWord: () -> Unit,
    onSpace: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onEnter: () -> Unit,
    onSymbols: () -> Unit,
    onEmoji: () -> Unit,
    onSwitchIme: () -> Unit,
    onGestureMove: (List<Pair<Float, Float>>) -> Unit,
    onGestureEnd: (List<Pair<Float, Float>>) -> Unit,
) {
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    val keyHeight = (46f * settings.keyboardHeightScale).dp
    val trailColor = when (settings.trailColorPreset) {
        TrailColorPreset.AUTO -> palette.accent
        else -> Color(settings.trailColorPreset.colorHex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (settings.showNumberRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                numbers.forEach { num ->
                    CharKeyCap(
                        label = num,
                        subHint = null,
                        longPressValue = null,
                        settings = settings,
                        palette = palette,
                        height = (38f * settings.keyboardHeightScale).dp,
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(num) },
                    )
                }
            }
        }

        // Alpha 3-row block with live Gesture Glide Trail overlay
        val strokeOffsets = remember { mutableStateListOf<Offset>() }
        var alphaBoxSize by remember { mutableStateOf(IntSize(1, 1)) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { alphaBoxSize = it }
                .then(
                    if (settings.gestureTypingEnabled) {
                        Modifier.pointerInput(settings.gestureTypingEnabled) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    strokeOffsets.clear()
                                    strokeOffsets.add(start)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pt = change.position
                                    strokeOffsets.add(pt)
                                    val normalized = strokeOffsets.map {
                                        (it.x / alphaBoxSize.width.coerceAtLeast(1)).coerceIn(0f, 1f) to
                                            (it.y / alphaBoxSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    }
                                    onGestureMove(normalized)
                                },
                                onDragEnd = {
                                    val normalized = strokeOffsets.map {
                                        (it.x / alphaBoxSize.width.coerceAtLeast(1)).coerceIn(0f, 1f) to
                                            (it.y / alphaBoxSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    }
                                    strokeOffsets.clear()
                                    onGestureEnd(normalized)
                                },
                                onDragCancel = {
                                    strokeOffsets.clear()
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row1.forEachIndexed { index, ch ->
                        val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                        val digit = ((index + 1) % 10).toString()
                        CharKeyCap(
                            label = display,
                            subHint = if (!settings.showNumberRow) digit else null,
                            longPressValue = if (!settings.showNumberRow) digit else null,
                            settings = settings,
                            palette = palette,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onClick = { onKey(ch) },
                            onLongClick = { onKey(digit) },
                        )
                    }
                }

                // Row 2 (A..L) centered with Gboard 0.5-key side margin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(0.45f))
                    row2.forEach { ch ->
                        val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                        CharKeyCap(
                            label = display,
                            subHint = null,
                            longPressValue = null,
                            settings = settings,
                            palette = palette,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onClick = { onKey(ch) },
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.45f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SpecialKeyCap(
                        settings = settings,
                        palette = palette,
                        height = keyHeight,
                        active = shiftState != ShiftState.OFF,
                        modifier = Modifier
                            .weight(1.45f)
                            .testTag("ime_key_shift"),
                        onClick = onShift,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardCapslock,
                                contentDescription = "Shift",
                                tint = if (shiftState == ShiftState.CAPS_LOCK) {
                                    palette.accent
                                } else if (shiftState == ShiftState.SINGLE) {
                                    palette.onAccentContainer
                                } else {
                                    palette.specialKeyText
                                },
                                modifier = Modifier.size(20.dp),
                            )
                            if (shiftState == ShiftState.CAPS_LOCK) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(2.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent),
                                )
                            }
                        }
                    }

                    row3.forEach { ch ->
                        val display = if (shiftState != ShiftState.OFF) ch.uppercase() else ch
                        CharKeyCap(
                            label = display,
                            subHint = null,
                            longPressValue = null,
                            settings = settings,
                            palette = palette,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onClick = { onKey(ch) },
                        )
                    }

                    var backspaceSwipeAccum by remember { mutableFloatStateOf(0f) }
                    var backspaceHeld by remember { mutableStateOf(false) }

                    LaunchedEffect(backspaceHeld) {
                        if (backspaceHeld) {
                            delay(340)
                            while (backspaceHeld) {
                                onBackspace()
                                delay(58)
                            }
                        }
                    }

                    val shape = keyCornerShape(settings)
                    Box(
                        modifier = Modifier
                            .weight(1.45f)
                            .height(keyHeight)
                            .clip(shape)
                            .background(if (backspaceHeld) palette.keyPressed else palette.specialKeyBackground)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        backspaceHeld = true
                                        tryAwaitRelease()
                                        backspaceHeld = false
                                    },
                                    onTap = { onBackspace() },
                                )
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { backspaceSwipeAccum = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        backspaceSwipeAccum += dragAmount
                                        if (backspaceSwipeAccum < -42f) {
                                            backspaceSwipeAccum = 0f
                                            onDeleteWord()
                                        }
                                    },
                                )
                            }
                            .testTag("ime_key_backspace"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = palette.specialKeyText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Gboard Glide Ribbon Trail
            if (strokeOffsets.size >= 2) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val path = Path().apply {
                        moveTo(strokeOffsets.first().x, strokeOffsets.first().y)
                        for (i in 1 until strokeOffsets.size) {
                            val prev = strokeOffsets[i - 1]
                            val curr = strokeOffsets[i]
                            quadraticTo(
                                prev.x,
                                prev.y,
                                (prev.x + curr.x) / 2f,
                                (prev.y + curr.y) / 2f,
                            )
                        }
                    }
                    drawPath(
                        path = path,
                        color = trailColor.copy(alpha = 0.25f),
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawPath(
                        path = path,
                        color = trailColor,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }

        // Gboard Bottom Utility Row
        BottomActionRow(
            settings = settings,
            palette = palette,
            height = keyHeight,
            leftLabel = "?123",
            showImeSwitcherKey = showImeSwitcherKey,
            isTranslateMode = isTranslateMode,
            spacebarLabel = spacebarLabel,
            onLeftMode = onSymbols,
            onEmoji = onEmoji,
            onComma = { onKey(",") },
            onSpace = onSpace,
            onMoveCursor = onMoveCursor,
            onPeriod = { onKey(".") },
            onEnter = onEnter,
            onSwitchIme = onSwitchIme,
        )
    }
}

@Composable
private fun SymbolsKeyboardBody(
    isExtended: Boolean,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    showImeSwitcherKey: Boolean,
    spacebarLabel: String,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onEnter: () -> Unit,
    onSwitchIme: () -> Unit,
    onToggleExtended: () -> Unit,
    onBackToAbc: () -> Unit,
) {
    val row1 = if (!isExtended) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ")
    }
    val row2 = if (!isExtended) {
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
    } else {
        listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
    }
    val row3 = if (!isExtended) {
        listOf("*", "\"", "'", ":", ";", "!", "?")
    } else {
        listOf("%", "©", "®", "™", "✓", "[", "]")
    }

    val keyHeight = (46f * settings.keyboardHeightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { sym ->
                CharKeyCap(sym, null, null, settings, palette, keyHeight, Modifier.weight(1f)) { onKey(sym) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { sym ->
                CharKeyCap(sym, null, null, settings, palette, keyHeight, Modifier.weight(1f)) { onKey(sym) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SpecialKeyCap(settings, palette, keyHeight, false, Modifier.weight(1.45f), onClick = onToggleExtended) {
                Text(
                    text = if (isExtended) "?123" else "=\\<",
                    color = palette.specialKeyText,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            row3.forEach { sym ->
                CharKeyCap(sym, null, null, settings, palette, keyHeight, Modifier.weight(1f)) { onKey(sym) }
            }
            SpecialKeyCap(settings, palette, keyHeight, false, Modifier.weight(1.45f), onClick = onBackspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = palette.specialKeyText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        BottomActionRow(
            settings = settings,
            palette = palette,
            height = keyHeight,
            leftLabel = "ABC",
            showImeSwitcherKey = showImeSwitcherKey,
            isTranslateMode = false,
            spacebarLabel = spacebarLabel,
            onLeftMode = onBackToAbc,
            onEmoji = {},
            onComma = { onKey(",") },
            onSpace = onSpace,
            onMoveCursor = onMoveCursor,
            onPeriod = { onKey(".") },
            onEnter = onEnter,
            onSwitchIme = onSwitchIme,
        )
    }
}

@Composable
private fun BottomActionRow(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    height: Dp,
    leftLabel: String,
    showImeSwitcherKey: Boolean,
    isTranslateMode: Boolean,
    spacebarLabel: String,
    onLeftMode: () -> Unit,
    onEmoji: () -> Unit,
    onComma: () -> Unit,
    onSpace: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onPeriod: () -> Unit,
    onEnter: () -> Unit,
    onSwitchIme: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpecialKeyCap(
            settings = settings,
            palette = palette,
            height = height,
            modifier = Modifier
                .weight(1.42f)
                .testTag("ime_key_mode_switch"),
            onClick = onLeftMode,
        ) {
            Text(
                text = leftLabel,
                color = palette.specialKeyText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }

        CharKeyCap(",", null, null, settings, palette, height, Modifier.weight(0.88f), onClick = onComma)

        SpecialKeyCap(
            settings = settings,
            palette = palette,
            height = height,
            modifier = Modifier
                .weight(0.92f)
                .testTag("ime_key_emoji"),
            onClick = onEmoji,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEmotions,
                contentDescription = "Emojis",
                tint = palette.specialKeyText,
                modifier = Modifier.size(19.dp),
            )
        }

        if (showImeSwitcherKey) {
            SpecialKeyCap(
                settings = settings,
                palette = palette,
                height = height,
                modifier = Modifier
                    .weight(0.88f)
                    .testTag("ime_key_globe"),
                onClick = onSwitchIme,
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Switch keyboard",
                    tint = palette.specialKeyText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Gboard Spacebar with horizontal cursor trackpad & long-press IME picker
        var spaceDragAccum by remember { mutableFloatStateOf(0f) }
        val shape = keyCornerShape(settings)
        Box(
            modifier = Modifier
                .weight(if (showImeSwitcherKey) 3.55f else 4.35f)
                .height(height)
                .then(
                    if (settings.keyShape != KeyboardKeyShape.BORDERLESS) {
                        Modifier.shadow(elevation = 1.dp, shape = shape, ambientColor = palette.keyShadow, spotColor = palette.keyShadow)
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(palette.keyBackground)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onSpace() },
                        onLongPress = { onSwitchIme() },
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { spaceDragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            spaceDragAccum += dragAmount
                            if (spaceDragAccum > 26f) {
                                spaceDragAccum = 0f
                                onMoveCursor(1)
                            } else if (spaceDragAccum < -26f) {
                                spaceDragAccum = 0f
                                onMoveCursor(-1)
                            }
                        },
                    )
                }
                .testTag("ime_key_space"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = spacebarLabel,
                color = palette.keySubText.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CharKeyCap(".", null, null, settings, palette, height, Modifier.weight(0.88f), onClick = onPeriod)

        // Material You Gboard Pill Action/Enter key
        Box(
            modifier = Modifier
                .weight(1.6f)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.actionKeyBackground)
                .clickable(onClick = onEnter)
                .testTag("ime_key_enter"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isTranslateMode) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Filled.KeyboardReturn,
                contentDescription = if (isTranslateMode) "Insert translation" else "Enter",
                tint = palette.actionKeyText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CharKeyCap(
    label: String,
    subHint: String?,
    longPressValue: String?,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    height: Dp,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    var longPressedPreview by remember { mutableStateOf<String?>(null) }
    val shape = keyCornerShape(settings)
    val bgColor = when {
        pressed -> palette.keyPressed
        settings.keyShape == KeyboardKeyShape.BORDERLESS -> Color.Transparent
        else -> palette.keyBackground
    }

    Box(
        modifier = modifier.height(height),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (settings.keyShape != KeyboardKeyShape.BORDERLESS) {
                        Modifier.shadow(elevation = 1.dp, shape = shape, ambientColor = palette.keyShadow, spotColor = palette.keyShadow)
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(bgColor)
                .pointerInput(label, longPressValue) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            longPressedPreview = null
                            tryAwaitRelease()
                            pressed = false
                            longPressedPreview = null
                        },
                        onLongPress = if (onLongClick != null && longPressValue != null) {
                            {
                                longPressedPreview = longPressValue
                                onLongClick()
                            }
                        } else {
                            null
                        },
                        onTap = { onClick() },
                    )
                }
                .testTag("ime_key_$label"),
            contentAlignment = Alignment.Center,
        ) {
            if (subHint != null) {
                Text(
                    text = subHint,
                    color = palette.keySubText.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    fontFamily = JetBrainsMonoFamily,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp),
                )
            }
            Text(
                text = label,
                color = palette.keyText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        if (pressed && settings.keyPopups) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.keyBackground,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset(y = (-44).dp)
                    .size(width = 38.dp, height = 44.dp)
                    .border(1.dp, palette.divider, RoundedCornerShape(12.dp)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = longPressedPreview ?: label,
                        color = palette.keyText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecialKeyCap(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    height: Dp,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = keyCornerShape(settings)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (active) palette.accentContainer else palette.specialKeyBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun keyCornerShape(settings: KeyboardSettings): RoundedCornerShape {
    return when (settings.keyShape) {
        KeyboardKeyShape.PILL -> RoundedCornerShape(999.dp)
        KeyboardKeyShape.BORDERLESS -> RoundedCornerShape(8.dp)
        KeyboardKeyShape.ROUNDED -> RoundedCornerShape(settings.keyCornerRadiusDp.dp)
    }
}
