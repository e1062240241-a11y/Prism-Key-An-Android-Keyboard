package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.OneHandedMode
import com.example.ui.StudioViewModel
import com.example.ui.theme.KeyboardKeyShape
import com.example.ui.theme.KeyboardThemePreset
import com.example.ui.theme.TrailColorPreset
import com.example.ui.theme.resolveKeyboardPalette

@Composable
fun ThemesScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    val settings = viewModel.settings.collectAsStateWithLifecycle().value
    val previewPalette = resolveKeyboardPalette(settings.themePreset, MaterialTheme.colorScheme)

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Colors & Size",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Pick a clean Material look and size that feels comfortable in your hands",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Live Mini Gboard Preview Card
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = previewPalette.boardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, previewPalette.divider, RoundedCornerShape(28.dp)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(settings.keyboardWidthScale)
                            .align(Alignment.CenterHorizontally)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = settings.themePreset.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = previewPalette.keyText,
                            )
                            Surface(
                                shape = CircleShape,
                                color = previewPalette.accentContainer,
                            ) {
                                Text(
                                    text = "${(settings.keyboardHeightScale * 100).toInt()}% · ${settings.keyShape.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = previewPalette.onAccentContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        val keyShape = when (settings.keyShape) {
                            KeyboardKeyShape.PILL -> RoundedCornerShape(999.dp)
                            KeyboardKeyShape.BORDERLESS -> RoundedCornerShape(8.dp)
                            KeyboardKeyShape.ROUNDED -> RoundedCornerShape(settings.keyCornerRadiusDp.dp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEach { k ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((40f * settings.keyboardHeightScale).dp)
                                        .clip(keyShape)
                                        .background(
                                            if (settings.keyShape == KeyboardKeyShape.BORDERLESS) {
                                                Color.Transparent
                                            } else {
                                                previewPalette.keyBackground
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = k,
                                        color = previewPalette.keyText,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height((38f * settings.keyboardHeightScale).dp)
                                    .clip(keyShape)
                                    .background(previewPalette.specialKeyBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("?123", color = previewPalette.specialKeyText, style = MaterialTheme.typography.labelSmall)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(3.8f)
                                    .height((38f * settings.keyboardHeightScale).dp)
                                    .clip(keyShape)
                                    .background(previewPalette.keyBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("English", color = previewPalette.keySubText, style = MaterialTheme.typography.labelSmall)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height((38f * settings.keyboardHeightScale).dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(previewPalette.actionKeyBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Go", color = previewPalette.actionKeyText, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Material Color Palettes Only
            item {
                Text(
                    text = "Material Color Themes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(KeyboardThemePreset.entries) { preset ->
                        val p = resolveKeyboardPalette(preset, MaterialTheme.colorScheme)
                        val selected = settings.themePreset == preset
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = p.boardBackground,
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else p.divider,
                                    shape = RoundedCornerShape(22.dp),
                                )
                                .clickable { viewModel.setThemePreset(preset) }
                                .testTag("theme_preset_${preset.name}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(16.dp).clip(CircleShape).background(p.actionKeyBackground))
                                        Box(Modifier.size(16.dp).clip(CircleShape).background(p.specialKeyBackground))
                                        Box(Modifier.size(16.dp).clip(CircleShape).background(p.keyBackground))
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = p.actionKeyBackground,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = p.keyText,
                                )
                                Text(
                                    text = preset.tagline,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.keySubText,
                                )
                            }
                        }
                    }
                }
            }

            // Resizable Keyboard Dimensions Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.Height, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Keyboard Size & Fit", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetKeyboardSize() },
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Filled.RestartAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset")
                            }
                        }

                        // Height Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                "Short" to 0.80f,
                                "Normal" to 1.00f,
                                "Tall" to 1.16f,
                                "Extra Tall" to 1.32f,
                            ).forEach { (label, scale) ->
                                val active = kotlin.math.abs(settings.keyboardHeightScale - scale) < 0.04f
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { viewModel.setKeyboardHeightScale(scale) },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Height", style = MaterialTheme.typography.bodyMedium)
                                Text("${(settings.keyboardHeightScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = settings.keyboardHeightScale,
                                onValueChange = { viewModel.setKeyboardHeightScale(it) },
                                valueRange = 0.75f..1.35f,
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Width", style = MaterialTheme.typography.bodyMedium)
                                Text("${(settings.keyboardWidthScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = settings.keyboardWidthScale,
                                onValueChange = { viewModel.setKeyboardWidthScale(it) },
                                valueRange = 0.82f..1.00f,
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Space Below Keyboard", style = MaterialTheme.typography.bodyMedium)
                                Text("${settings.keyboardBottomOffsetDp} dp", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = settings.keyboardBottomOffsetDp.toFloat(),
                                onValueChange = { viewModel.setKeyboardBottomOffsetDp(it.toInt()) },
                                valueRange = 0f..32f,
                            )
                        }
                    }
                }
            }

            // Key Borders & Glide Trail
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Key Style & Swipe Trail", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            KeyboardKeyShape.entries.forEach { shape ->
                                val selected = settings.keyShape == shape
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { viewModel.setKeyShape(shape) },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = shape.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Key Roundness", style = MaterialTheme.typography.bodyMedium)
                                Text("${settings.keyCornerRadiusDp} dp", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = settings.keyCornerRadiusDp.toFloat(),
                                onValueChange = { viewModel.setKeyCornerRadius(it.toInt()) },
                                valueRange = 4f..22f,
                            )
                        }

                        Text("Swipe Trail Color", style = MaterialTheme.typography.bodyMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TrailColorPreset.entries) { trail ->
                                val selected = settings.trailColorPreset == trail
                                val swatchColor = if (trail == TrailColorPreset.AUTO) {
                                    previewPalette.accent
                                } else {
                                    Color(trail.colorHex)
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { viewModel.setTrailColorPreset(trail) },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(Modifier.size(12.dp).clip(CircleShape).background(swatchColor))
                                        Text(trail.label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Layout & Studio Appearance Toggles
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Keyboard Feel & Layout", style = MaterialTheme.typography.titleMedium)

                        ThemeSwitchRow("Show Number Row (1–0)", settings.showNumberRow) {
                            viewModel.setShowNumberRow(it)
                        }
                        ThemeSwitchRow("Key Pop-up Bubbles", settings.keyPopups) {
                            viewModel.setKeyPopups(it)
                        }
                        ThemeSwitchRow("Gentle Vibration on Tap", settings.hapticFeedback) {
                            viewModel.setHapticFeedback(it)
                        }

                        Text("One-Handed Typing", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OneHandedMode.entries.forEach { mode ->
                                val selected = settings.oneHandedMode == mode
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { viewModel.setOneHandedMode(mode) },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        Text("App Light / Dark Mode", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("SYSTEM" to "Auto", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                                val selected = settings.appDarkModeOverride == key
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { viewModel.setAppDarkModeOverride(key) },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
}

@Composable
private fun ThemeSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
