package com.eficazautomotriz.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, line: Int, weight: FontWeight, spacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
)

/**
 * System font (Roboto) at the mockup scale (1140 px PNG = 3x, i.e. px / 3 = dp). Raw mockup
 * sizes are header title ~16sp, card titles ~12sp, body ~10.5sp; these keep the same
 * hierarchy one step above them: header title 18sp, card titles 14sp, body 12sp,
 * secondary 11sp, uppercase section labels 10sp semibold letter-spaced.
 */
val EficazTypography = Typography(
    displaySmall = style(22, 28, FontWeight.Bold),
    headlineMedium = style(20, 24, FontWeight.Bold),
    headlineSmall = style(18, 24, FontWeight.SemiBold),
    titleLarge = style(18, 23, FontWeight.SemiBold),
    titleMedium = style(14, 19, FontWeight.SemiBold),
    titleSmall = style(13, 17, FontWeight.SemiBold),
    bodyLarge = style(13, 18, FontWeight.Normal),
    bodyMedium = style(12, 17, FontWeight.Normal),
    bodySmall = style(11, 15, FontWeight.Normal),
    labelLarge = style(13, 17, FontWeight.SemiBold),
    labelMedium = style(11, 14, FontWeight.SemiBold),
    labelSmall = style(10, 13, FontWeight.SemiBold, spacing = 0.5f),
)

/** Uppercase section label ("PRÓXIMA CITA"): 10sp semibold, letter-spaced, gray. */
val SectionLabelStyle: TextStyle = style(10, 13, FontWeight.SemiBold, spacing = 0.8f)

/** Bottom navigation label (10sp; bold when selected). */
val NavLabelStyle: TextStyle = style(10, 13, FontWeight.Normal)