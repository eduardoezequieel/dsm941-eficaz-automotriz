package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.DangerTextButton
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.KeyValueRow
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OrangeFab
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.AppointmentDetailViewModel
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.AppointmentsViewModel
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import java.time.LocalDate
import java.time.LocalTime

/** Chip text of f5: the two waiting states say who has to act. */
internal fun appointmentCardLabel(status: AppointmentStatus): String = when (status) {
    AppointmentStatus.PENDING -> "Pendiente de confirmación"
    AppointmentStatus.RESCHEDULED -> "Reprogramada por el taller"
    else -> status.label
}

/** Left bar of the f5 cards: navy confirmed, orange pending, gray rescheduled, red rejected. */
private fun accentFor(status: AppointmentStatus): Color = when (status) {
    AppointmentStatus.CONFIRMED -> EficazColors.NavyInfo
    AppointmentStatus.PENDING -> EficazColors.Orange
    AppointmentStatus.RESCHEDULED -> EficazColors.TextMuted
    AppointmentStatus.REJECTED -> EficazColors.DangerText
    AppointmentStatus.ATTENDED -> EficazColors.SuccessText
    AppointmentStatus.CANCELLED -> EficazColors.TextMuted
}

/** Previous date stored in a reschedule reason: "Fecha anterior: 2026-10-21 14:00." */
private val earlierSlotPattern = Regex("""Fecha anterior: (\d{4}-\d{2}-\d{2}) (\d{2}:\d{2})""")

internal data class EarlierSlot(val date: LocalDate, val time: LocalTime)

/** The slot the shop moved the appointment away from, when the reason records one. */
internal fun earlierSlot(reason: String?): EarlierSlot? {
    val match = reason?.let { earlierSlotPattern.find(it) } ?: return null
    return EarlierSlot(LocalDate.parse(match.groupValues[1]), LocalTime.parse(match.groupValues[2]))
}

/** Workshop sentence without the machine-readable previous slot. */
internal fun rescheduleExplanation(reason: String?): String? =
    reason?.replace(earlierSlotPattern, "")
        ?.replace(Regex("""^\.\s*"""), "")
        ?.trim()
        ?.ifBlank { null }

/** "Motivo indicado por el taller: horario no disponible." */
internal fun rejectionText(reason: String?): String {
    val text = reason?.trim()?.trimEnd('.')?.replaceFirstChar { it.lowercase() }?.ifBlank { null } ?: "sin detalle"
    return "Motivo indicado por el taller: $text."
}

/** P5 my appointments (f5): "Próximas" / "Historial" segments, status-colored cards and the orange FAB. */
@Composable
fun AppointmentsScreen(
    onAppointment: (String) -> Unit,
    onNewAppointment: () -> Unit,
    vm: AppointmentsViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    ReloadOnResume(vm::load)
    MessageSnackbar(message, vm::consumeMessage)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var toCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    var toDecline by remember { mutableStateOf<AppointmentRow?>(null) }

    ScreenScaffold(
        header = { AppHeader("Mis citas", "Solicitudes y confirmaciones", showAvatar = false) },
        fab = { OrangeFab(onClick = onNewAppointment, contentDescription = "Solicitar cita") },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> {
                val upcomingTab = selected == 0
                val rows = if (upcomingTab) s.data.upcoming else s.data.history
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = screenPadding(bottom = 96),
                    verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
                ) {
                    item(key = "tabs") { SegmentedTabs(listOf("Próximas", "Historial"), selected, onSelect = { selected = it }) }
                    if (rows.isEmpty()) {
                        item(key = "empty") {
                            EmptyCard(
                                if (upcomingTab) "No tiene citas próximas. Solicite una con el botón +."
                                else "Aún no hay citas atendidas, rechazadas ni canceladas.",
                            )
                        }
                    }
                    items(rows, key = { it.appointment.id }) { row ->
                        AppointmentStatusCard(
                            row = row,
                            onClick = { onAppointment(row.appointment.id) },
                            onCancel = { toCancel = row },
                            onAccept = { vm.acceptReschedule(row.appointment.id) },
                            onDecline = { toDecline = row },
                        )
                    }
                    if (upcomingTab) {
                        item(key = "status-info") { InfoBanner(lifecycleText()) }
                    }
                }
            }
        }
    }
    toCancel?.let { row ->
        CancelDialog(row, onConfirm = { vm.cancel(row.appointment.id); toCancel = null }, onDismiss = { toCancel = null })
    }
    toDecline?.let { row ->
        DeclineRescheduleDialog(
            row,
            onConfirm = { vm.declineReschedule(row.appointment.id); toDecline = null },
            onDismiss = { toDecline = null },
        )
    }
}

/**
 * f5 card: colored left bar, chip and "#CT-1042" on top, service, vehicle, date line, and the
 * actions the state allows (cancel pill; accept/decline for a reschedule; the reason when rejected).
 * Pass null actions to show the card read-only.
 */
@Composable
private fun AppointmentStatusCard(
    row: AppointmentRow,
    onClick: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onAccept: (() -> Unit)?,
    onDecline: (() -> Unit)?,
) {
    val appointment = row.appointment
    val status = appointment.status
    EficazCard(
        accentColor = accentFor(status),
        onClick = onClick,
        contentPadding = PaddingValues(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding, bottom = EficazDimens.ScreenPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(appointmentCardLabel(status), StatusColors.toneForAppointment(status))
            Spacer(Modifier.weight(1f))
            Text("#${appointment.id}", style = MaterialTheme.typography.bodySmall, color = EficazColors.TextSecondary)
        }
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        Text(
            row.serviceName,
            style = MaterialTheme.typography.titleMedium,
            color = EficazColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (status == AppointmentStatus.REJECTED) {
            Text(rejectionText(appointment.reason), style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
            return@EficazCard
        }
        Text(
            row.vehicle?.let(ClientText::vehicleLine) ?: appointment.vehicleId,
            style = MaterialTheme.typography.bodyMedium,
            color = EficazColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(appointmentDateLine(row), style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
        if (row.canRespondToReschedule && onAccept != null && onDecline != null) {
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            Row(horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                PrimaryButton("Aceptar", onClick = onAccept, compact = true, modifier = Modifier.weight(1f))
                OutlinedNavyButton("Rechazar", onClick = onDecline, compact = true, modifier = Modifier.weight(1f))
            }
        }
        if (row.cancellable && onCancel != null) {
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            DangerTextButton(
                if (status == AppointmentStatus.PENDING) "Cancelar solicitud" else "Cancelar cita",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * "Vie 21 ago 2026 · 10:00 – 11:00 a.m.". A reschedule with a recorded previous slot draws
 * "Mié 19 ago · 2:00 p.m. → Jue 20 ago · 9:00 a.m.", the old half struck through (f5).
 */
internal fun appointmentDateLine(row: AppointmentRow): AnnotatedString {
    val appointment = row.appointment
    val current = "${ClientText.shortDate(appointment.date)} · ${ClientText.slotRange(row.slot)}"
    val earlier = if (appointment.status == AppointmentStatus.RESCHEDULED) earlierSlot(appointment.reason) else null
    if (earlier == null) return AnnotatedString(current)
    val start = row.slot?.let { ClientText.time12(it.startTime) } ?: "franja no disponible"
    return buildAnnotatedString {
        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = EficazColors.TextMuted)) {
            append("${ClientText.shortDayMonth(earlier.date)} · ${ClientText.time12(earlier.time)}")
        }
        append(" → ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = EficazColors.TextPrimary)) {
            append("${ClientText.shortDayMonth(appointment.date)} · $start")
        }
    }
}

/** Lifecycle reminder at the bottom of "Próximas" (f5 blue banner). */
private fun lifecycleText(): AnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    return buildAnnotatedString {
        append("Estados de una cita: ")
        withStyle(bold) { append("pendiente → confirmada → atendida") }
        append(". El taller puede además ")
        withStyle(bold) { append("reprogramar") }
        append(" o ")
        withStyle(bold) { append("rechazar") }
        append("; el cliente puede ")
        withStyle(bold) { append("cancelar") }
        append(" antes de la confirmación o de la fecha.")
    }
}

/** Appointment detail (client/appointments/{id}): the f5 card, its data and the allowed actions. */
@Composable
fun AppointmentDetailScreen(vm: AppointmentDetailViewModel = viewModel(factory = LocalVmFactory.current)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    MessageSnackbar(message, vm::consumeMessage)
    var confirm by remember { mutableStateOf(false) }
    var decline by remember { mutableStateOf(false) }
    when (val s = state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
        is UiState.Ready -> {
            val row = s.data
            val appointment = row.appointment
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenPadding()),
                verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
            ) {
                AppointmentStatusCard(row, onClick = null, onCancel = null, onAccept = null, onDecline = null)
                SectionHeader("Datos de la solicitud")
                EficazCard(contentPadding = PaddingValues(horizontal = EficazDimens.CardPadding, vertical = EficazDimens.SpaceSm)) {
                    row.vehicle?.let { BoundedKeyValue("Vehículo", "${it.make} ${it.model} · ${Formats.plate(it.plate)}") }
                    BoundedKeyValue("Servicio", row.serviceName)
                    KeyValueRow("Fecha", ClientText.shortDate(appointment.date))
                    KeyValueRow("Franja", ClientText.slotRange(row.slot))
                    KeyValueRow("Solicitada el", ClientText.dateTime12(appointment.requestedAt))
                }
                rescheduleExplanation(appointment.reason)
                    ?.takeIf { appointment.status == AppointmentStatus.RESCHEDULED }
                    ?.let { InfoBanner(it, tone = StatusTone.Neutral) }
                if (row.canRespondToReschedule) {
                    InfoBanner("El taller reprogramó su cita. Acepte la nueva fecha o rechácela para cancelarla.")
                    Row(horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                        PrimaryButton("Aceptar", onClick = vm::acceptReschedule, modifier = Modifier.weight(1f))
                        OutlinedNavyButton("Rechazar", onClick = { decline = true }, modifier = Modifier.weight(1f))
                    }
                }
                if (row.cancellable) {
                    DangerTextButton(
                        if (appointment.status == AppointmentStatus.PENDING) "Cancelar solicitud" else "Cancelar cita",
                        onClick = { confirm = true },
                        compact = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (confirm) CancelDialog(row, onConfirm = { vm.cancel(); confirm = false }, onDismiss = { confirm = false })
            if (decline) {
                DeclineRescheduleDialog(
                    row,
                    onConfirm = { vm.declineReschedule(); decline = false },
                    onDismiss = { decline = false },
                )
            }
        }
    }
}

@Composable
private fun CancelDialog(row: AppointmentRow, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EficazColors.Surface,
        title = { Text("¿Cancelar la cita?") },
        text = {
            Text("${row.serviceName} el ${ClientText.shortDate(row.appointment.date)}, ${ClientText.slotRange(row.slot)}. La franja quedará libre.")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Cancelar cita", color = EficazColors.DangerText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver", color = EficazColors.Navy) } },
    )
}

@Composable
private fun DeclineRescheduleDialog(row: AppointmentRow, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EficazColors.Surface,
        title = { Text("¿Rechazar la nueva fecha?") },
        text = {
            Text(
                "${row.serviceName} el ${ClientText.shortDate(row.appointment.date)}, ${ClientText.slotRange(row.slot)}. " +
                    "La cita quedará cancelada y la franja libre. Puede solicitar otra cita cuando lo necesite.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Rechazar", color = EficazColors.DangerText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver", color = EficazColors.Navy) } },
    )
}
