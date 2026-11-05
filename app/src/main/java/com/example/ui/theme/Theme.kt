package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

val LocalCompactMode = compositionLocalOf { false }

val AnonymousnetShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AnonymousnetTheme(
    darkTheme: Boolean = false,
    colorTheme: AppColorTheme = AppColorTheme.CUSTOM_ACCENT,
    fontOption: AppFontOption = AppFontOption.PLUS_JAKARTA_SANS,
    useSystemFont: Boolean = false,
    compactMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    // Automatically adapt for small phone displays (< 360dp width) or when user explicitly enables Compact Mode
    val isSmallScreen = configuration.screenWidthDp in 1..359
    val effectiveCompact = compactMode || isSmallScreen

    val scaledDensity = remember(baseDensity, effectiveCompact) {
        if (effectiveCompact) {
            Density(
                density = baseDensity.density * 0.87f,
                fontScale = baseDensity.fontScale * 0.89f
            )
        } else {
            baseDensity
        }
    }

    val colorScheme = remember(darkTheme, colorTheme) {
        if (darkTheme) {
            darkColorScheme(
                primary = colorTheme.primary,
                onPrimary = Color.White,
                primaryContainer = AnonDarkSecondarySurface,
                onPrimaryContainer = AnonDarkTextPrimary,
                secondary = colorTheme.darkPrimary,
                onSecondary = Color.White,
                secondaryContainer = AnonDarkSecondarySurface,
                onSecondaryContainer = AnonDarkTextPrimary,
                background = AnonDarkCanvas,
                onBackground = AnonDarkTextPrimary,
                surface = AnonDarkCard,
                onSurface = AnonDarkTextPrimary,
                surfaceVariant = AnonDarkSecondarySurface,
                onSurfaceVariant = AnonDarkTextSecondary,
                outline = AnonDarkDivider,
                error = AnonHeartRed
            )
        } else {
            lightColorScheme(
                primary = colorTheme.primary,
                onPrimary = Color.White,
                primaryContainer = colorTheme.softSurfaceLight,
                onPrimaryContainer = colorTheme.primaryDark,
                secondary = colorTheme.secondaryLight,
                onSecondary = Color.White,
                secondaryContainer = AnonChipInactiveBg,
                onSecondaryContainer = AnonTextPrimary,
                background = AnonCanvasWhite,
                onBackground = AnonTextPrimary,
                surface = AnonCardWhite,
                onSurface = AnonTextPrimary,
                surfaceVariant = colorTheme.softSurfaceLight,
                onSurfaceVariant = AnonTextSecondary,
                outline = AnonDivider,
                error = AnonHeartRed
            )
        }
    }

    val effectiveFont = remember(fontOption, useSystemFont) {
        if (useSystemFont) AppFontOption.SYSTEM_DEFAULT else fontOption
    }

    val typography = remember(effectiveFont) {
        buildAppTypography(effectiveFont)
    }

    CompositionLocalProvider(
        LocalCompactMode provides effectiveCompact,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AnonymousnetShapes,
            content = content
        )
    }
}
