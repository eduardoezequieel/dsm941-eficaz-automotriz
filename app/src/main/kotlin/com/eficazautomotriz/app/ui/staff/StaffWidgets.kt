package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.eficazautomotriz.app.ui.components.ButtonShape
import com.eficazautomotriz.app.ui.components.CardShape
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.SectionLabelStyle

/**
 * One-line text that shrinks its font (down to [minFontSize]) instead of wrapping or
 * ellipsizing. Counter labels such as "POR CONFIRMAR" use it. Card actions do not: f8
 * requires Confirmar / Reprogramar / Rechazar at full size.
 */
@Composable
internal fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 10.sp,
    textAlign: TextAlign? = null,
) {
    var fitted by remember(text, style) { mutableStateOf(style) }
    var ready by remember(text, style) { mutableStateOf(false) }
    Text(
        text,
        style = fitted,
        color = color,
        maxLines = 1,
        softWrap = false,
        textAlign = textAlign,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { layout ->
            val smaller = fitted.fontSize * 0.92f
            if (layout.didOverflowWidth && smaller >= minFontSize) {
                fitted = fitted.copy(
                    fontSize = smaller,
                    letterSpacing = if (fitted.letterSpacing.isSp) fitted.letterSpacing * 0.85f else fitted.letterSpacing,
                )
            } else {
                ready = true
            }
        },
    )
}

/** Look of a button inside a card action row (f8): navy filled, navy outlined, or red on gray. */
internal enum class CardActionStyle { Primary, Outlined, Danger }

private val CardActionPadding = PaddingValues(horizontal = EficazDimens.SpaceXs, vertical = EficazDimens.SpaceXxs)

/** Visual height of a card action. Above a 1.3 font scale it may grow so the label is not clipped. */
@Composable
private fun Modifier.cardActionHeight(): Modifier {
    val height = EficazDimens.ButtonHeightCompact
    return if (LocalDensity.current.fontScale <= 1.3f) heightIn(min = height, max = height) else heightIn(min = height)
}

/**
 * Button for the action rows inside staff cards ("Confirmar / Reprogramar / Rechazar",
 * "Subir evidencia / Finalizar"). Same colors and shape as the shared buttons, with narrow
 * padding so three equal labels fit on one row at 412dp without wrapping or shrinking.
 */
@Composable
internal fun CardActionButton(
    text: String,
    style: CardActionStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val sized = modifier.cardActionHeight()
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides EficazDimens.MinTouchTarget) {
        when (style) {
            CardActionStyle.Primary -> Button(
                onClick = onClick,
                enabled = enabled,
                shape = ButtonShape,
                contentPadding = CardActionPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EficazColors.Navy,
                    contentColor = Color.White,
                    disabledContainerColor = EficazColors.Navy.copy(alpha = 0.45f),
                    disabledContentColor = Color.White,
                ),
                modifier = sized,
            ) { ActionLabel(text, Color.White) }
            CardActionStyle.Outlined -> OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = ButtonShape,
                contentPadding = CardActionPadding,
                border = BorderStroke(EficazDimens.CardBorder, if (enabled) EficazColors.Navy else EficazColors.FieldBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = sized,
            ) { ActionLabel(text, if (enabled) EficazColors.Navy else EficazColors.TextMuted) }
            CardActionStyle.Danger -> Button(
                onClick = onClick,
                enabled = enabled,
                shape = ButtonShape,
                contentPadding = CardActionPadding,
                elevation = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EficazColors.NeutralPill,
                    disabledContainerColor = EficazColors.NeutralPill,
                ),
                modifier = sized,
            ) { ActionLabel(text, if (enabled) EficazColors.DangerText else EficazColors.TextMuted) }
        }
    }
}

/** Full-size one-line label. It does not shrink; the row is sized so the word fits at 412dp. */
@Composable
private fun ActionLabel(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

/** f8 counter: white card, big navy number, uppercase gray label that never wraps. */
@Composable
internal fun CounterTile(value: String, label: String, modifier: Modifier = Modifier, centered: Boolean = false) {
    Column(
        modifier
            .clip(CardShape)
            .background(EficazColors.Surface)
            .border(EficazDimens.CardBorder, EficazColors.CardBorder, CardShape)
            .padding(horizontal = EficazDimens.SpaceSm, vertical = EficazDimens.SpaceMd),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = EficazColors.Navy)
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        FitText(
            label.uppercase(),
            SectionLabelStyle,
            EficazColors.TextSecondary,
            minFontSize = 9.sp,
            textAlign = if (centered) TextAlign.Center else null,
        )
    }
}

/**
 * Selectable rounded option (vehicle, service type, slot). Selected: navy border and light
 * blue fill; disabled: muted text. Replaces M3 FilterChip in the staff forms.
 */
@Composable
internal fun OptionPill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(EficazDimens.FieldRadius)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) EficazColors.InfoBg else EficazColors.Surface)
            .border(EficazDimens.CardBorder, if (selected) EficazColors.Navy else EficazColors.FieldBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = EficazDimens.SpaceMd, vertical = EficazDimens.SpaceSm),
    ) { content() }
}

/** Main text of an [OptionPill]. */
@Composable
internal fun OptionText(text: String, selected: Boolean, enabled: Boolean = true, secondary: String? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = when {
            !enabled -> EficazColors.TextMuted
            selected -> EficazColors.Navy
            else -> EficazColors.TextPrimary
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    if (secondary != null) {
        Text(
            secondary,
            style = MaterialTheme.typography.bodySmall,
            color = EficazColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Gray body text used for empty sections inside a staff screen. */
@Composable
internal fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = EficazColors.TextSecondary,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Uppercase gray label over a semibold value ("KILOMETRAJE AL SERVICIO / 78,400 km", f7). */
@Composable
internal fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
        FitText(label.uppercase(), SectionLabelStyle, EficazColors.TextSecondary, minFontSize = 9.sp)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = EficazColors.TextPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Thin divider inside a card, under its title row (f7). */
@Composable
internal fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier.padding(vertical = EficazDimens.SpaceSm), color = EficazColors.CardBorder)
}
