package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.KeyboardKeyShape
import com.example.ui.theme.KeyboardThemePreset
import com.example.ui.theme.TrailColorPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keyboardPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "prismkeys_preferences",
)

enum class OneHandedMode(val label: String) {
    NONE("Full"),
    LEFT("Left"),
    RIGHT("Right"),
}

data class KeyboardSettings(
    val themePreset: KeyboardThemePreset = KeyboardThemePreset.DYNAMIC_YOU,
    val keyShape: KeyboardKeyShape = KeyboardKeyShape.ROUNDED,
    val keyCornerRadiusDp: Int = 8,
    val keyboardHeightScale: Float = 1.0f,
    val keyboardWidthScale: Float = 1.0f,
    val keyboardBottomOffsetDp: Int = 0,
    val showNumberRow: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keyPopups: Boolean = true,
    val gestureTypingEnabled: Boolean = true,
    val trailColorPreset: TrailColorPreset = TrailColorPreset.AUTO,
    val autocorrectEnabled: Boolean = true,
    val suggestionsEnabled: Boolean = true,
    val autoCapitalize: Boolean = true,
    val oneHandedMode: OneHandedMode = OneHandedMode.NONE,
    val sourceLangCode: String = "en",
    val targetLangCode: String = "es",
    val strictOfflineMode: Boolean = true,
    val clipboardAutoCapture: Boolean = true,
    val appDarkModeOverride: String = "SYSTEM", // SYSTEM, LIGHT, DARK
)

class KeyboardPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.keyboardPrefsDataStore

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val KEY_SHAPE = stringPreferencesKey("key_shape")
        val KEY_CORNER_RADIUS = intPreferencesKey("key_corner_radius")
        val KEYBOARD_HEIGHT_SCALE = floatPreferencesKey("keyboard_height_scale")
        val KEYBOARD_WIDTH_SCALE = floatPreferencesKey("keyboard_width_scale")
        val KEYBOARD_BOTTOM_OFFSET = intPreferencesKey("keyboard_bottom_offset")
        val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEY_POPUPS = booleanPreferencesKey("key_popups")
        val GESTURE_TYPING = booleanPreferencesKey("gesture_typing")
        val TRAIL_COLOR = stringPreferencesKey("trail_color")
        val AUTOCORRECT = booleanPreferencesKey("autocorrect")
        val SUGGESTIONS = booleanPreferencesKey("suggestions")
        val AUTO_CAPITALIZE = booleanPreferencesKey("auto_capitalize")
        val ONE_HANDED_MODE = stringPreferencesKey("one_handed_mode")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val STRICT_OFFLINE = booleanPreferencesKey("strict_offline")
        val CLIPBOARD_CAPTURE = booleanPreferencesKey("clipboard_capture")
        val APP_DARK_MODE = stringPreferencesKey("app_dark_mode")
    }

    val settingsFlow: Flow<KeyboardSettings> = dataStore.data.map { prefs ->
        KeyboardSettings(
            themePreset = prefs[Keys.THEME_PRESET]?.let { name ->
                KeyboardThemePreset.entries.find { it.name == name }
            } ?: KeyboardThemePreset.DYNAMIC_YOU,
            keyShape = prefs[Keys.KEY_SHAPE]?.let { name ->
                KeyboardKeyShape.entries.find { it.name == name }
                    ?: if (name == "SQUIRCLE") KeyboardKeyShape.ROUNDED else null
            } ?: KeyboardKeyShape.ROUNDED,
            keyCornerRadiusDp = (prefs[Keys.KEY_CORNER_RADIUS] ?: 8).coerceIn(4, 22),
            keyboardHeightScale = (prefs[Keys.KEYBOARD_HEIGHT_SCALE] ?: 1.0f).coerceIn(0.75f, 1.35f),
            keyboardWidthScale = (prefs[Keys.KEYBOARD_WIDTH_SCALE] ?: 1.0f).coerceIn(0.82f, 1.0f),
            keyboardBottomOffsetDp = (prefs[Keys.KEYBOARD_BOTTOM_OFFSET] ?: 0).coerceIn(0, 32),
            showNumberRow = prefs[Keys.SHOW_NUMBER_ROW] ?: false,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            keyPopups = prefs[Keys.KEY_POPUPS] ?: true,
            gestureTypingEnabled = prefs[Keys.GESTURE_TYPING] ?: true,
            trailColorPreset = prefs[Keys.TRAIL_COLOR]?.let { name ->
                TrailColorPreset.entries.find { it.name == name }
            } ?: TrailColorPreset.AUTO,
            autocorrectEnabled = prefs[Keys.AUTOCORRECT] ?: true,
            suggestionsEnabled = prefs[Keys.SUGGESTIONS] ?: true,
            autoCapitalize = prefs[Keys.AUTO_CAPITALIZE] ?: true,
            oneHandedMode = prefs[Keys.ONE_HANDED_MODE]?.let { name ->
                OneHandedMode.entries.find { it.name == name }
            } ?: OneHandedMode.NONE,
            sourceLangCode = prefs[Keys.SOURCE_LANG] ?: "en",
            targetLangCode = prefs[Keys.TARGET_LANG] ?: "es",
            strictOfflineMode = prefs[Keys.STRICT_OFFLINE] ?: true,
            clipboardAutoCapture = prefs[Keys.CLIPBOARD_CAPTURE] ?: true,
            appDarkModeOverride = prefs[Keys.APP_DARK_MODE] ?: "SYSTEM",
        )
    }

    suspend fun setThemePreset(preset: KeyboardThemePreset) {
        dataStore.edit { it[Keys.THEME_PRESET] = preset.name }
    }

    suspend fun setKeyShape(shape: KeyboardKeyShape) {
        dataStore.edit { it[Keys.KEY_SHAPE] = shape.name }
    }

    suspend fun setKeyCornerRadius(radiusDp: Int) {
        dataStore.edit { it[Keys.KEY_CORNER_RADIUS] = radiusDp.coerceIn(4, 22) }
    }

    suspend fun setKeyboardHeightScale(scale: Float) {
        dataStore.edit { it[Keys.KEYBOARD_HEIGHT_SCALE] = scale.coerceIn(0.75f, 1.35f) }
    }

    suspend fun setKeyboardWidthScale(scale: Float) {
        dataStore.edit { it[Keys.KEYBOARD_WIDTH_SCALE] = scale.coerceIn(0.82f, 1.0f) }
    }

    suspend fun setKeyboardBottomOffsetDp(offsetDp: Int) {
        dataStore.edit { it[Keys.KEYBOARD_BOTTOM_OFFSET] = offsetDp.coerceIn(0, 32) }
    }

    suspend fun resetKeyboardSize() {
        dataStore.edit {
            it[Keys.KEYBOARD_HEIGHT_SCALE] = 1.0f
            it[Keys.KEYBOARD_WIDTH_SCALE] = 1.0f
            it[Keys.KEYBOARD_BOTTOM_OFFSET] = 0
        }
    }

    suspend fun setShowNumberRow(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_NUMBER_ROW] = show }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setKeyPopups(enabled: Boolean) {
        dataStore.edit { it[Keys.KEY_POPUPS] = enabled }
    }

    suspend fun setGestureTyping(enabled: Boolean) {
        dataStore.edit { it[Keys.GESTURE_TYPING] = enabled }
    }

    suspend fun setTrailColorPreset(preset: TrailColorPreset) {
        dataStore.edit { it[Keys.TRAIL_COLOR] = preset.name }
    }

    suspend fun setAutocorrect(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTOCORRECT] = enabled }
    }

    suspend fun setSuggestions(enabled: Boolean) {
        dataStore.edit { it[Keys.SUGGESTIONS] = enabled }
    }

    suspend fun setAutoCapitalize(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_CAPITALIZE] = enabled }
    }

    suspend fun setOneHandedMode(mode: OneHandedMode) {
        dataStore.edit { it[Keys.ONE_HANDED_MODE] = mode.name }
    }

    suspend fun setLanguages(sourceCode: String, targetCode: String) {
        dataStore.edit {
            it[Keys.SOURCE_LANG] = sourceCode
            it[Keys.TARGET_LANG] = targetCode
        }
    }

    suspend fun setStrictOfflineMode(enabled: Boolean) {
        dataStore.edit { it[Keys.STRICT_OFFLINE] = enabled }
    }

    suspend fun setClipboardAutoCapture(enabled: Boolean) {
        dataStore.edit { it[Keys.CLIPBOARD_CAPTURE] = enabled }
    }

    suspend fun setAppDarkModeOverride(mode: String) {
        dataStore.edit { it[Keys.APP_DARK_MODE] = mode }
    }
}
