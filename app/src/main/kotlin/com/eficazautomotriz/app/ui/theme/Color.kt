package com.eficazautomotriz.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mockup palette (Etapa 1, sampled from the f1–f9 PNGs). Screens read these through
 * [EficazColors] and never hardcode hex values.
 */
object EficazColors {
    /** Header band, primary buttons, selected nav item, titles on light backgrounds. */
    val Navy = Color(0xFF14425C)
    /** Header avatar square and secondary navy accents. */
    val NavyMid = Color(0xFF2E6A8C)
    /** Link-like navy used by info chips and banners. */
    val NavyInfo = Color(0xFF134E86)
    /** FAB, "Agendar ahora", accent bar of the next-appointment card. */
    val Orange = Color(0xFFE07A28)

    val Background = Color(0xFFF4F6F8)
    val Surface = Color.White
    val CardBorder = Color(0xFFE3E8EC)
    /** Border of form fields (slightly darker than cards). */
    val FieldBorder = Color(0xFFC9D2D8)
    /** Container of segmented tabs and the light gray "destructive" pill. */
    val SegmentBackground = Color(0xFFE8EDF1)
    val NeutralPill = Color(0xFFEFF2F4)

    val TextPrimary = Color(0xFF16242E)
    val TextSecondary = Color(0xFF657782)
    /** Unselected bottom navigation items. */
    val TextMuted = Color(0xFF8D9CA6)
    /** Subtitle inside the navy header. */
    val HeaderSubtitle = Color(0xFFD4DDE1)

    /** Evidence placeholders: light blue-gray tile and its label. */
    val PlaceholderTile = Color(0xFFD2DBE1)
    val PlaceholderText = Color(0xFF48606E)

    // Chip pairs (background, text) and banner borders.
    val InfoBg = Color(0xFFE6EFF8)
    val InfoText = NavyInfo
    val InfoBorder = Color(0xFFCFE0F0)
    val WarningBg = Color(0xFFFCEFDC)
    val WarningText = Color(0xFF9A5B00)
    val WarningBorder = Color(0xFFF0DCBB)
    val SuccessBg = Color(0xFFE4F2EC)
    val SuccessText = Color(0xFF17724F)
    val DangerBg = Color(0xFFFBE9E7)
    val DangerText = Color(0xFFA72820)
    val NeutralBg = Color(0xFFEDF1F4)
    val NeutralText = TextSecondary
}

// Legacy names kept so screens not yet redesigned keep compiling; they map to the mockup palette.
val WorkshopBlue = EficazColors.Navy
val WorkshopBlueLight = EficazColors.InfoBg
val WorkshopBlueDark = EficazColors.Navy
val WorkshopBlueContainer = EficazColors.InfoBg
val ToolOrange = EficazColors.Orange
val ToolOrangeContainer = EficazColors.WarningBg
val Graphite = EficazColors.TextPrimary
val Steel = EficazColors.TextSecondary
val Mist = EficazColors.Background
val ErrorRed = EficazColors.DangerText

val StatusGreen = EficazColors.SuccessText
val StatusAmber = EficazColors.WarningText
val StatusRed = EficazColors.DangerText
val StatusGray = EficazColors.NeutralText
