package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.LanguageCatalog
import com.example.engine.SupportedLanguage
import com.example.ui.StudioViewModel

@Composable
fun TranslationHubScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val downloadedModels by viewModel.downloadedModels.collectAsStateWithLifecycle()
    val downloadingModels by viewModel.downloadingModels.collectAsStateWithLifecycle()
    val workbenchResult by viewModel.workbenchResult.collectAsStateWithLifecycle()
    val statusMessage by viewModel.translationStatusMessage.collectAsStateWithLifecycle()
    val isMlKitReady by viewModel.isMlKitReady.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("Good morning! On my way, thank you.") }
    var langFilter by remember { mutableStateOf("") }
    var pickerTarget by remember { mutableStateOf<String?>(null) } // "SOURCE" or "TARGET"

    val sourceLang = LanguageCatalog.findSourceByCode(settings.sourceLangCode)
    val targetLang = LanguageCatalog.findByCode(settings.targetLangCode)

    val filteredLanguages = remember(langFilter) {
        LanguageCatalog.allLanguages.filter {
            langFilter.isBlank() ||
                it.englishName.contains(langFilter, ignoreCase = true) ||
                it.nativeName.contains(langFilter, ignoreCase = true) ||
                it.code.equals(langFilter, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Offline Translator",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Translate 59 languages right on your phone — even without internet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Strict Offline Privacy Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Column {
                                Text(
                                    text = "100% Private Offline Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    text = if (settings.strictOfflineMode) {
                                        "Everything stays on your phone. Turn off briefly only to save a new language."
                                    } else {
                                        "Language downloads allowed · ${downloadedModels.size} packs saved"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                        Switch(
                            checked = settings.strictOfflineMode,
                            onCheckedChange = { viewModel.setStrictOfflineMode(it) },
                            modifier = Modifier.testTag("strict_offline_switch"),
                        )
                    }
                }
            }

            // Interactive Workbench Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(CircleShape)
                                    .clickable { pickerTarget = "SOURCE" }
                                    .testTag("hub_source_lang_btn"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(sourceLang.flag, fontSize = 18.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        sourceLang.englishName,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.swapLanguages()
                                    workbenchResult?.translatedText?.takeIf { it.isNotBlank() }?.let {
                                        inputText = it
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("hub_swap_lang_btn"),
                            ) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    contentDescription = "Swap languages",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(CircleShape)
                                    .clickable { pickerTarget = "TARGET" }
                                    .testTag("hub_target_lang_btn"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(targetLang.flag, fontSize = 18.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        targetLang.englishName,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val autoActive = settings.sourceLangCode == LanguageCatalog.AUTO_DETECT_CODE
                            Surface(
                                shape = CircleShape,
                                color = if (autoActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val nextSource = if (autoActive) "en" else LanguageCatalog.AUTO_DETECT_CODE
                                        viewModel.setLanguages(nextSource, targetLang.code)
                                        viewModel.translateWorkbench(inputText)
                                    }
                                    .testTag("hub_auto_detect_chip"),
                            ) {
                                Text(
                                    text = if (autoActive) "✨ Auto-Detect Language On" else "✨ Auto-Detect Language",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (autoActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                            Text(
                                text = "${downloadedModels.size} packs saved offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.translateWorkbench(it)
                            },
                            label = { Text("Text in ${sourceLang.englishName}") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hub_translate_input"),
                        )

                        Button(
                            onClick = { viewModel.translateWorkbench(inputText) },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("hub_translate_button"),
                        ) {
                            Icon(Icons.Filled.Translate, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Translate to ${targetLang.englishName}")
                        }

                        val result = workbenchResult
                        if (result != null && result.translatedText.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (result.detectedSourceCode != null) {
                                        val detected = LanguageCatalog.findByCode(result.detectedSourceCode)
                                        val pct = ((result.detectedConfidence ?: 0f) * 100).toInt()
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        ) {
                                            Text(
                                                text = "Detected: ${detected.flag} ${detected.englishName} ($pct%)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = result.translatedText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.testTag("hub_translate_output"),
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "${result.engineKind.label} · On your phone",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    cm?.setPrimaryClip(ClipData.newPlainText("Translation", result.translatedText))
                                                },
                                            ) {
                                                Icon(
                                                    Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy translation",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.addClipboardSnippet(result.translatedText, pinned = true)
                                                },
                                            ) {
                                                Icon(
                                                    Icons.Filled.BookmarkAdd,
                                                    contentDescription = "Pin to Saved Clips",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = statusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.dismissTranslationStatus() },
                    ) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            // 59 On-Device Language Directory Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "All 59 Offline Languages",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = langFilter,
                        onValueChange = { langFilter = it },
                        placeholder = { Text("Type a language name (e.g. Spanish, Japanese)...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            items(filteredLanguages, key = { it.code }) { lang ->
                val isNeuralInstalled = lang.code in downloadedModels
                val isDownloading = lang.code in downloadingModels

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(lang.flag, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lang.englishName} · ${lang.nativeName}",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (lang.hasBuiltInLexicon) {
                                    Text(
                                        text = "Built-in Phrasebook",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (isNeuralInstalled) {
                                    Text(
                                        text = "· Saved Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }

                        when {
                            lang.code == "en" -> {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "English ready",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            isDownloading -> {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            }
                            isNeuralInstalled -> {
                                IconButton(onClick = { viewModel.deleteLanguagePack(lang.code) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove language pack",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            else -> {
                                IconButton(onClick = { viewModel.downloadLanguagePack(lang.code) }) {
                                    Icon(
                                        Icons.Filled.CloudDownload,
                                        contentDescription = "Save language pack",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickerTarget != null) {
        val isSource = pickerTarget == "SOURCE"
        LanguagePickerDialog(
            title = if (isSource) "Translate From" else "Translate To",
            selectedCode = if (isSource) sourceLang.code else targetLang.code,
            includeAutoDetect = isSource,
            downloadedModels = downloadedModels,
            onSelect = { picked ->
                if (isSource) {
                    viewModel.setLanguages(picked.code, targetLang.code)
                } else {
                    viewModel.setLanguages(sourceLang.code, picked.code)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    title: String,
    selectedCode: String,
    includeAutoDetect: Boolean,
    downloadedModels: Set<String>,
    onSelect: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val list = remember(query, includeAutoDetect) {
        val base = if (includeAutoDetect && query.isBlank()) {
            listOf(LanguageCatalog.autoDetectLanguage) + LanguageCatalog.allLanguages
        } else {
            LanguageCatalog.allLanguages
        }
        base.filter {
            query.isBlank() ||
                it.englishName.contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.height(340.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search 59 languages...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list, key = { it.code }) { lang ->
                        val selected = lang.code == selectedCode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelect(lang) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(lang.flag, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lang.englishName, style = MaterialTheme.typography.titleSmall)
                                    Text(lang.nativeName, style = MaterialTheme.typography.labelSmall)
                                }
                                if (lang.code in downloadedModels || lang.hasBuiltInLexicon) {
                                    Text(
                                        text = if (lang.code in downloadedModels) "Full Pack" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
