package com.beatbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// =============================================================================
// BEATBOX TYPOGRAPHY SYSTEM
// =============================================================================
//
// CENTRALIZED FONT CONFIGURATION
//
// The primary font for the entire BeatBox application is Times New Roman.
//
// Android does not include Times New Roman as a system font. The closest
// available system font is FontFamily.Serif, which on most Android devices
// renders as a Times-like serif font (Noto Serif / Droid Serif).
//
// For EXACT Times New Roman rendering:
// 1. Obtain a properly licensed Times New Roman .ttf or .otf file
// 2. Place it in app/src/main/res/font/ as beatbox_serif_regular.ttf
//    and beatbox_serif_bold.ttf
// 3. Uncomment the FontFamily definitions below that load from resources
//
// To change the app-wide font in the future, modify ONLY this file.
// =============================================================================

// PRIMARY FONT FAMILY - Times New Roman (via Serif fallback)
//
// Option A: Using system serif (current implementation)
// This uses the device's serif font family, which is the closest available
// to Times New Roman on Android.
val BeatBoxFontFamily = FontFamily.Serif

// Option B: Using bundled font files (uncomment after adding .ttf files to res/font/)
// val BeatBoxFontFamily = FontFamily(
//     Font(R.font.beatbox_serif_regular, FontWeight.Normal),
//     Font(R.font.beatbox_serif_regular, FontWeight.Medium),
//     Font(R.font.beatbox_serif_bold, FontWeight.Bold),
//     Font(R.font.beatbox_serif_bold, FontWeight.ExtraBold),
// )

// Font sizes
object BeatBoxFontSizes {
    val displayLarge = 57.sp
    val displayMedium = 45.sp
    val displaySmall = 36.sp
    val headlineLarge = 32.sp
    val headlineMedium = 28.sp
    val headlineSmall = 24.sp
    val titleLarge = 22.sp
    val titleMedium = 18.sp
    val titleSmall = 16.sp
    val bodyLarge = 16.sp
    val bodyMedium = 14.sp
    val bodySmall = 12.sp
    val labelLarge = 14.sp
    val labelMedium = 12.sp
    val labelSmall = 11.sp
}

// =============================================================================
// TYPOGRAPHY DEFINITIONS
// All styles use BeatBoxFontFamily (Times New Roman / Serif)
// =============================================================================

val BeatBoxTypography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BeatBoxFontSizes.displayLarge,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BeatBoxFontSizes.displayMedium,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BeatBoxFontSizes.displaySmall,
        lineHeight = 44.sp,
    ),

    // Headline
    headlineLarge = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BeatBoxFontSizes.headlineLarge,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BeatBoxFontSizes.headlineMedium,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BeatBoxFontSizes.headlineSmall,
        lineHeight = 32.sp,
    ),

    // Title
    titleLarge = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BeatBoxFontSizes.titleLarge,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = BeatBoxFontSizes.titleMedium,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = BeatBoxFontSizes.titleSmall,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // Body
    bodyLarge = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BeatBoxFontSizes.bodyLarge,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BeatBoxFontSizes.bodyMedium,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BeatBoxFontSizes.bodySmall,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // Label
    labelLarge = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = BeatBoxFontSizes.labelLarge,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = BeatBoxFontSizes.labelMedium,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BeatBoxFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = BeatBoxFontSizes.labelSmall,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
