package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.StaffOrderRow
import java.time.LocalDate

/**
 * Card body of f8: semibold title, gray lines below it and the status chip at the right,
 * vertically centered on the text block. [actions] is an optional row of equal-width buttons.
 */
@Composable
internal fun StaffItemCard(
    title: String,
    lines: List<String>,
    chip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** Detail lines use the full card width, under the title row. Order subtitles are too long to sit beside the chip. */
    linesBelow: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    EficazCard(modifier = modifier, onClick = onClick) {
        if (linesBelow) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = EficazColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(EficazDimens.SpaceSm))
                chip()
            }
            lines.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = EficazColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    lines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = EficazColors.TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(EficazDimens.SpaceSm))
                chip()
            }
        }
        if (actions != null) {
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm), content = actions)
        }
    }
}

/** Appointment card of f8: service, "Nissan Frontier · P987-321", "Lun 24 ago · 9:00 – 10:00 a.m.". */
@Composable
internal fun StaffAppointmentCard(
    row: AppointmentRow,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showClient: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val appointment = row.appointment
    StaffItemCard(
        title = row.serviceName,
        lines = listOfNotNull(
            row.clientName?.takeIf { showClient }?.let { "Cliente: $it" },
            vehicleLabel(row),
            StaffFormats.appointmentLine(appointment.date, row.slot),
        ),
        chip = { StatusChip(appointment.status.label, StatusColors.toneForAppointment(appointment.status)) },
        modifier = modifier,
        onClick = onClick,
        actions = actions,
    )
}

/** Order card of f8: "Reparación de motor · #SV-2104", "Toyota Corolla · recibido a las 8:30 a.m.". */
@Composable
internal fun StaffOrderCard(
    row: StaffOrderRow,
    today: LocalDate,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val order = row.order
    val vehicle = row.vehicle?.let { "${it.make} ${it.model}" } ?: "Vehículo no disponible"
    StaffItemCard(
        title = "${row.serviceName} · #${order.id}",
        lines = listOf("$vehicle · ${StaffFormats.receivedLine(order.receivedAt, today)}"),
        chip = { StatusChip(order.status.label, StatusColors.toneForService(order.status)) },
        modifier = modifier,
        onClick = onClick,
        linesBelow = true,
        actions = actions,
    )
}
