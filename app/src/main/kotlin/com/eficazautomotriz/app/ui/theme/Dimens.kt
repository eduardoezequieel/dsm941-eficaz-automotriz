package com.eficazautomotriz.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and component sizes measured on the Etapa 1 mockups. The PNGs are 1140x2436 px,
 * a 3x phone of ~380dp, so a mockup length in px divided by 3 gives dp (card margin 42 px →
 * 14dp, card radius 33 px → 11dp). Controls keep a legible/touchable floor above the raw
 * measurement (mockup buttons are ~29–37dp, chips ~18dp, bottom bar ~56dp).
 */
object EficazDimens {
    // Spacing scale.
    val SpaceXxs = 2.dp
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp

    /** Horizontal margin between the screen edge and the cards (42 px). */
    val ScreenPadding = 14.dp
    /** Side margin of the centered login form (f1: 60 px). */
    val FormPadding = 20.dp
    /** Vertical gap between consecutive cards and sections (40 px). */
    val SectionGap = 12.dp
    /** Gap between the section label and its card (~36 px). */
    val LabelGap = 8.dp

    // Cards.
    val CardRadius = 12.dp
    val CardPadding = 14.dp
    val CardBorder = 1.dp
    /** Gap between the lines stacked inside a card (mockup line pitch ~1.4x the font size). */
    val CardLineGap = 2.dp
    /** Orange bar of the "Próxima cita" card (13 px). */
    val CardAccentWidth = 4.dp

    // Header band (below the status bar: ~176 px).
    val HeaderMinHeight = 56.dp
    val HeaderPaddingStart = 16.dp
    val HeaderPaddingEnd = 14.dp
    val HeaderPaddingTop = 8.dp
    val HeaderPaddingBottom = 12.dp
    /** Initials square at the right of the header (102 px). */
    val AvatarSize = 38.dp
    val AvatarRadius = 11.dp

    // Buttons.
    val ButtonRadius = 10.dp
    /** Full-width actions ("Iniciar sesión", "Agendar ahora"). */
    val ButtonHeight = 40.dp
    /** Buttons inside cards ("Ver detalle", "Confirmar"). */
    val ButtonHeightCompact = 36.dp
    /** Touch target of compact controls (visual size can be smaller). */
    val MinTouchTarget = 44.dp
    val ButtonPaddingHorizontal = 12.dp
    val FabSize = 52.dp
    val FabRadius = 14.dp

    // Chips and segmented tabs.
    val ChipHeight = 20.dp
    val ChipPaddingHorizontal = 10.dp
    val SegmentHeight = 34.dp
    val SegmentRadius = 12.dp
    val SegmentInnerRadius = 9.dp
    val SegmentPadding = 3.dp

    // Fields.
    val FieldRadius = 12.dp
    val FieldPaddingHorizontal = 14.dp
    val FieldPaddingVertical = 8.dp

    // Tiles and thumbnails.
    /** Evidence placeholders (131 px). */
    val ThumbHeight = 52.dp
    val ThumbRadius = 10.dp
    val IconSm = 18.dp
    val IconMd = 22.dp

    // Bottom navigation (168 px including the home indicator).
    val BottomBarHeight = 56.dp
    val BottomBarIcon = 22.dp
}
