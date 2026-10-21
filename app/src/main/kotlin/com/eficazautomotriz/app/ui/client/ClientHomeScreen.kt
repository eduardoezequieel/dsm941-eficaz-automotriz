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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AccentButton
import com.eficazautomotriz.app.ui.components.DangerTextButton
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.EvidencePhotoTiles
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.components.evidenceStageLabel
import com.eficazautomotriz.app.ui.components.maintenanceShortLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.ClientHome
import com.eficazautomotriz.app.viewmodel.ClientHomeViewModel
import com.eficazautomotriz.app.viewmodel.LastService
import com.eficazautomotriz.app.viewmodel.MaintenanceAlert
import com.eficazautomotriz.app.viewmodel.VehicleSummary
import com.eficazautomotriz.domain.model.enums.EvidenceStage

/** P2 client home (f2). The "Hola, … / Rol: Cliente" header is drawn by the shell (AppRoot). */
@Composable
fun ClientHomeScreen(
    onVehicle: (String) -> Unit,
    onAppointment: (String) -> Unit,
    onService: (String) -> Unit,
    onNewAppointment: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: (vehicleId: String?) -> Unit,
    onMileage: (vehicleId: String?) -> Unit,
    vm: ClientHomeViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    ReloadOnResume(vm::load)
    MessageSnackbar(message, vm::consumeMessage)
    when (val s = state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
        is UiState.Ready -> HomeContent(
            home = s.data,
            onVehicle = onVehicle,
            onAppointment = onAppointment,
            onService = onService,
            onNewAppointment = onNewAppointment,
            onVehicles = onVehicles,
            onHistory = onHistory,
            onMileage = onMileage,
            onCancel = vm::cancel,
        )
    }
}

@Composable
private fun HomeContent(
    home: ClientHome,
    onVehicle: (String) -> Unit,
    onAppointment: (String) -> Unit,
    onService: (String) -> Unit,
    onNewAppointment: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: (String?) -> Unit,
    onMileage: (String?) -> Unit,
    onCancel: (String) -> Unit,
) {
    val soleVehicleId = home.vehicles.singleOrNull()?.vehicle?.id
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
    ) {
        item {
            NextAppointmentCard(
                row = home.nextAppointment,
                onDetail = { home.nextAppointment?.let { onAppointment(it.appointment.id) } },
                onCancel = { confirmCancel = home.nextAppointment },
                onNewAppointment = onNewAppointment,
            )
        }

        home.maintenanceAlert?.let { alert ->
            item { MaintenanceAlertCard(alert, onSchedule = onNewAppointment) }
        }

        item { SectionHeader("Accesos rápidos") }
        item {
            QuickAccess(
                onNewAppointment = onNewAppointment,
                onVehicles = onVehicles,
                onHistory = { onHistory(soleVehicleId) },
                onMileage = { onMileage(soleVehicleId) },
            )
        }

        item { SectionHeader("Último servicio") }
        item {
            val last = home.lastService
            if (last == null) {
                EmptyCard("Aún no hay servicios finalizados.")
            } else {
                LastServiceCard(last, onClick = { onService(last.row.detail.order.id) })
            }
        }

        item { SectionHeader("Mis vehículos") }
        item {
            if (home.vehicles.isEmpty()) {
                EmptyCard("Aún no registra vehículos.")
            } else {
                VehiclesCard(home.vehicles, onVehicle)
            }
        }
    }
    confirmCancel?.let { row ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = EficazColors.Surface,
            title = { Text("¿Cancelar la cita?") },
            text = {
                Text("${row.serviceName} el ${ClientText.longDayDate(row.appointment.date)}, ${ClientText.slotRange(row.slot)}. La franja quedará libre.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = null
                    onCancel(row.appointment.id)
                }) { Text("Cancelar cita", color = EficazColors.DangerText) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver", color = EficazColors.Navy) } },
        )
    }
}

/** f2 "PRÓXIMA CITA": orange left bar, chip at the right, outlined "Ver detalle" next to the red "Cancelar cita". */
@Composable
private fun NextAppointmentCard(
    row: AppointmentRow?,
    onDetail: () -> Unit,
    onCancel: () -> Unit,
    onNewAppointment: () -> Unit,
) {
    EficazCard(accentColor = EficazColors.Orange, contentPadding = PaddingValues(start = EficazDimens.CardPadding, end = EficazDimens.CardPadding, top = EficazDimens.CardPadding, bottom = EficazDimens.CardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Próxima cita", Modifier.weight(1f))
            if (row != null) {
                StatusChip(row.appointment.status.label, StatusColors.toneForAppointment(row.appointment.status))
            }
        }
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        if (row == null) {
            Text("No tiene citas próximas.", style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            OutlinedNavyButton("Solicitar cita", onClick = onNewAppointment, compact = true, modifier = Modifier.fillMaxWidth())
            return@EficazCard
        }
        Text(
            row.serviceName,
            style = MaterialTheme.typography.titleMedium,
            color = EficazColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            row.vehicle?.let(ClientText::vehicleLine) ?: row.appointment.vehicleId,
            style = MaterialTheme.typography.bodyMedium,
            color = EficazColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${ClientText.longDayDate(row.appointment.date)} · ${ClientText.slotRange(row.slot)}",
            style = MaterialTheme.typography.bodyMedium,
            color = EficazColors.TextSecondary,
        )
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        Row(horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            OutlinedNavyButton("Ver detalle", onClick = onDetail, compact = true, modifier = Modifier.weight(1f))
            if (row.cancellable) {
                DangerTextButton("Cancelar cita", onClick = onCancel, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** f2 orange alert: tinted card, white chip, vehicle line, mileage sentence with bold figures, "Agendar ahora". */
@Composable
private fun MaintenanceAlertCard(alert: MaintenanceAlert, onSchedule: () -> Unit) {
    val text = EficazColors.WarningText
    EficazCard(
        containerColor = EficazColors.WarningBg,
        borderColor = EficazColors.WarningBorder,
        contentPadding = PaddingValues(EficazDimens.ScreenPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Alerta de mantenimiento", Modifier.weight(1f), color = text)
            OnTintChip(maintenanceShortLabel(alert.status), StatusColors.toneForMaintenance(alert.status))
        }
        Spacer(Modifier.height(EficazDimens.SpaceXxs))
        Text(
            ClientText.vehicleShortLine(alert.vehicle),
            style = MaterialTheme.typography.bodyMedium,
            color = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val bold = SpanStyle(fontWeight = FontWeight.Bold)
        Text(
            buildAnnotatedString {
                append("${ClientText.alertAction(alert.typeName)} a los ")
                withStyle(bold) { append(Formats.km(alert.recommendedMileage)) }
                append(". Kilometraje actual: ")
                withStyle(bold) { append(Formats.km(alert.currentMileage)) }
                append(".")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = text,
        )
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        AccentButton("Agendar ahora", onClick = onSchedule, compact = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QuickAccess(
    onNewAppointment: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: () -> Unit,
    onMileage: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        QuickTile(Icons.Outlined.EditCalendar, "Nueva cita", onNewAppointment, Modifier.weight(1f))
        QuickTile(Icons.Outlined.DirectionsCar, "Vehículos", onVehicles, Modifier.weight(1f))
        QuickTile(Icons.Outlined.History, "Historial", onHistory, Modifier.weight(1f))
        QuickTile(Icons.Outlined.Speed, "Actualizar km", onMileage, Modifier.weight(1f))
    }
}

/** White tile with a navy icon over a bold label. */
@Composable
private fun QuickTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    EficazCard(modifier, onClick = onClick, contentPadding = PaddingValues(horizontal = EficazDimens.SpaceXs, vertical = EficazDimens.SpaceMd)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = EficazColors.Navy, modifier = Modifier.size(EficazDimens.IconMd))
            Spacer(Modifier.height(EficazDimens.SpaceXs))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = EficazColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/** f2 "ÚLTIMO SERVICIO": title and chip, vehicle · date · km line, three evidence tiles. */
@Composable
private fun LastServiceCard(last: LastService, onClick: () -> Unit) {
    val order = last.row.detail.order
    val details = listOfNotNull(
        ClientText.vehicleName(last.vehicle),
        ClientText.dayMonthYear((order.closedAt ?: order.receivedAt).toLocalDate()),
        order.mileageAtService?.let(Formats::km),
    ).joinToString(" · ")
    EficazCard(onClick = onClick, contentPadding = PaddingValues(EficazDimens.CardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                last.row.serviceName,
                style = MaterialTheme.typography.titleMedium,
                color = EficazColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(EficazDimens.SpaceSm))
            StatusChip(order.status.label, StatusColors.toneForService(order.status))
        }
        Text(
            details,
            style = MaterialTheme.typography.bodySmall,
            color = EficazColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        EvidencePhotoTiles(
            last.row.detail.evidences,
            labelOf = { stage -> if (stage == EvidenceStage.DIAGNOSIS) "Proceso" else evidenceStageLabel(stage) },
        )
    }
}

/** f2 "MIS VEHÍCULOS": one white card, a row per vehicle separated by thin dividers. */
@Composable
private fun VehiclesCard(vehicles: List<VehicleSummary>, onVehicle: (String) -> Unit) {
    EficazCard(contentPadding = PaddingValues(0.dp)) {
        Column {
        vehicles.forEachIndexed { index, summary ->
            if (index > 0) RowDivider()
            val vehicle = summary.vehicle
            ListRow(
                title = ClientText.vehicleName(vehicle),
                subtitle = "${Formats.plate(vehicle.plate)} · ${Formats.km(vehicle.currentMileage)}",
                onClick = { onVehicle(vehicle.id) },
                trailing = { StatusChip(maintenanceShortLabel(summary.status), StatusColors.toneForMaintenance(summary.status)) },
            )
        }
        }
    }
}

/** White card with a gray sentence, used where a section has nothing to show yet. */
@Composable
internal fun EmptyCard(text: String, modifier: Modifier = Modifier) {
    EficazCard(modifier, contentPadding = PaddingValues(EficazDimens.CardPadding)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
    }
}
