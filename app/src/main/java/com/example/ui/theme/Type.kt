package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

private fun safeResourceFontFamily(resId: Int): FontFamily = try {
    FontFamily(
        Font(resId, FontWeight.Normal),
        Font(resId, FontWeight.Medium),
        Font(resId, FontWeight.SemiBold),
        Font(resId, FontWeight.Bold)
    )
} catch (_: Throwable) {
    FontFamily.Default
}

enum class AppFontOption(
    val key: String,
    val displayName: String,
    private val fontResId: Int?
) {
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta", R.font.plus_jakarta_sans),
    INTER("inter", "Inter", R.font.inter),
    ROBOTO("roboto", "Roboto", R.font.roboto),
    POPPINS("poppins", "Poppins", R.font.poppins),
    MANROPE("manrope", "Manrope", R.font.manrope),
    NUNITO_SANS("nunito_sans", "Nunito Sans", R.font.nunito_sans),
    SYSTEM_DEFAULT("system_default", "System Default", null);

    val fontFamily: FontFamily by lazy {
        if (fontResId != null) {
            safeResourceFontFamily(fontResId)
        } else {
            FontFamily.Default
        }
    }

    companion object {
        fun fromKey(key: String?): AppFontOption {
            return when (key?.lowercase()) {
                "inter" -> INTER
                "roboto" -> ROBOTO
                "poppins" -> POPPINS
                "manrope" -> MANROPE
                "nunito_sans" -> NUNITO_SANS
                "system_default", "system" -> SYSTEM_DEFAULT
                "plus_jakarta_sans", "plus_jakarta" -> PLUS_JAKARTA_SANS
                else -> PLUS_JAKARTA_SANS
            }
        }
    }
}

val InterFontFamily: FontFamily get() = AppFontOption.INTER.fontFamily
val RobotoFontFamily: FontFamily get() = AppFontOption.ROBOTO.fontFamily
val PoppinsFontFamily: FontFamily get() = AppFontOption.POPPINS.fontFamily
val ManropeFontFamily: FontFamily get() = AppFontOption.MANROPE.fontFamily
val NunitoSansFontFamily: FontFamily get() = AppFontOption.NUNITO_SANS.fontFamily
val PlusJakartaSansFontFamily: FontFamily get() = AppFontOption.PLUS_JAKARTA_SANS.fontFamily

fun buildAppTypography(fontOption: AppFontOption = AppFontOption.PLUS_JAKARTA_SANS): Typography {
    val activeFont = fontOption.fontFamily
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.3).sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}

val Typography: Typography by lazy { buildAppTypography() }

