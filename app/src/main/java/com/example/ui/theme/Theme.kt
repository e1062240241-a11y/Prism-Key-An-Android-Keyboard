package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PrismPrimaryDark,
    onPrimary = PrismOnPrimaryDark,
    primaryContainer = PrismPrimaryContainerDark,
    onPrimaryContainer = PrismOnPrimaryContainerDark,
    secondary = PrismSecondaryDark,
    onSecondary = PrismOnSecondaryDark,
    secondaryContainer = PrismSecondaryContainerDark,
    onSecondaryContainer = PrismOnSecondaryContainerDark,
    tertiary = PrismTertiaryDark,
    onTertiary = PrismOnTertiaryDark,
    tertiaryContainer = PrismTertiaryContainerDark,
    onTertiaryContainer = PrismOnTertiaryContainerDark,
    background = PrismBackgroundDark,
    onBackground = PrismOnSurfaceDark,
    surface = PrismSurfaceDark,
    onSurface = PrismOnSurfaceDark,
    surfaceVariant = PrismSurfaceVariantDark,
    onSurfaceVariant = PrismOnSurfaceVariantDark,
    outline = PrismOutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrismPrimaryLight,
    onPrimary = PrismOnPrimaryLight,
    primaryContainer = PrismPrimaryContainerLight,
    onPrimaryContainer = PrismOnPrimaryContainerLight,
    secondary = PrismSecondaryLight,
    onSecondary = PrismOnSecondaryLight,
    secondaryContainer = PrismSecondaryContainerLight,
    onSecondaryContainer = PrismOnSecondaryContainerLight,
    tertiary = PrismTertiaryLight,
    onTertiary = PrismOnTertiaryLight,
    tertiaryContainer = PrismTertiaryContainerLight,
    onTertiaryContainer = PrismOnTertiaryContainerLight,
    background = PrismBackgroundLight,
    onBackground = PrismOnSurfaceLight,
    surface = PrismSurfaceLight,
    onSurface = PrismOnSurfaceLight,
    surfaceVariant = PrismSurfaceVariantLight,
    onSurfaceVariant = PrismOnSurfaceVariantLight,
    outline = PrismOutlineLight,
)

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PrismKeysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = ExpressiveShapes,
        content = content,
    )
}
