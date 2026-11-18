package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class KeyboardThemePreset(
    val displayName: String,
    val tagline: String,
    val isDark: Boolean,
) {
    DYNAMIC_YOU("Material You", "Matches your phone wallpaper", false),
    MATERIAL_LIGHT("Material Light", "Clean bright white & blue", false),
    MATERIAL_DARK("Material Dark", "Easy-on-the-eyes dark mode", true),
    MATERIAL_BLUE("Material Blue", "Fresh calm blue tones", false),
    MATERIAL_NAVY("Material Navy", "Deep evening navy blue", true),
    MATERIAL_TEAL("Material Teal", "Crisp mint & ocean teal", false),
    MATERIAL_GREEN("Material Green", "Soft natural sage green", false),
    MATERIAL_AMBER("Material Sand", "Warm cozy sand & amber", false),
    MATERIAL_ROSE("Material Rose", "Gentle soft rose pink", false),
    MATERIAL_MONOCHROME("Material Mono", "Clean high-contrast gray", true),
}

enum class KeyboardKeyShape(val label: String) {
    ROUNDED("Classic Keys"),
    PILL("Rounded Pills"),
    BORDERLESS("Clean Flat"),
}

enum class TrailColorPreset(val label: String, val colorHex: Long) {
    AUTO("Theme Color", 0x00000000L),
    BLUE("Ocean Blue", 0xFF1A73E8L),
    TEAL("Fresh Teal", 0xFF00796BL),
    INDIGO("Deep Indigo", 0xFF3949ABL),
    AMBER("Warm Amber", 0xFFE37400L),
}

data class KeyboardPalette(
    val boardBackground: Color,
    val boardBackgroundAccent: Color,
    val toolbarBackground: Color,
    val keyBackground: Color,
    val keyPressed: Color,
    val keyText: Color,
    val keySubText: Color,
    val specialKeyBackground: Color,
    val specialKeyText: Color,
    val actionKeyBackground: Color,
    val actionKeyText: Color,
    val accent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val keyShadow: Color,
    val divider: Color,
)

@Composable
fun resolveKeyboardPalette(
    preset: KeyboardThemePreset,
    colorScheme: ColorScheme,
): KeyboardPalette {
    return when (preset) {
        KeyboardThemePreset.DYNAMIC_YOU -> KeyboardPalette(
            boardBackground = colorScheme.surfaceContainer,
            boardBackgroundAccent = colorScheme.surfaceContainerHigh,
            toolbarBackground = colorScheme.surfaceContainer,
            keyBackground = colorScheme.surfaceContainerLowest,
            keyPressed = colorScheme.primaryContainer,
            keyText = colorScheme.onSurface,
            keySubText = colorScheme.onSurfaceVariant,
            specialKeyBackground = colorScheme.secondaryContainer,
            specialKeyText = colorScheme.onSecondaryContainer,
            actionKeyBackground = colorScheme.primary,
            actionKeyText = colorScheme.onPrimary,
            accent = colorScheme.primary,
            accentContainer = colorScheme.primaryContainer,
            onAccentContainer = colorScheme.onPrimaryContainer,
            keyShadow = Color(0x22000000),
            divider = colorScheme.outlineVariant,
        )

        KeyboardThemePreset.MATERIAL_LIGHT -> KeyboardPalette(
            boardBackground = Color(0xFFECEEF3),
            boardBackgroundAccent = Color(0xFFE2E5EC),
            toolbarBackground = Color(0xFFECEEF3),
            keyBackground = Color(0xFFFFFFFF),
            keyPressed = Color(0xFFD3E3FD),
            keyText = Color(0xFF1F1F1F),
            keySubText = Color(0xFF5E6368),
            specialKeyBackground = Color(0xFFD7DCE6),
            specialKeyText = Color(0xFF1F1F1F),
            actionKeyBackground = Color(0xFF0B57D0),
            actionKeyText = Color(0xFFFFFFFF),
            accent = Color(0xFF0B57D0),
            accentContainer = Color(0xFFD3E3FD),
            onAccentContainer = Color(0xFF041E49),
            keyShadow = Color(0x24000000),
            divider = Color(0x33747775),
        )

        KeyboardThemePreset.MATERIAL_DARK -> KeyboardPalette(
            boardBackground = Color(0xFF1B1C1F),
            boardBackgroundAccent = Color(0xFF25262A),
            toolbarBackground = Color(0xFF1B1C1F),
            keyBackground = Color(0xFF33353A),
            keyPressed = Color(0xFF474A52),
            keyText = Color(0xFFE3E3E3),
            keySubText = Color(0xFF9AA0A6),
            specialKeyBackground = Color(0xFF27292E),
            specialKeyText = Color(0xFFC4C7C5),
            actionKeyBackground = Color(0xFFA8C7FA),
            actionKeyText = Color(0xFF062E6F),
            accent = Color(0xFFA8C7FA),
            accentContainer = Color(0xFF0842A0),
            onAccentContainer = Color(0xFFD3E3FD),
            keyShadow = Color(0x55000000),
            divider = Color(0x338E918F),
        )

        KeyboardThemePreset.MATERIAL_BLUE -> KeyboardPalette(
            boardBackground = Color(0xFFE8EEF8),
            boardBackgroundAccent = Color(0xFFDCE5F4),
            toolbarBackground = Color(0xFFE8EEF8),
            keyBackground = Color(0xFFFCFDFF),
            keyPressed = Color(0xFFC2D7FF),
            keyText = Color(0xFF171D26),
            keySubText = Color(0xFF535E70),
            specialKeyBackground = Color(0xFFD1DDF0),
            specialKeyText = Color(0xFF102A4C),
            actionKeyBackground = Color(0xFF1A73E8),
            actionKeyText = Color(0xFFFFFFFF),
            accent = Color(0xFF1A73E8),
            accentContainer = Color(0xFFD2E3FC),
            onAccentContainer = Color(0xFF062E6F),
            keyShadow = Color(0x220F2848),
            divider = Color(0x30535E70),
        )

        KeyboardThemePreset.MATERIAL_NAVY -> KeyboardPalette(
            boardBackground = Color(0xFF131924),
            boardBackgroundAccent = Color(0xFF1B2332),
            toolbarBackground = Color(0xFF131924),
            keyBackground = Color(0xFF242E40),
            keyPressed = Color(0xFF33425C),
            keyText = Color(0xFFE2E8F5),
            keySubText = Color(0xFF96A3BA),
            specialKeyBackground = Color(0xFF1B2333),
            specialKeyText = Color(0xFFBAC8E2),
            actionKeyBackground = Color(0xFF8AB4F8),
            actionKeyText = Color(0xFF08224A),
            accent = Color(0xFF8AB4F8),
            accentContainer = Color(0xFF1E3A66),
            onAccentContainer = Color(0xFFD2E3FC),
            keyShadow = Color(0x55000000),
            divider = Color(0x3096A3BA),
        )

        KeyboardThemePreset.MATERIAL_TEAL -> KeyboardPalette(
            boardBackground = Color(0xFFE6F2F0),
            boardBackgroundAccent = Color(0xFFD9EAE7),
            toolbarBackground = Color(0xFFE6F2F0),
            keyBackground = Color(0xFFFBFEFD),
            keyPressed = Color(0xFFBBE5DF),
            keyText = Color(0xFF122321),
            keySubText = Color(0xFF496460),
            specialKeyBackground = Color(0xFFCDE2DF),
            specialKeyText = Color(0xFF0A332E),
            actionKeyBackground = Color(0xFF006A60),
            actionKeyText = Color(0xFFFFFFFF),
            accent = Color(0xFF006A60),
            accentContainer = Color(0xFFA2F2E6),
            onAccentContainer = Color(0xFF00201C),
            keyShadow = Color(0x20052E29),
            divider = Color(0x30496460),
        )

        KeyboardThemePreset.MATERIAL_GREEN -> KeyboardPalette(
            boardBackground = Color(0xFFEBF2E8),
            boardBackgroundAccent = Color(0xFFDFEADA),
            toolbarBackground = Color(0xFFEBF2E8),
            keyBackground = Color(0xFFFCFEFA),
            keyPressed = Color(0xFFC6E5BF),
            keyText = Color(0xFF172216),
            keySubText = Color(0xFF51634E),
            specialKeyBackground = Color(0xFFD2E2CE),
            specialKeyText = Color(0xFF143511),
            actionKeyBackground = Color(0xFF2E6B27),
            actionKeyText = Color(0xFFFFFFFF),
            accent = Color(0xFF2E6B27),
            accentContainer = Color(0xFFB4F3A8),
            onAccentContainer = Color(0xFF032100),
            keyShadow = Color(0x20102A0D),
            divider = Color(0x3051634E),
        )

        KeyboardThemePreset.MATERIAL_AMBER -> KeyboardPalette(
            boardBackground = Color(0xFFF5EFE6),
            boardBackgroundAccent = Color(0xFFECE2D4),
            toolbarBackground = Color(0xFFF5EFE6),
            keyBackground = Color(0xFFFFFDF9),
            keyPressed = Color(0xFFFADBB0),
            keyText = Color(0xFF231B11),
            keySubText = Color(0xFF6A5B49),
            specialKeyBackground = Color(0xFFE7DAC8),
            specialKeyText = Color(0xFF3E290F),
            actionKeyBackground = Color(0xFF8C5000),
            actionKeyText = Color(0xFFFFFFFF),
            accent = Color(0xFF8C5000),
            accentContainer = Color(0xFFFFDCBA),
            onAccentContainer = Color(0xFF2D1600),
            keyShadow = Color(0x20352208),
            divider = Color(0x306A5B49),
        )

        KeyboardThemePreset.MATERIAL_ROSE -> KeyboardPalette(
            boardBackground = Color(0xFFF6ECEF),
            boardBackgroundAccent = Color(0xFFEDE0E4),
            toolbarBackground = Color(0xFFF6ECEF),
            keyBackground = Color(0xFFFFFBFC),
            keyPressed = Color(0xFFFAD2DF),
            keyText = Color(0xFF25181C),
            keySubText = Color(0xFF6B565D),
            specialKeyBackground = Color(0xFFE9D5DC),
            specialKeyText = Color(0xFF3E1826),
            actionKeyBackground = Color(0xFF9A3E5D),
            actionKeyText = Color(0xFFFFFFFF),
            accent = Color(0xFF9A3E5D),
            accentContainer = Color(0xFFFFD9E3),
            onAccentContainer = Color(0xFF3E001A),
            keyShadow = Color(0x2038101E),
            divider = Color(0x306B565D),
        )

        KeyboardThemePreset.MATERIAL_MONOCHROME -> KeyboardPalette(
            boardBackground = Color(0xFF18181A),
            boardBackgroundAccent = Color(0xFF222225),
            toolbarBackground = Color(0xFF18181A),
            keyBackground = Color(0xFF303034),
            keyPressed = Color(0xFF47474D),
            keyText = Color(0xFFF2F2F4),
            keySubText = Color(0xFFA2A2AA),
            specialKeyBackground = Color(0xFF242428),
            specialKeyText = Color(0xFFD4D4DA),
            actionKeyBackground = Color(0xFFE2E2E6),
            actionKeyText = Color(0xFF18181A),
            accent = Color(0xFFD6D6DC),
            accentContainer = Color(0xFF3B3B40),
            onAccentContainer = Color(0xFFFFFFFF),
            keyShadow = Color(0x55000000),
            divider = Color(0x30A2A2AA),
        )
    }
}
