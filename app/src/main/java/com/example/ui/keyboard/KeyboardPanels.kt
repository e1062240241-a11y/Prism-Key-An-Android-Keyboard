package com.example.ui.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.StickerEntity
import com.example.data.local.StickerPackEntity
import com.example.data.preferences.KeyboardSettings
import com.example.data.preferences.OneHandedMode
import com.example.data.repository.StickerRepository
import com.example.engine.LanguageCatalog
import com.example.engine.SupportedLanguage
import com.example.engine.TranslationResult
import com.example.engine.VoiceTypingState
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.KeyboardThemePreset
import java.io.File

@Composable
fun InlineTranslateBar(
    settings: KeyboardSettings,
    translateInput: String,
    translationResult: TranslationResult?,
    isTranslating: Boolean,
    downloadedModels: Set<String>,
    palette: KeyboardPalette,
    onSwapLanguages: () -> Unit,
    onSelectLanguages: (String, String) -> Unit,
    onClearInput: () -> Unit,
    onInsertTranslation: () -> Unit,
    onClose: () -> Unit,
) {
    val srcLang = LanguageCatalog.findSourceByCode(settings.sourceLangCode)
    val dstLang = LanguageCatalog.findByCode(settings.targetLangCode)
    var pickingForSource by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LanguagePillButton(
                lang = srcLang,
                isNeuralInstalled = srcLang.code in downloadedModels,
                palette = palette,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ime_translate_source_lang"),
                onClick = { pickingForSource = true },
            )
            IconButton(
                onClick = onSwapLanguages,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.keyBackground)
                    .testTag("ime_translate_swap"),
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap languages",
                    tint = palette.accent,
                )
            }
            LanguagePillButton(
                lang = dstLang,
                isNeuralInstalled = dstLang.code in downloadedModels,
                palette = palette,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ime_translate_target_lang"),
                onClick = { pickingForSource = false },
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close translate",
                    tint = palette.keySubText,
                )
            }
        }

        if (pickingForSource != null) {
            val isSource = pickingForSource == true
            InlineLanguagePicker(
                selectedCode = if (isSource) srcLang.code else dstLang.code,
                includeAutoDetect = isSource,
                downloadedModels = downloadedModels,
                palette = palette,
                onPick = { picked ->
                    if (isSource) {
                        onSelectLanguages(picked.code, dstLang.code)
                    } else {
                        onSelectLanguages(srcLang.code, picked.code)
                    }
                    pickingForSource = null
                },
                onDismiss = { pickingForSource = null },
            )
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.keyBackground,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (translateInput.isEmpty()) {
                                "Type or swipe below to translate..."
                            } else {
                                translateInput
                            },
                            color = if (translateInput.isEmpty()) palette.keySubText else palette.keyText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (translateInput.isNotEmpty()) {
                            IconButton(
                                onClick = onClearInput,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear translation text",
                                    tint = palette.keySubText,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    if (translationResult != null && translationResult.translatedText.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.accentContainer)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (translationResult.detectedSourceCode != null) {
                                    val detectedName = LanguageCatalog.findByCode(translationResult.detectedSourceCode).englishName
                                    val confPct = ((translationResult.detectedConfidence ?: 0f) * 100).toInt()
                                    Text(
                                        text = "Detected: $detectedName ($confPct%)",
                                        color = palette.accent,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                                Text(
                                    text = translationResult.translatedText,
                                    color = palette.onAccentContainer,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${translationResult.engineKind.label} · On your phone",
                                    color = palette.onAccentContainer.copy(alpha = 0.78f),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = palette.actionKeyBackground,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onInsertTranslation)
                                    .testTag("ime_translate_insert"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Insert translation",
                                        tint = palette.actionKeyText,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Insert",
                                        color = palette.actionKeyText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    } else if (isTranslating) {
                        Text(
                            text = "Translating...",
                            color = palette.accent,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePillButton(
    lang: SupportedLanguage,
    isNeuralInstalled: Boolean,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = palette.keyBackground,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = lang.flag, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = lang.englishName,
                color = palette.keyText,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isNeuralInstalled || lang.hasBuiltInLexicon) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isNeuralInstalled) Color(0xFF10B981) else palette.accent),
                )
            }
        }
    }
}

@Composable
private fun InlineLanguagePicker(
    selectedCode: String,
    includeAutoDetect: Boolean,
    downloadedModels: Set<String>,
    palette: KeyboardPalette,
    onPick: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, includeAutoDetect) {
        val base = if (includeAutoDetect && query.isBlank()) {
            listOf(LanguageCatalog.autoDetectLanguage) + LanguageCatalog.allLanguages
        } else {
            LanguageCatalog.allLanguages
        }
        base.filter {
            query.isBlank() ||
                it.englishName.contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true) ||
                it.code.equals(query, ignoreCase = true)
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.keyBackground,
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Type a language name...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close language list", tint = palette.keySubText)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.code }) { lang ->
                    val isSelected = lang.code == selectedCode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) palette.accentContainer else palette.boardBackgroundAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(lang) },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = lang.flag, fontSize = 18.sp)
                            Text(
                                text = lang.englishName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) palette.onAccentContainer else palette.keyText,
                            )
                            Text(
                                text = if (lang.code in downloadedModels) "Full Pack" else if (lang.hasBuiltInLexicon) "Built-in" else "Tap to Get",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.keySubText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardPanel(
    items: List<ClipboardItemEntity>,
    searchQuery: String,
    pinnedOnly: Boolean,
    heightScale: Float,
    palette: KeyboardPalette,
    onSearchChange: (String) -> Unit,
    onTogglePinnedFilter: () -> Unit,
    onPasteItem: (ClipboardItemEntity) -> Unit,
    onTogglePin: (ClipboardItemEntity) -> Unit,
    onDeleteItem: (ClipboardItemEntity) -> Unit,
    onClearUnpinned: () -> Unit,
    onClose: () -> Unit,
) {
    var showSearchBar by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    val panelHeight = (246f * heightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.boardBackground)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Saved Clips",
                    color = palette.keyText,
                    style = MaterialTheme.typography.titleSmall,
                )
                Surface(
                    shape = CircleShape,
                    color = if (pinnedOnly) palette.accentContainer else palette.specialKeyBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePinnedFilter)
                        .testTag("ime_clipboard_pin_filter"),
                ) {
                    Text(
                        text = if (pinnedOnly) "Pinned Only" else "All (${items.size})",
                        color = if (pinnedOnly) palette.onAccentContainer else palette.specialKeyText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (showSearchBar && searchQuery.isNotEmpty()) {
                            onSearchChange("")
                        }
                        showSearchBar = !showSearchBar
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ime_clipboard_search_toggle"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search clipboard history",
                        tint = if (showSearchBar) palette.accent else palette.keySubText,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onClearUnpinned,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear unpinned",
                        tint = palette.keySubText,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Back to keys",
                        tint = palette.keySubText,
                    )
                }
            }
        }

        if (showSearchBar) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Type to filter saved clips...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, "Clear search", modifier = Modifier.size(15.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ime_clipboard_search_input"),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        tint = palette.keySubText,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (pinnedOnly) "No pinned clips yet" else "Anything you copy stays safely right here",
                        color = palette.keySubText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.keyBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPasteItem(item) }
                            .testTag("ime_clipboard_item_${item.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.accentContainer,
                                    ) {
                                        Text(
                                            text = item.category,
                                            color = palette.onAccentContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        )
                                    }
                                    if (item.isPinned) {
                                        Text(
                                            text = "PINNED",
                                            color = palette.accent,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                    if (item.usageCount > 0) {
                                        Text(
                                            text = "· ${item.usageCount}×",
                                            color = palette.keySubText,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.content,
                                    color = palette.keyText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(
                                onClick = { onTogglePin(item) },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (item.isPinned) "Unpin" else "Pin",
                                    tint = if (item.isPinned) palette.accent else palette.keySubText,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = { onDeleteItem(item) },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete clip",
                                    tint = palette.keySubText,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPanel(
    recentEmojis: List<String>,
    heightScale: Float,
    palette: KeyboardPalette,
    onEmojiTapped: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackToKeys: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(EmojiCatalog.categories.first()) }
    val panelHeight = (246f * heightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.boardBackground)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.specialKeyBackground,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBackToKeys),
            ) {
                Text(
                    text = "ABC",
                    color = palette.specialKeyText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            EmojiCatalog.categories.forEach { cat ->
                val active = cat.id == selectedCategory.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) palette.accentContainer else palette.keyBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedCategory = cat },
                ) {
                    Text(
                        text = "${cat.iconEmoji} ${cat.label}",
                        color = if (active) palette.onAccentContainer else palette.keyText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent",
                color = palette.keySubText,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 4.dp),
            )
            recentEmojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.keyBackground)
                        .clickable { onEmojiTapped(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 42.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(end = 46.dp),
            ) {
                items(selectedCategory.emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEmojiTapped(emoji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.specialKeyBackground,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onBackspace),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = palette.specialKeyText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun StickersPanel(
    packs: List<StickerPackEntity>,
    stickers: List<StickerEntity>,
    selectedPackId: Long?,
    heightScale: Float,
    stickerRepository: StickerRepository,
    palette: KeyboardPalette,
    onSelectPack: (Long) -> Unit,
    onStickerTapped: (StickerEntity) -> Unit,
    onClose: () -> Unit,
) {
    val activePackId = selectedPackId ?: packs.firstOrNull()?.id
    val visibleStickers = remember(activePackId, stickers) {
        if (activePackId == null) stickers else stickers.filter { it.packId == activePackId }
    }
    val panelHeight = (246f * heightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.boardBackground)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(packs, key = { it.id }) { pack ->
                    val selected = pack.id == activePackId
                    Surface(
                        shape = CircleShape,
                        color = if (selected) palette.accentContainer else palette.keyBackground,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectPack(pack.id) },
                    ) {
                        Text(
                            text = pack.name,
                            color = if (selected) palette.onAccentContainer else palette.keyText,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, "Back to keys", tint = palette.keySubText)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (visibleStickers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Add custom stickers in the Saved tab",
                    color = palette.keySubText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleStickers, key = { it.id }) { sticker ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.keyBackground,
                        modifier = Modifier
                            .height(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onStickerTapped(sticker) }
                            .testTag("ime_sticker_${sticker.id}"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            StickerThumb(
                                sticker = sticker,
                                stickerRepository = stickerRepository,
                                modifier = Modifier.size(54.dp),
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = sticker.label,
                                color = palette.keySubText,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerThumb(
    sticker: StickerEntity,
    stickerRepository: StickerRepository,
    modifier: Modifier = Modifier,
) {
    val resId = remember(sticker.drawableResName) {
        stickerRepository.resolveDrawableId(sticker.drawableResName)
    }
    val localFile = remember(sticker.localFilePath) {
        sticker.localFilePath?.let { File(it) }?.takeIf { it.exists() }
    }

    when {
        localFile != null -> {
            Image(
                painter = rememberAsyncImagePainter(model = localFile),
                contentDescription = sticker.label,
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(RoundedCornerShape(14.dp)),
            )
        }
        resId != null -> {
            Image(
                painter = painterResource(id = resId),
                contentDescription = sticker.label,
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(RoundedCornerShape(14.dp)),
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = sticker.emojiFallback, fontSize = 28.sp)
            }
        }
    }
}

@Composable
fun VoiceTypingPanel(
    voiceState: VoiceTypingState,
    heightScale: Float,
    palette: KeyboardPalette,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClose: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb_scale",
    )
    val panelHeight = (246f * heightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.boardBackground)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = CircleShape,
                color = palette.accentContainer,
            ) {
                Text(
                    text = "VOICE TYPING",
                    color = palette.onAccentContainer,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, "Back to keys", tint = palette.keySubText)
            }
        }

        val isListening = voiceState is VoiceTypingState.Listening
        val rmsScale = if (voiceState is VoiceTypingState.Listening) {
            1f + (voiceState.rmsDb / 24f)
        } else {
            1f
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    palette.accent.copy(alpha = if (isListening) 0.22f * pulse else 0.12f),
                )
                .clickable {
                    if (isListening) onStopListening() else onStartListening()
                }
                .testTag("ime_voice_orb"),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size((62 * rmsScale).coerceIn(58f, 78f).dp)
                    .clip(CircleShape)
                    .background(palette.actionKeyBackground),
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start voice typing",
                    tint = palette.actionKeyText,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        val statusText = when (voiceState) {
            is VoiceTypingState.Idle -> "Tap the mic to start speaking"
            is VoiceTypingState.Listening -> voiceState.partialTranscript.ifEmpty {
                if (voiceState.isOnDeviceEngine) "Listening clearly..." else "Listening..."
            }
            is VoiceTypingState.Error -> voiceState.message
        }

        Text(
            text = statusText,
            color = if (voiceState is VoiceTypingState.Error) palette.accent else palette.keyText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (voiceState is VoiceTypingState.Error) {
            Button(
                onClick = onStartListening,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.actionKeyBackground,
                    contentColor = palette.actionKeyText,
                ),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(if (voiceState.needsMicPermission) "Allow Mic & Try Again" else "Try Again")
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun QuickThemesPanel(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    onSelectTheme: (KeyboardThemePreset) -> Unit,
    onToggleNumberRow: () -> Unit,
    onCycleOneHanded: () -> Unit,
    onOpenResize: () -> Unit,
    onClose: () -> Unit,
) {
    val panelHeight = (246f * settings.keyboardHeightScale).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.boardBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Material Color Themes",
                color = palette.keyText,
                style = MaterialTheme.typography.titleSmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = palette.accentContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenResize)
                        .testTag("ime_quick_themes_resize"),
                ) {
                    Text(
                        text = "Resize",
                        color = palette.onAccentContainer,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, "Close themes", tint = palette.keySubText)
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(KeyboardThemePreset.entries) { preset ->
                val selected = settings.themePreset == preset
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) palette.accentContainer else palette.keyBackground,
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) palette.accent else palette.divider,
                            shape = RoundedCornerShape(18.dp),
                        )
                        .clickable { onSelectTheme(preset) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = preset.displayName,
                                color = if (selected) palette.onAccentContainer else palette.keyText,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active",
                                    tint = palette.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.tagline,
                            color = palette.keySubText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (settings.showNumberRow) palette.accentContainer else palette.keyBackground,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onToggleNumberRow),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Dialpad, null, tint = palette.accent)
                    Column {
                        Text(
                            text = "Number Row",
                            color = palette.keyText,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = if (settings.showNumberRow) "Shown" else "Hidden",
                            color = palette.keySubText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (settings.oneHandedMode != OneHandedMode.NONE) palette.accentContainer else palette.keyBackground,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onCycleOneHanded),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.SwapHoriz, null, tint = palette.accent)
                    Column {
                        Text(
                            text = "One-Handed",
                            color = palette.keyText,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = settings.oneHandedMode.label,
                            color = palette.keySubText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

