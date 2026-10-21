package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.SectionLabelStyle
import com.eficazautomotriz.app.ui.theme.StatusTone

/** Card shape shared by every white card of the mockups. */
val CardShape = RoundedCornerShape(EficazDimens.CardRadius)

/** Uppercase, letter-spaced gray label ("PRÓXIMA CITA"). The text is uppercased here. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = EficazColors.TextSecondary) {
    Text(text.uppercase(), style = SectionLabelStyle, color = color, modifier = modifier)
}

/**
 * White card, 12dp radius, 1dp border, no shadow. With [accentColor] a 4dp rounded bar is
 * drawn along the left edge (the "PRÓXIMA CITA" card). Use `contentPadding = PaddingValues(0.dp)`
 * for list cards whose rows draw their own padding and dividers.
 */
@Composable
fun EficazCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    containerColor: Color = EficazColors.Surface,
    borderColor: Color = EficazColors.CardBorder,
    contentPadding: PaddingValues = PaddingValues(EficazDimens.CardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(containerColor)
            .border(BorderStroke(EficazDimens.CardBorder, borderColor), CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(IntrinsicSize.Min),
    ) {
        if (accentColor != null) {
            Box(
                Modifier
                    .width(EficazDimens.CardAccentWidth)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = EficazDimens.CardRadius, bottomStart = EficazDimens.CardRadius)),
            )
        }
        Column(
            Modifier.weight(1f).padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.CardLineGap),
            content = content,
        )
    }
}

/** White tile with a big number and an uppercase label (P3 summary, P8 counters, P9 totals). */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = EficazColors.Navy,
    centered: Boolean = false,
) {
    EficazCard(modifier) {
        val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
        Column(Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
            Spacer(Modifier.height(EficazDimens.SpaceXxs))
            SectionLabel(label)
        }
    }
}

/** Light rounded box with body text (login footer, lifecycle notes). Info = blue, Warning = orange. */
@Composable
fun InfoBanner(text: AnnotatedString, modifier: Modifier = Modifier, tone: StatusTone = StatusTone.Info) {
    val border = when (tone) {
        StatusTone.Warning -> EficazColors.WarningBorder
        StatusTone.Info -> EficazColors.InfoBorder
        else -> tone.colors.background
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(tone.colors.background)
            .border(EficazDimens.CardBorder, border, CardShape)
            .padding(horizontal = EficazDimens.CardPadding, vertical = EficazDimens.SpaceMd),
    ) {
        Text(text, color = tone.colors.content, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, tone: StatusTone = StatusTone.Info) {
    InfoBanner(AnnotatedString(text), modifier, tone)
}

/** Summary row: label at the left, value (semibold) at the right. */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = EficazDimens.SpaceXs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = EficazColors.TextPrimary,
            textAlign = TextAlign.End,
        )
    }
}

/** Light blue-gray rounded tile with an image icon and a label (evidence placeholders). */
@Composable
fun PlaceholderThumb(label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .heightIn(min = EficazDimens.ThumbHeight)
            .clip(RoundedCornerShape(EficazDimens.ThumbRadius))
            .background(EficazColors.PlaceholderTile)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = EficazDimens.SpaceSm, vertical = EficazDimens.SpaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = EficazColors.PlaceholderText, modifier = Modifier.size(EficazDimens.IconSm))
        Spacer(Modifier.height(EficazDimens.SpaceXxs))
        Text(label, style = MaterialTheme.typography.labelMedium, color = EficazColors.PlaceholderText, textAlign = TextAlign.Center)
    }
}
