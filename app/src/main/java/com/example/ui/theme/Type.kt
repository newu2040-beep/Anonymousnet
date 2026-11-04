package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val RobotoFontFamily = FontFamily(
    Font(R.font.roboto, FontWeight.Normal),
    Font(R.font.roboto, FontWeight.Medium),
    Font(R.font.roboto, FontWeight.SemiBold),
    Font(R.font.roboto, FontWeight.Bold)
)

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
)

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold)
)

val NunitoSansFontFamily = FontFamily(
    Font(R.font.nunito_sans, FontWeight.Normal),
    Font(R.font.nunito_sans, FontWeight.Medium),
    Font(R.font.nunito_sans, FontWeight.SemiBold),
    Font(R.font.nunito_sans, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

enum class AppFontOption(
    val key: String,
    val displayName: String,
    val fontFamily: FontFamily
) {
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta", PlusJakartaSansFontFamily),
    INTER("inter", "Inter", InterFontFamily),
    ROBOTO("roboto", "Roboto", RobotoFontFamily),
    POPPINS("poppins", "Poppins", PoppinsFontFamily),
    MANROPE("manrope", "Manrope", ManropeFontFamily),
    NUNITO_SANS("nunito_sans", "Nunito Sans", NunitoSansFontFamily),
    SYSTEM_DEFAULT("system_default", "System Default", FontFamily.Default);

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

val Typography = buildAppTypography()
