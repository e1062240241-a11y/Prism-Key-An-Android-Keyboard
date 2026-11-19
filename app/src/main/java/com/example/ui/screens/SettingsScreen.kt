package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StudioViewModel

@Composable
fun SettingsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val learnedWords by viewModel.learnedWords.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
    }

    var newWordInput by remember { mutableStateOf("") }

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
                        text = "Privacy & Typing",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Everything you type stays private on your phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Local Privacy Shield Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp),
                            )
                            Column {
                                Text(
                                    text = "100% Private by Design",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Your words, copied clips, and custom dictionary never leave your device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = if (hasMicPermission) Icons.Filled.CheckCircle else Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = if (hasMicPermission) {
                                        "Voice Typing Ready"
                                    } else {
                                        "Voice Typing Needs Mic"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            if (!hasMicPermission) {
                                Button(
                                    onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.testTag("grant_mic_button"),
                                ) {
                                    Text("Allow Mic")
                                }
                            }
                        }
                    }
                }
            }

            // System-Wide Android Default Keyboard Card
            item {
                val isDefault = imeStatus.isEnabledInSettings && imeStatus.isCurrentDefaultIme
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (isDefault) Icons.Filled.CheckCircle else Icons.Filled.Keyboard,
                                contentDescription = null,
                                tint = if (isDefault) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text("Use Across Your Whole Phone", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = when {
                                        isDefault -> "You're all set! PrismKeys works across all your apps."
                                        imeStatus.isEnabledInSettings -> "Turned on · Currently using ${imeStatus.activeDefaultImeLabel ?: "Phone Keyboard"}"
                                        else -> "Turn on PrismKeys in Settings to type with it everywhere."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (imeStatus.isEnabledInSettings) "Keyboard Settings" else "Turn On")
                            }
                            Button(
                                onClick = {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.showInputMethodPicker()
                                },
                                enabled = imeStatus.isEnabledInSettings,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (isDefault) "Switch Keyboard" else "Make Main")
                            }
                        }
                    }
                }
            }

            // Typing Intelligence Settings
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Helpful Typing Features", style = MaterialTheme.typography.titleMedium)

                        SettingToggleRow(
                            title = "Swipe to Type",
                            subtitle = "Slide across letters to type words faster",
                            checked = settings.gestureTypingEnabled,
                            onCheckedChange = { viewModel.setGestureTyping(it) },
                        )
                        SettingToggleRow(
                            title = "Word Suggestions",
                            subtitle = "See helpful next words above the keys",
                            checked = settings.suggestionsEnabled,
                            onCheckedChange = { viewModel.setSuggestions(it) },
                        )
                        SettingToggleRow(
                            title = "Smart Auto-Fix",
                            subtitle = "Fix small spelling typos when you tap space",
                            checked = settings.autocorrectEnabled,
                            onCheckedChange = { viewModel.setAutocorrect(it) },
                        )
                        SettingToggleRow(
                            title = "Auto-Capitalize",
                            subtitle = "Start every new sentence with a capital letter",
                            checked = settings.autoCapitalize,
                            onCheckedChange = { viewModel.setAutoCapitalize(it) },
                        )
                        SettingToggleRow(
                            title = "Save Copied Text",
                            subtitle = "Keep copied text handy inside your keyboard",
                            checked = settings.clipboardAutoCapture,
                            onCheckedChange = { viewModel.setClipboardAutoCapture(it) },
                        )
                    }
                }
            }

            // Personal Dictionary Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = "Your Custom Words (${learnedWords.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Learns words and phrases you type privately on your phone",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (learnedWords.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.clearPersonalDictionary() },
                                    shape = RoundedCornerShape(999.dp),
                                ) {
                                    Text("Reset")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newWordInput,
                                onValueChange = { newWordInput = it },
                                placeholder = { Text("Add a name, nickname, or favorite word...") },
                                singleLine = true,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    viewModel.addPersonalWord(newWordInput)
                                    newWordInput = ""
                                },
                                enabled = newWordInput.trim().length >= 2,
                                shape = CircleShape,
                                modifier = Modifier.height(50.dp),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add word")
                            }
                        }
                    }
                }
            }

            items(learnedWords, key = { it.word }) { entry ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(entry.word, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Used ${entry.frequency} times",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.removePersonalWord(entry.word) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove word")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
