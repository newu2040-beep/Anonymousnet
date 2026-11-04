package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Anonymousnet Design System Tokens
val AnonPrimaryViolet = Color(0xFF5950F2)
val AnonPrimaryIndigo = Color(0xFF473ED8)
val AnonVioletLight = Color(0xFF837BFF)
val AnonVioletSoftSurface = Color(0xFFF4F3FF)
val AnonVioletPillBg = Color(0xFFECEBFF)

val AnonCanvasWhite = Color(0xFFFAF9FE)
val AnonCardWhite = Color(0xFFFFFFFF)
val AnonChipInactiveBg = Color(0xFFF1F1F7)

val AnonTextPrimary = Color(0xFF141322)
val AnonTextSecondary = Color(0xFF6B6A7E)
val AnonTextMuted = Color(0xFF9A98AE)
val AnonDivider = Color(0xFFECEBF5)

val AnonHeartRed = Color(0xFFEF4444)
val AnonStarGold = Color(0xFFF59E0B)
val AnonBadgeBlue = Color(0xFF4F46E5)

// Dark Theme Tokens
val AnonDarkCanvas = Color(0xFF0E0D18)
val AnonDarkCard = Color(0xFF191828)
val AnonDarkSecondarySurface = Color(0xFF232138)
val AnonDarkTextPrimary = Color(0xFFF5F4FF)
val AnonDarkTextSecondary = Color(0xFFA7A5C2)
val AnonDarkDivider = Color(0xFF2C2A44)

enum class AppColorTheme(
    val key: String,
    val displayName: String,
    val primary: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val softSurfaceLight: Color,
    val darkPrimary: Color
) {
    VIOLET(
        key = "violet",
        displayName = "Vivid Violet",
        primary = Color(0xFF5950F2),
        primaryDark = Color(0xFF473ED8),
        secondaryLight = Color(0xFF837BFF),
        softSurfaceLight = Color(0xFFF4F3FF),
        darkPrimary = Color(0xFF837BFF)
    ),
    OCEAN(
        key = "ocean",
        displayName = "Midnight Ocean",
        primary = Color(0xFF2563EB),
        primaryDark = Color(0xFF1D4ED8),
        secondaryLight = Color(0xFF60A5FA),
        softSurfaceLight = Color(0xFFEFF6FF),
        darkPrimary = Color(0xFF60A5FA)
    ),
    EMERALD(
        key = "emerald",
        displayName = "Emerald Mint",
        primary = Color(0xFF059669),
        primaryDark = Color(0xFF047857),
        secondaryLight = Color(0xFF34D399),
        softSurfaceLight = Color(0xFFECFDF5),
        darkPrimary = Color(0xFF34D399)
    ),
    ROSE(
        key = "rose",
        displayName = "Rose Sunset",
        primary = Color(0xFFE11D48),
        primaryDark = Color(0xFFBE123C),
        secondaryLight = Color(0xFFFB7185),
        softSurfaceLight = Color(0xFFFFF1F2),
        darkPrimary = Color(0xFFFB7185)
    ),
    AMBER(
        key = "amber",
        displayName = "Royal Amber",
        primary = Color(0xFFD97706),
        primaryDark = Color(0xFFB45309),
        secondaryLight = Color(0xFFFBBF24),
        softSurfaceLight = Color(0xFFFFFBEB),
        darkPrimary = Color(0xFFFBBF24)
    );

    companion object {
        fun fromKey(key: String?): AppColorTheme {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: VIOLET
        }
    }
}
