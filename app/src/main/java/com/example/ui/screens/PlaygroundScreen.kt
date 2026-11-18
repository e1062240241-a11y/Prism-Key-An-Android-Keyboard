package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.StudioViewModel
import com.example.ui.keyboard.ExpressiveKeyboardView
import com.example.ui.keyboard.KeyboardPanelMode
import com.example.ui.keyboard.StickerThumb

@Composable
fun PlaygroundScreen(
    viewModel: StudioViewModel,
    onShowImePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playground by viewModel.playground.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var systemRootTestText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Scrollable top studio area
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero + Android Root / Default Keyboard Activation Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                ),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_keyboard_hero),
                            contentDescription = "PrismKeys keyboard studio art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                        ),
                                    ),
                                ),
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "Welcome to PrismKeys",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Fast, helpful & 100% private on your phone",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "100% Private",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }
                    }

                    // 2-Step Android System-Wide Root Keyboard Setup
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val isDefault = imeStatus.isEnabledInSettings && imeStatus.isCurrentDefaultIme
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (isDefault) Icons.Filled.CheckCircle else Icons.Filled.Keyboard,
                                contentDescription = null,
                                tint = if (isDefault) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isDefault -> "You're All Set! Default Keyboard Ready"
                                        imeStatus.isEnabledInSettings -> "One Last Step: Pick PrismKeys"
                                        else -> "Use PrismKeys in Every App"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val subtitle = when {
                                    isDefault -> "Enjoy private offline typing across all your chats, search & apps."
                                    imeStatus.isEnabledInSettings -> "Currently using ${imeStatus.activeDefaultImeLabel ?: "another keyboard"} — tap below to switch."
                                    else -> "Turn it on in 2 quick taps so you can use it anywhere on your phone."
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Step indicators & Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!imeStatus.isEnabledInSettings) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("enable_ime_button"),
                                ) {
                                    Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("1. Turn On")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.testTag("enable_ime_button"),
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Turned On")
                                }
                            }

                            if (!isDefault) {
                                Button(
                                    onClick = {
                                        if (!imeStatus.isEnabledInSettings) {
                                            context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                        } else {
                                            onShowImePicker()
                                        }
                                    },
                                    enabled = imeStatus.isEnabledInSettings,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("select_ime_button"),
                                ) {
                                    Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("2. Set as Main")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = onShowImePicker,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("select_ime_button"),
                                ) {
                                    Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Switch Keyboard")
                                }
                            }
                        }

                        // Step checklist row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ImeStepBadge("Turned on in Settings", imeStatus.isEnabledInSettings)
                            ImeStepBadge("Active on your phone", imeStatus.isCurrentDefaultIme)
                        }

                        // Real System Input Field to pop up the Android OS Root IME immediately
                        OutlinedTextField(
                            value = systemRootTestText,
                            onValueChange = { systemRootTestText = it },
                            label = { Text("Try your active phone keyboard") },
                            placeholder = { Text("Tap here to type with your active keyboard...") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("system_ime_test_field"),
                        )
                    }
                }
            }

            // Live Interactive Studio Sandbox Editor
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp),
                    ),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "TRY TYPING HERE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.keyboardController.setPanelMode(KeyboardPanelMode.RESIZE)
                                    }
                                    .testTag("sandbox_resize_button"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Height,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Resize ${(settings.keyboardHeightScale * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.keyboardController.setPanelMode(KeyboardPanelMode.TRANSLATE)
                                    },
                            ) {
                                Text(
                                    text = "${settings.sourceLangCode.uppercase()}→${settings.targetLangCode.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                )
                            }
                            IconButton(
                                onClick = { viewModel.copyPlaygroundToClipboard() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("sandbox_copy_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy to local clipboard",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearPlayground() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("sandbox_clear_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "Clear sandbox",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val annotated = buildAnnotatedString {
                        append(playground.committedBeforeCursor)
                        if (playground.composingText.isNotEmpty()) {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            ) {
                                append(playground.composingText)
                            }
                        }
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append("|")
                        }
                        append(playground.committedAfterCursor)
                    }

                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_editor_text"),
                    )

                    AnimatedVisibility(visible = playground.sentStickers.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "SENT STICKERS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(playground.sentStickers) { sticker ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            StickerThumb(
                                                sticker = sticker,
                                                stickerRepository = viewModel.stickerRepository,
                                                modifier = Modifier.size(36.dp),
                                            )
                                            Text(
                                                text = sticker.label,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = playground.statusNotice != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { viewModel.dismissPlaygroundNotice() },
                        ) {
                            Text(
                                text = playground.statusNotice.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // Docked Live Gboard-Style Keyboard inside Studio Playground
        ExpressiveKeyboardView(
            controller = viewModel.keyboardController,
            stickerRepository = viewModel.stickerRepository,
            showImeSwitcherKey = true,
        )
    }
}

@Composable
private fun ImeStepBadge(
    label: String,
    completed: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
