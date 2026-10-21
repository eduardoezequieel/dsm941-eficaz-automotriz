package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.BoxedTextField
import com.eficazautomotriz.app.ui.components.DangerTextButton
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.DatePickerField
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.SlotOption
import com.eficazautomotriz.app.viewmodel.staff.RescheduleState
import com.eficazautomotriz.app.viewmodel.staff.StaffAppointmentState
import com.eficazautomotriz.app.viewmodel.staff.StaffAppointmentViewModel
import com.eficazautomotriz.app.viewmodel.validation.ReasonValidator
import com.eficazautomotriz.domain.validation.TextFields
import java.time.LocalDate

/**
 * Staff appointment detail (no mockup; derived from f7/f8): summary card, then
 * Confirmar / Reprogramar / Rechazar while the request is open, or "Recibir vehículo"
 * once it is confirmed. Owns its header so the subtitle can carry the id and the client.
 */
@Composable
fun StaffAppointmentScreen(
    onCreateOrder: (String) -> Unit,
    vm: StaffAppointmentViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    MessageSnackbar(state.message, vm::consumeMessage)
    var rejecting by remember { mutableStateOf(false) }

    val ready = (state.row as? UiState.Ready)?.data
    ScreenScaffold(
        header = {
            AppHeader(
                title = "Solicitud de cita",
                subtitle = ready?.let { row -> "#${row.appointment.id}" + (row.clientName?.let { " · $it" } ?: "") },
                showAvatar = false,
            )
        },
    ) {
        when (val row = state.row) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(row.message, onRetry = vm::load)
            is UiState.Ready -> AppointmentContent(
                row = row.data,
                state = state,
                vm = vm,
                onReject = { rejecting = true },
                onCreateOrder = onCreateOrder,
            )
        }
    }
    if (rejecting) RejectDialog(onReject = { reason -> if (vm.reject(reason)) rejecting = false }, onDismiss = { rejecting = false })
}

@Composable
private fun AppointmentContent(
    row: AppointmentRow,
    state: StaffAppointmentState,
    vm: StaffAppointmentViewModel,
    onReject: () -> Unit,
    onCreateOrder: (String) -> Unit,
) {
    val appointment = row.appointment
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(EficazDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
    ) {
        EficazCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = EficazColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(appointment.status.label, StatusColors.toneForAppointment(appointment.status))
            }
            CardDivider()
            LabeledValue("Fecha y hora", StaffFormats.appointmentLine(appointment.date, row.slot))
            LabeledValue("Vehículo", vehicleLabel(row), Modifier.padding(top = EficazDimens.SpaceSm))
            row.clientName?.let { LabeledValue("Cliente", it, Modifier.padding(top = EficazDimens.SpaceSm)) }
            LabeledValue("Solicitada", Formats.dateTime(appointment.requestedAt), Modifier.padding(top = EficazDimens.SpaceSm))
        }
        appointment.reason?.let { InfoBanner("Motivo: $it", tone = StatusTone.Neutral) }
        if (state.canDecide && state.reschedule == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                CardActionButton("Confirmar", CardActionStyle.Primary, vm::confirm, Modifier.weight(1f), enabled = !state.busy)
                CardActionButton("Reprogramar", CardActionStyle.Outlined, vm::startReschedule, Modifier.weight(1f), enabled = !state.busy)
                CardActionButton("Rechazar", CardActionStyle.Danger, onReject, Modifier.weight(1f), enabled = !state.busy)
            }
        }
        if (state.canCreateOrder) {
            PrimaryButton(
                "Recibir vehículo y crear orden",
                onClick = { onCreateOrder(appointment.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.reschedule?.let { form ->
            ReschedulePanel(
                form = form,
                busy = state.busy,
                today = vm.today,
                lastDate = vm.lastSchedulableDate(),
                isSchedulable = vm::isSchedulable,
                onDate = vm::rescheduleDate,
                onSlot = vm::rescheduleSlot,
                onReason = vm::rescheduleReason,
                onSubmit = vm::submitReschedule,
                onCancel = vm::cancelReschedule,
            )
        }
    }
}

@Composable
private fun ReschedulePanel(
    form: RescheduleState,
    busy: Boolean,
    today: LocalDate,
    lastDate: LocalDate,
    isSchedulable: (LocalDate) -> Boolean,
    onDate: (LocalDate) -> Unit,
    onSlot: (SlotOption) -> Unit,
    onReason: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    EficazCard {
        Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            SectionLabel("Reprogramar")
            DatePickerField(
                label = "Nueva fecha",
                value = form.date,
                yearRange = today.year..lastDate.year,
                isSelectable = isSchedulable,
                onSelected = onDate,
            )
            when {
                form.date == null -> Unit
                form.slotsLoading -> MutedText("Cargando franjas…")
                form.slots.isEmpty() -> MutedText("El taller no atiende ese día.")
                else -> SlotChips(form.slots, form.slotId, onSlot)
            }
            BoxedTextField(
                label = "Motivo (se muestra al cliente)",
                value = form.reason,
                onValueChange = onReason,
                isError = form.reasonError != null,
                enabled = !busy,
                imeAction = ImeAction.Done,
                maxLength = TextFields.REASON.maxLength,
                inputFilter = InputFilters.SingleLine,
                showCounter = true,
            )
            form.reasonError?.let { FieldError(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                OutlinedNavyButton("Cancelar", onClick = onCancel, enabled = !busy, modifier = Modifier.weight(1f))
                PrimaryButton(
                    "Reprogramar",
                    onClick = onSubmit,
                    enabled = form.date != null && form.slotId != null,
                    busy = busy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Reason dialog for rejecting a request. The panel and the detail screen both use it. */
@Composable
internal fun RejectDialog(onReject: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EficazColors.Surface,
        title = { Text("Rechazar cita", style = MaterialTheme.typography.titleLarge, color = EficazColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                Text(
                    "El cliente verá este motivo en su lista de citas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                )
                BoxedTextField(
                    label = "Motivo",
                    value = reason,
                    onValueChange = {
                        reason = it
                        error = null
                    },
                    isError = error != null,
                    imeAction = ImeAction.Done,
                    maxLength = TextFields.REASON.maxLength,
                    inputFilter = InputFilters.SingleLine,
                    showCounter = true,
                )
                error?.let { FieldError(it) }
            }
        },
        confirmButton = {
            DangerTextButton(
                "Rechazar",
                onClick = {
                    val failure = ReasonValidator.error(reason)
                    if (failure != null) error = failure else onReject(reason)
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Volver", color = EficazColors.Navy, fontWeight = FontWeight.SemiBold) }
        },
    )
}
