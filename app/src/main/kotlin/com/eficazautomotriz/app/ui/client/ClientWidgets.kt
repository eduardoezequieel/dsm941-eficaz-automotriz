package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eficazautomotriz.app.ui.LocalShell
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.domain.validation.TextFields
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.ChipColors
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.ServiceOrderRow

/** Padding of every client list: 16dp gutters, extra bottom room when a FAB or fixed button floats over it. */
internal fun screenPadding(bottom: Int = 24) = PaddingValues(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding, bottom = bottom.dp)

/** Thin row separator inside list cards (f2 "MIS VEHÍCULOS", f4 plan rows). */
@Composable
internal fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = EficazColors.CardBorder)
}

/** Section label with an optional gray text at the right ("EVIDENCIAS FOTOGRÁFICAS … 3 imágenes"). */
@Composable
internal fun SectionHeader(label: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = EficazDimens.SpaceXs), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(label, Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
        }
    }
}

/** Chip on a tinted card (f2 alert): white pill with the tone's text color. */
@Composable
internal fun OnTintChip(label: String, tone: StatusTone) {
    StatusChip(label, ChipColors(Color.White, tone.colors.content))
}

/** Rounded square with a vehicle's initials (f3 "TC"): light blue-gray with navy text. */
@Composable
internal fun InitialsTile(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(EficazDimens.AvatarSize)
            .clip(RoundedCornerShape(EficazDimens.ThumbRadius))
            .background(EficazColors.PlaceholderTile.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = EficazColors.Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** f3 search box: white rounded field with a gray magnifier. */
@Composable
internal fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EficazDimens.FieldRadius)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = EficazDimens.MinTouchTarget)
            .clip(shape)
            .background(EficazColors.Surface)
            .border(1.dp, EficazColors.FieldBorder, shape)
            .padding(horizontal = EficazDimens.FieldPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = EficazColors.TextSecondary, modifier = Modifier.size(EficazDimens.IconMd))
        Spacer(Modifier.width(EficazDimens.SpaceSm))
        BasicTextField(
            value = value,
            onValueChange = { typed ->
                onValueChange(InputFilters.apply(typed, InputFilters.SingleLine, TextFields.SEARCH.maxLength))
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = EficazColors.TextPrimary),
            cursorBrush = SolidColor(EficazColors.Navy),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = EficazColors.TextSecondary)
                }
                inner()
            },
        )
    }
}

/**
 * f6 header: [AppHeader] plus the 4-segment progress bar drawn on the same navy band
 * (reached segments white, the rest translucent white).
 */
@Composable
internal fun StepHeader(title: String, subtitle: String, step: Int, steps: Int = 4) {
    Column(Modifier.background(EficazColors.Navy)) {
        AppHeader(title, subtitle, showAvatar = false)
        Row(
            Modifier.fillMaxWidth().padding(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, bottom = EficazDimens.SpaceMd),
            horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXs),
        ) {
            repeat(steps) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < step) Color.White else Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

/** Back action of the current route (null on tab routes), for screens that build their own header. */
@Composable
internal fun shellBack(): (() -> Unit)? = LocalShell.current?.onBack

/**
 * One white card listing service orders: title, "dd/MM/yyyy · km" and the status chip, rows
 * separated by thin dividers (f4 "SERVICIOS RECIENTES DE ESTE VEHÍCULO", vehicle history).
 */
@Composable
internal fun ServiceRowsCard(rows: List<ServiceOrderRow>, onService: (String) -> Unit, modifier: Modifier = Modifier) {
    EficazCard(modifier, contentPadding = PaddingValues(0.dp)) {
        Column {
        rows.forEachIndexed { index, row ->
            if (index > 0) RowDivider()
            val order = row.detail.order
            val details = listOfNotNull(
                Formats.date((order.closedAt ?: order.receivedAt).toLocalDate()),
                order.mileageAtService?.let(Formats::km),
            ).joinToString(" · ")
            ListRow(
                title = row.serviceName,
                subtitle = details,
                onClick = { onService(order.id) },
                trailing = { StatusChip(order.status.label, StatusColors.toneForService(order.status)) },
            )
        }
        }
    }
}

/** Same layout for appointments (P4 "Citas" tab): service, "Vie 21 ago 2026 · 10:00 – 11:00 a.m.", chip. */
@Composable
internal fun AppointmentRowsCard(rows: List<AppointmentRow>, onAppointment: (String) -> Unit, modifier: Modifier = Modifier) {
    EficazCard(modifier, contentPadding = PaddingValues(0.dp)) {
        Column {
        rows.forEachIndexed { index, row ->
            if (index > 0) RowDivider()
            val appointment = row.appointment
            ListRow(
                title = row.serviceName,
                subtitle = "${ClientText.shortDate(appointment.date)} · ${ClientText.slotRange(row.slot)}",
                onClick = { onAppointment(appointment.id) },
                trailing = { StatusChip(appointment.status.label, StatusColors.toneForAppointment(appointment.status)) },
            )
        }
        }
    }
}

/**
 * Summary row whose value stays on one line. A long make, model or plate ellipsizes
 * instead of pushing the label off the row.
 */
@Composable
internal fun BoundedKeyValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = EficazDimens.SpaceXs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
        Spacer(Modifier.width(EficazDimens.SpaceSm))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = EficazColors.TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Row of a list card: bold title, gray line below, trailing content (usually a chip). */
@Composable
internal fun ListRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = EficazColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EficazColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(EficazDimens.SpaceSm))
        trailing()
    }
}
