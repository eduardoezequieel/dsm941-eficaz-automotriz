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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.EvidencePhotoTiles
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.ServiceTimelineCard
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.ServiceDetail
import com.eficazautomotriz.app.viewmodel.ServiceDetailViewModel
import com.eficazautomotriz.app.viewmodel.ServiceHistoryViewModel

/** Service history of one vehicle: a single divided card, as the f4 recent-services list. */
@Composable
fun ServiceHistoryScreen(
    onService: (String) -> Unit,
    vm: ServiceHistoryViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
        is UiState.Ready -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
        ) {
            item {
                SectionHeader(
                    "Servicios de este vehículo",
                    trailing = if (s.data.size == 1) "1 servicio" else "${s.data.size} servicios",
                )
            }
            item {
                if (s.data.isEmpty()) {
                    EmptyCard("Este vehículo aún no tiene servicios registrados.")
                } else {
                    ServiceRowsCard(s.data, onService)
                }
            }
        }
    }
}

/** P7 service detail (f7). The header names the order and the vehicle once they are loaded. */
@Composable
fun ServiceDetailScreen(vm: ServiceDetailViewModel = viewModel(factory = LocalVmFactory.current)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val detail = (state as? UiState.Ready)?.data
    ScreenScaffold(
        header = {
            AppHeader(
                "Detalle del servicio",
                detail?.let { "Orden #${it.order.detail.order.id} · ${ClientText.vehicleName(it.vehicle)}" },
                showAvatar = false,
            )
        },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> ServiceDetailContent(s.data)
        }
    }
}

@Composable
private fun ServiceDetailContent(detail: ServiceDetail) {
    val order = detail.order.detail.order
    val evidences = detail.order.detail.evidences
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
    ) {
        item { ServiceSummaryCard(detail) }
        item { SectionHeader("Seguimiento de estados") }
        item { ServiceTimelineCard(order, timeText = ClientText::dateTime12) }
        item { SectionHeader("Evidencias fotográficas", trailing = ClientText.imageCount(evidences.size)) }
        item { EvidencePhotoTiles(evidences, tileHeight = 60.dp) }
        item {
            EficazCard(contentPadding = PaddingValues(EficazDimens.CardPadding)) {
                SectionLabel("Observaciones del personal")
                Text(
                    order.notes.ifBlank { "Sin observaciones." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                )
            }
        }
        item { RecordCard(detail) }
        item {
            Text(
                "Las imágenes se almacenan en Cloudinary; el documento del servicio guarda únicamente la URL segura.",
                style = MaterialTheme.typography.bodySmall,
                color = EficazColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceSm),
            )
        }
    }
}

/** f7 first card: service and chip, divider, mileage and closing date side by side. */
@Composable
private fun ServiceSummaryCard(detail: ServiceDetail) {
    val order = detail.order.detail.order
    EficazCard(contentPadding = PaddingValues(EficazDimens.CardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(detail.order.serviceName, style = MaterialTheme.typography.titleMedium, color = EficazColors.TextPrimary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(EficazDimens.SpaceSm))
            StatusChip(order.status.label, StatusColors.toneForService(order.status))
        }
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        RowDivider()
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        Row(Modifier.fillMaxWidth()) {
            LabeledValue("Kilometraje al servicio", order.mileageAtService?.let(Formats::km) ?: "Sin registro", Modifier.weight(1.25f))
            LabeledValue("Fecha de cierre", order.closedAt?.toLocalDate()?.let(Formats::date) ?: "Sin cierre", Modifier.weight(1f))
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionLabel(label)
        Text(value, style = MaterialTheme.typography.titleSmall, color = EficazColors.TextPrimary)
    }
}

/** f7 last card: "CITA ASOCIADA" (id · date · hour + chip) and "REGISTRADO POR" with the date. */
@Composable
private fun RecordCard(detail: ServiceDetail) {
    val order = detail.order.detail.order
    val appointment = detail.appointment
    EficazCard(contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Cita asociada")
                Text(
                    when {
                        appointment != null -> {
                            val start = appointment.slot?.let { " · ${ClientText.time12(it.startTime)}" }.orEmpty()
                            "#${appointment.appointment.id} · ${ClientText.compactDate(appointment.appointment.date)}$start"
                        }
                        order.appointmentId != null -> "#${order.appointmentId}"
                        else -> "Sin cita (ingreso directo)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = EficazColors.TextPrimary,
                )
            }
            appointment?.let {
                StatusChip(it.appointment.status.label, StatusColors.toneForAppointment(it.appointment.status))
            }
        }
        RowDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Registrado por")
                Text("Personal del taller", style = MaterialTheme.typography.titleSmall, color = EficazColors.TextPrimary)
            }
            Text(
                Formats.date((order.closedAt ?: order.receivedAt).toLocalDate()),
                style = MaterialTheme.typography.bodySmall,
                color = EficazColors.TextSecondary,
            )
        }
    }
}
