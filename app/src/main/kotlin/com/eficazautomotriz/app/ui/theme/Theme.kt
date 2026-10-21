package com.eficazautomotriz.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Every role is set explicitly: roles left unset fall back to the Material baseline (lavender),
 * which would leak into untouched Material widgets (cards use surfaceContainerHighest, etc.).
 */
private val LightColors = lightColorScheme(
    primary = EficazColors.Navy,
    onPrimary = Color.White,
    primaryContainer = EficazColors.InfoBg,
    onPrimaryContainer = EficazColors.Navy,
    inversePrimary = EficazColors.InfoBg,
    secondary = EficazColors.Orange,
    onSecondary = Color.White,
    secondaryContainer = EficazColors.WarningBg,
    onSecondaryContainer = EficazColors.WarningText,
    tertiary = EficazColors.NavyMid,
    onTertiary = Color.White,
    tertiaryContainer = EficazColors.InfoBg,
    onTertiaryContainer = EficazColors.NavyInfo,
    background = EficazColors.Background,
    onBackground = EficazColors.TextPrimary,
    surface = EficazColors.Surface,
    onSurface = EficazColors.TextPrimary,
    surfaceVariant = EficazColors.NeutralPill,
    onSurfaceVariant = EficazColors.TextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = EficazColors.TextPrimary,
    inverseOnSurface = Color.White,
    error = EficazColors.DangerText,
    onError = Color.White,
    errorContainer = EficazColors.DangerBg,
    onErrorContainer = EficazColors.DangerText,
    outline = EficazColors.FieldBorder,
    outlineVariant = EficazColors.CardBorder,
    scrim = Color.Black,
    surfaceBright = EficazColors.Surface,
    surfaceDim = EficazColors.Background,
    surfaceContainerLowest = EficazColors.Surface,
    surfaceContainerLow = EficazColors.Surface,
    surfaceContainer = EficazColors.Surface,
    surfaceContainerHigh = EficazColors.Surface,
    surfaceContainerHighest = EficazColors.Surface,
)

/**
 * App theme: light only, as in the mockups. `darkTheme` stays in the signature to make the
 * decision explicit but defaults to false and is ignored; `dynamicColor` stays false because
 * the workshop identity (and the meaning of the status colors) must not follow the wallpaper.
 */
@Composable
fun EficazTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = LightColors, typography = EficazTypography, content = content)
}