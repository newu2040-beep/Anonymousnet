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
    val darkPrimary: Color,
    val previewColor: Color
) {
    CUSTOM_ACCENT(
        key = "custom_accent",
        displayName = "Custom Accent",
        primary = Color(0xFF5950F2),
        primaryDark = Color(0xFF473ED8),
        secondaryLight = Color(0xFF837BFF),
        softSurfaceLight = Color(0xFFF4F3FF),
        darkPrimary = Color(0xFF837BFF),
        previewColor = Color(0xFF5950F2)
    ),
    PURE_WHITE(
        key = "pure_white",
        displayName = "Pure White",
        primary = Color(0xFF18181B),
        primaryDark = Color(0xFF09090B),
        secondaryLight = Color(0xFF71717A),
        softSurfaceLight = Color(0xFFF4F4F5),
        darkPrimary = Color(0xFFE4E4E7),
        previewColor = Color(0xFFF4F4F5)
    ),
    SOFT_CREAM(
        key = "soft_cream",
        displayName = "Soft Cream",
        primary = Color(0xFF78593E),
        primaryDark = Color(0xFF5D4037),
        secondaryLight = Color(0xFFA07855),
        softSurfaceLight = Color(0xFFFAF6EE),
        darkPrimary = Color(0xFFD7CCC8),
        previewColor = Color(0xFFF5EBE1)
    ),
    MINIMAL_GRAY(
        key = "minimal_gray",
        displayName = "Minimal Gray",
        primary = Color(0xFF475569),
        primaryDark = Color(0xFF334155),
        secondaryLight = Color(0xFF94A3B8),
        softSurfaceLight = Color(0xFFF1F5F9),
        darkPrimary = Color(0xFFCBD5E1),
        previewColor = Color(0xFF64748B)
    ),
    MIDNIGHT_BLACK(
        key = "midnight_black",
        displayName = "Midnight Black",
        primary = Color(0xFF0F172A),
        primaryDark = Color(0xFF020617),
        secondaryLight = Color(0xFF334155),
        softSurfaceLight = Color(0xFFF1F5F9),
        darkPrimary = Color(0xFF94A3B8),
        previewColor = Color(0xFF0B0F19)
    ),
    OCEAN_BLUE(
        key = "ocean_blue",
        displayName = "Ocean Blue",
        primary = Color(0xFF0284C7),
        primaryDark = Color(0xFF0369A1),
        secondaryLight = Color(0xFF38BDF8),
        softSurfaceLight = Color(0xFFF0F9FF),
        darkPrimary = Color(0xFF38BDF8),
        previewColor = Color(0xFF0284C7)
    ),
    MINT_GREEN(
        key = "mint_green",
        displayName = "Mint Green",
        primary = Color(0xFF059669),
        primaryDark = Color(0xFF047857),
        secondaryLight = Color(0xFF34D399),
        softSurfaceLight = Color(0xFFECFDF5),
        darkPrimary = Color(0xFF34D399),
        previewColor = Color(0xFF10B981)
    ),
    LAVENDER(
        key = "lavender",
        displayName = "Lavender",
        primary = Color(0xFF7C3AED),
        primaryDark = Color(0xFF6D28D9),
        secondaryLight = Color(0xFFA78BFA),
        softSurfaceLight = Color(0xFFF5F3FF),
        darkPrimary = Color(0xFFA78BFA),
        previewColor = Color(0xFF8B5CF6)
    ),
    ROSE_PINK(
        key = "rose_pink",
        displayName = "Rose Pink",
        primary = Color(0xFFE11D48),
        primaryDark = Color(0xFFBE123C),
        secondaryLight = Color(0xFFFB7185),
        softSurfaceLight = Color(0xFFFFF1F2),
        darkPrimary = Color(0xFFFB7185),
        previewColor = Color(0xFFF43F5E)
    ),
    WARM_SAND(
        key = "warm_sand",
        displayName = "Warm Sand",
        primary = Color(0xFFD97706),
        primaryDark = Color(0xFFB45309),
        secondaryLight = Color(0xFFFBBF24),
        softSurfaceLight = Color(0xFFFFFBEB),
        darkPrimary = Color(0xFFFBBF24),
        previewColor = Color(0xFFF59E0B)
    );

    companion object {
        fun fromKey(key: String?): AppColorTheme {
            return when (key?.lowercase()) {
                "custom_accent", "violet" -> CUSTOM_ACCENT
                "pure_white" -> PURE_WHITE
                "soft_cream" -> SOFT_CREAM
                "minimal_gray" -> MINIMAL_GRAY
                "midnight_black" -> MIDNIGHT_BLACK
                "ocean_blue", "ocean" -> OCEAN_BLUE
                "mint_green", "emerald" -> MINT_GREEN
                "lavender" -> LAVENDER
                "rose_pink", "rose" -> ROSE_PINK
                "warm_sand", "amber" -> WARM_SAND
                else -> CUSTOM_ACCENT
            }
        }
    }
}
