package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.StickerEntity
import com.example.data.local.StickerPackEntity
import com.example.ui.StudioViewModel
import com.example.ui.keyboard.StickerThumb

@Composable
fun VaultScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Clipboard, 1 = Stickers

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Saved Clips & Stickers",
                style = MaterialTheme.typography.headlineMedium,
            )

            // Expressive Pill Segmented Switcher
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Copied Text", "Stickers").forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .clickable { selectedTab = idx }
                                .testTag("vault_tab_$idx"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                ClipboardVaultTab(viewModel = viewModel, modifier = Modifier.weight(1f))
            } else {
                CustomStickersTab(viewModel = viewModel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClipboardVaultTab(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.clipboardItems.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var pinnedOnly by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(items, search, pinnedOnly) {
        items.filter { item ->
            (!pinnedOnly || item.isPinned) &&
                (search.isBlank() || item.content.contains(search, ignoreCase = true))
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search things you've copied...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (pinnedOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .clickable { pinnedOnly = !pinnedOnly },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.PushPin, null, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (pinnedOnly) "Pinned Only" else "All (${items.size})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = { viewModel.clearUnpinnedClipboard() },
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear Older")
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.testTag("vault_add_clip_button"),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Note")
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nothing copied yet — anything you copy appears safely here",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filtered, key = { it.id }) { clip ->
                    ClipboardVaultCard(
                        item = clip,
                        onCopy = { viewModel.copyClipboardItem(clip) },
                        onTogglePin = { viewModel.toggleClipboardPin(clip) },
                        onDelete = { viewModel.deleteClipboardItem(clip.id) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var snippetText by remember { mutableStateOf("") }
        var pinByDefault by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Save Quick Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = snippetText,
                        onValueChange = { snippetText = it },
                        label = { Text("Type or paste something you use often") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Keep pinned at the top")
                        Switch(checked = pinByDefault, onCheckedChange = { pinByDefault = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addClipboardSnippet(snippetText, pinByDefault)
                        showAddDialog = false
                    },
                    enabled = snippetText.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ClipboardVaultCard(
    item: ClipboardItemEntity,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    if (item.isPinned) {
                        Text(
                            text = "PINNED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (item.isPinned) "Unpin" else "Pin",
                    tint = if (item.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun CustomStickersTab(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    val packs by viewModel.stickerPacks.collectAsStateWithLifecycle()
    val allStickers by viewModel.allStickers.collectAsStateWithLifecycle()

    var showCreatePackDialog by remember { mutableStateOf(false) }
    var emojiTargetPackId by remember { mutableStateOf<Long?>(null) }
    var activeImportPackId by remember { mutableStateOf<Long?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val packId = activeImportPackId
        if (uri != null && packId != null) {
            viewModel.importSticker(packId, uri, "Custom Sticker")
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Your Sticker Packs (${packs.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = { showCreatePackDialog = true },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.testTag("vault_new_sticker_pack_btn"),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Pack")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(packs, key = { it.id }) { pack ->
                val packStickers = remember(allStickers, pack.id) {
                    allStickers.filter { it.packId == pack.id }
                }
                StickerPackCard(
                    pack = pack,
                    stickers = packStickers,
                    viewModel = viewModel,
                    onImportImage = {
                        activeImportPackId = pack.id
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onAddEmojiSticker = { emojiTargetPackId = pack.id },
                    onDeletePack = { viewModel.deleteCustomPack(pack.id) },
                )
            }
        }
    }

    if (showCreatePackDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePackDialog = false },
            title = { Text("Create Sticker Pack") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Sticker Pack Name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Short description") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCustomStickerPack(name, desc)
                        showCreatePackDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePackDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (emojiTargetPackId != null) {
        var emoji by remember { mutableStateOf("🦊") }
        var label by remember { mutableStateOf("Foxy") }
        AlertDialog(
            onDismissRequest = { emojiTargetPackId = null },
            title = { Text("Create Emoji Sticker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Short name") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addEmojiSticker(emojiTargetPackId!!, emoji, label)
                        emojiTargetPackId = null
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { emojiTargetPackId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StickerPackCard(
    pack: StickerPackEntity,
    stickers: List<StickerEntity>,
    viewModel: StudioViewModel,
    onImportImage: () -> Unit,
    onAddEmojiSticker: () -> Unit,
    onDeletePack: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pack.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        pack.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!pack.isBuiltIn) {
                    IconButton(onClick = onDeletePack) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete pack")
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(stickers, key = { it.id }) { sticker ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            StickerThumb(
                                sticker = sticker,
                                stickerRepository = viewModel.stickerRepository,
                                modifier = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = sticker.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                            if (!pack.isBuiltIn || sticker.localFilePath != null) {
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable { viewModel.deleteSticker(sticker) },
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onImportImage,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Photo")
                }
                OutlinedButton(
                    onClick = onAddEmojiSticker,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Emoji")
                }
            }
        }
    }
}
