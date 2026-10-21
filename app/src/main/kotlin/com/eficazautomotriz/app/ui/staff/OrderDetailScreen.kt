package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.EvidenceStageThumbnails
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.ServiceStatusTimeline
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.staff.OrderAction
import com.eficazautomotriz.app.viewmodel.staff.OrderDetail
import com.eficazautomotriz.app.viewmodel.staff.OrderDetailViewModel

/**
 * Staff order detail, laid out like f7 (P7): summary card, actions, status timeline,
 * evidence thumbnails per stage, reception notes and the source appointment.
 * Owns its header so the subtitle can name the order and the vehicle.
 */
@Composable
fun OrderDetailScreen(
    onEvidence: () -> Unit,
    onComplete: () -> Unit,
    vm: OrderDetailViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val starting by vm.starting.collectAsStateWithLifecycle()
    ReloadOnResume(vm::load)
    MessageSnackbar(message, vm::consumeMessage)

    val detail = (state as? UiState.Ready)?.data
    ScreenScaffold(
        header = { AppHeader("Orden de servicio", detail?.let(::orderSubtitle), showAvatar = false) },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> OrderDetailContent(s.data, starting, onStart = vm::start, onEvidence, onComplete)
        }
    }
}

/** "Orden #SV-2104 · Toyota Corolla 2016", as the f7 header. */
private fun orderSubtitle(detail: OrderDetail): String {
    val order = detail.row.detail.order
    return "Orden #${order.id}" + (detail.vehicle?.let { " · ${it.make} ${it.model} ${it.year}" } ?: "")
}

@Composable
private fun OrderDetailContent(
    detail: OrderDetail,
    starting: Boolean,
    onStart: () -> Unit,
    onEvidence: () -> Unit,
    onComplete: () -> Unit,
) {
    val order = detail.row.detail.order
    val evidences = detail.row.detail.evidences
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(EficazDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
    ) {
        item(key = "summary") {
            EficazCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        detail.row.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = EficazColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(order.status.label, StatusColors.toneForService(order.status))
                }
                CardDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                    val mileage = order.mileageAtService
                    if (mileage != null) {
                        LabeledValue("Kilometraje al servicio", Formats.km(mileage), Modifier.weight(1f))
                    } else {
                        LabeledValue("Kilometraje actual", detail.vehicle?.let { Formats.km(it.currentMileage) } ?: "—", Modifier.weight(1f))
                    }
                    val closedAt = order.closedAt
                    if (closedAt != null) {
                        LabeledValue("Fecha de cierre", Formats.date(closedAt.toLocalDate()), Modifier.weight(1f))
                    } else {
                        LabeledValue("Recibido", Formats.date(order.receivedAt.toLocalDate()), Modifier.weight(1f))
                    }
                }
                detail.vehicle?.let { vehicle ->
                    LabeledValue("Vehículo", StaffFormats.vehicleLine(vehicle, order.vehicleId), Modifier.padding(top = EficazDimens.SpaceSm))
                }
            }
        }
        val actions = detail.actions
        if (actions.isNotEmpty()) {
            item(key = "actions") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                    if (OrderAction.ADD_EVIDENCE in actions) {
                        OutlinedNavyButton("Subir evidencia", onClick = onEvidence, modifier = Modifier.weight(1f))
                    }
                    if (OrderAction.START in actions) {
                        PrimaryButton("Iniciar", onClick = onStart, busy = starting, modifier = Modifier.weight(1f))
                    }
                    if (OrderAction.COMPLETE in actions) {
                        PrimaryButton("Finalizar", onClick = onComplete, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "timeline-label") { SectionLabel("Seguimiento de estados", Modifier.padding(top = EficazDimens.SpaceXs)) }
        item(key = "timeline") { EficazCard { ServiceStatusTimeline(order) } }
        item(key = "evidence-label") {
            Row(Modifier.fillMaxWidth().padding(top = EficazDimens.SpaceXs), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Evidencias fotográficas", Modifier.weight(1f))
                Text(
                    if (evidences.size == 1) "1 imagen" else "${evidences.size} imágenes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                )
            }
        }
        item(key = "evidence") { EvidenceStageThumbnails(evidences) }
        item(key = "notes") {
            EficazCard {
                SectionLabel("Notas de recepción")
                Text(
                    order.notes.ifBlank { "Sin notas de recepción." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = EficazColors.TextSecondary,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        order.appointmentId?.let { appointmentId ->
            item(key = "appointment") {
                EficazCard {
                    SectionLabel("Cita asociada")
                    Text(
                        "#$appointmentId",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EficazColors.TextPrimary,
                    )
                }
            }
        }
    }
}
