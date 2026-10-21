package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AccentButton
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.EficazHeader
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.components.initialsOf
import com.eficazautomotriz.app.ui.components.maintenanceShortLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.VehicleDetail
import com.eficazautomotriz.app.viewmodel.VehicleDetailViewModel
import com.eficazautomotriz.application.MaintenanceLine
import com.eficazautomotriz.application.VehicleMaintenanceReport
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

private val detailTabs = listOf("Mantenimiento", "Historial", "Citas")

/**
 * P4 vehicle detail (f4). The header carries the vehicle name, plate and initials; the
 * initials open a menu with the full history, the mileage update and the removal.
 */
@Composable
fun VehicleDetailScreen(
    onMileage: () -> Unit,
    onHistory: () -> Unit,
    onDeleted: () -> Unit,
    onNewAppointment: () -> Unit,
    onService: (String) -> Unit,
    onAppointment: (String) -> Unit = {},
    vm: VehicleDetailViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    ReloadOnResume(vm::load)
    MessageSnackbar(message, vm::consumeMessage)
    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    val vehicle = (state as? UiState.Ready)?.data?.report?.vehicle
    ScreenScaffold(
        header = {
            VehicleHeader(
                vehicle = vehicle,
                onHistory = onHistory,
                onMileage = onMileage,
                onDelete = { confirmDelete = true },
            )
        },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> DetailContent(s.data, onMileage, onNewAppointment, onService, onAppointment)
        }
    }
    if (confirmDelete && vehicle != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = EficazColors.Surface,
            title = { Text("¿Eliminar ${Formats.plate(vehicle.plate)}?") },
            text = { Text("También se borran sus registros de mantenimiento.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete()
                }) { Text("Eliminar", color = EficazColors.DangerText) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = EficazColors.Navy) } },
        )
    }
}

@Composable
private fun VehicleHeader(vehicle: Vehicle?, onHistory: () -> Unit, onMileage: () -> Unit, onDelete: () -> Unit) {
    EficazHeader(
        title = vehicle?.let(ClientText::vehicleName) ?: "Detalle del vehículo",
        subtitle = vehicle?.let(ClientText::plateDetail) ?: "Plan de mantenimiento",
        avatarInitials = vehicle?.let { initialsOf("${it.make} ${it.model}") },
        onBack = shellBack(),
        avatarMenu = { dismiss ->
            DropdownMenuItem(text = { Text("Ver historial completo") }, onClick = {
                dismiss()
                onHistory()
            })
            DropdownMenuItem(text = { Text("Actualizar kilometraje") }, onClick = {
                dismiss()
                onMileage()
            })
            DropdownMenuItem(text = { Text("Eliminar vehículo", color = EficazColors.DangerText) }, onClick = {
                dismiss()
                onDelete()
            })
        },
    )
}

@Composable
private fun DetailContent(
    detail: VehicleDetail,
    onMileage: () -> Unit,
    onNewAppointment: () -> Unit,
    onService: (String) -> Unit,
    onAppointment: (String) -> Unit,
) {
    val report = detail.report
    val schedule = scheduleActionLabel(report)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = screenPadding(),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
        ) {
            item { MileageCard(report.vehicle, onMileage) }
            item { SegmentedTabs(detailTabs, tab, onSelect = { tab = it }) }
            when (tab) {
                0 -> {
                    item { SectionHeader("Plan de mantenimiento preventivo") }
                    item {
                        if (report.lines.isEmpty()) {
                            EmptyCard("Catálogo de mantenimiento no configurado.")
                        } else {
                            MaintenancePlanCard(report.lines)
                        }
                    }
                    item { SectionHeader("Servicios recientes de este vehículo") }
                    item {
                        if (detail.recentServices.isEmpty()) {
                            EmptyCard("Este vehículo aún no tiene servicios registrados.")
                        } else {
                            ServiceRowsCard(detail.recentServices, onService)
                        }
                    }
                }
                1 -> {
                    item { SectionHeader("Historial de servicios", trailing = serviceCount(detail.history.size)) }
                    item {
                        if (detail.history.isEmpty()) {
                            EmptyCard("Este vehículo aún no tiene servicios registrados.")
                        } else {
                            ServiceRowsCard(detail.history, onService)
                        }
                    }
                }
                else -> {
                    item { SectionHeader("Citas de este vehículo") }
                    item {
                        if (detail.appointments.isEmpty()) {
                            EmptyCard("Este vehículo aún no tiene citas.")
                        } else {
                            AppointmentRowsCard(detail.appointments, onAppointment)
                        }
                    }
                }
            }
        }
        if (schedule != null) {
            AccentButton(
                schedule,
                onClick = onNewAppointment,
                modifier = Modifier.fillMaxWidth().padding(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.SpaceSm, bottom = EficazDimens.ScreenPadding),
            )
        }
    }
}

/** f4 top card: big current mileage, update date and the outlined "Actualizar km". */
@Composable
private fun MileageCard(vehicle: Vehicle, onMileage: () -> Unit) {
    EficazCard(contentPadding = PaddingValues(EficazDimens.CardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Kilometraje actual")
                Text(Formats.km(vehicle.currentMileage), style = MaterialTheme.typography.headlineMedium, color = EficazColors.TextPrimary)
                Text(
                    "Actualizado el ${Formats.date(vehicle.mileageUpdatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EficazColors.TextSecondary,
                )
            }
            OutlinedNavyButton("Actualizar km", onClick = onMileage, compact = true)
        }
    }
}

/** f4 plan: one card, a row per maintenance type; overdue rows red, due-soon rows orange. */
@Composable
private fun MaintenancePlanCard(lines: List<MaintenanceLine>) {
    EficazCard(contentPadding = PaddingValues(0.dp)) {
        // Inner column without spacing: tinted rows must touch their dividers.
        Column {
            sortedBySeverity(lines).forEachIndexed { index, line ->
                if (index > 0) RowDivider()
                MaintenanceLineRow(line)
            }
        }
    }
}

@Composable
private fun MaintenanceLineRow(line: MaintenanceLine) {
    val status = line.evaluation.status
    val (background, titleColor, textColor) = when (status) {
        MaintenanceStatus.OVERDUE -> Triple(EficazColors.DangerBg, EficazColors.DangerText, EficazColors.DangerText)
        MaintenanceStatus.DUE_SOON -> Triple(EficazColors.WarningBg, EficazColors.WarningText, EficazColors.WarningText)
        MaintenanceStatus.UP_TO_DATE -> Triple(EficazColors.Surface, EficazColors.TextPrimary, EficazColors.TextSecondary)
        MaintenanceStatus.NO_PREVIOUS_RECORD -> Triple(EficazColors.Surface, EficazColors.TextSecondary, EficazColors.TextMuted)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                line.type.name,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(EficazDimens.SpaceSm))
            when (status) {
                MaintenanceStatus.OVERDUE, MaintenanceStatus.DUE_SOON -> Text(
                    maintenanceShortLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = titleColor,
                    modifier = Modifier.padding(end = EficazDimens.SpaceSm),
                )
                else -> StatusChip(maintenanceShortLabel(status), StatusColors.toneForMaintenance(status))
            }
        }
        Text(baselineText(line), style = MaterialTheme.typography.bodySmall, color = textColor)
        Text(recommendationText(line), style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

/** f4 order: overdue rows first, then due soon, up to date and without record; stable within each. */
internal fun sortedBySeverity(lines: List<MaintenanceLine>): List<MaintenanceLine> = lines.sortedBy {
    when (it.evaluation.status) {
        MaintenanceStatus.OVERDUE -> 0
        MaintenanceStatus.DUE_SOON -> 1
        MaintenanceStatus.UP_TO_DATE -> 2
        MaintenanceStatus.NO_PREVIOUS_RECORD -> 3
    }
}

private fun baselineText(line: MaintenanceLine): String {
    val last = line.lastServiceMileage?.let(Formats::km) ?: "sin registro"
    return "Último: $last · Intervalo: ${Formats.km(line.type.intervalKm)}"
}

/** "Recomendado a los **84,000 km** · excedido por 100 km" / "· faltan 400 km". */
internal fun recommendationText(line: MaintenanceLine): AnnotatedString {
    val recommended = line.evaluation.recommendedMileage
        ?: return AnnotatedString("Se registra al cerrar el primer servicio de este tipo")
    val difference = line.evaluation.differenceKm
    return buildAnnotatedString {
        append("Recomendado a los ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Formats.km(recommended)) }
        if (difference != null) {
            append(if (difference < 0) " · excedido por ${Formats.km(-difference)}" else " · faltan ${Formats.km(difference)}")
        }
    }
}

/** Bottom orange action of f4; hidden when every line is up to date. */
internal fun scheduleActionLabel(report: VehicleMaintenanceReport): String? = when {
    report.lines.any { it.evaluation.status == MaintenanceStatus.OVERDUE } -> "Agendar mantenimiento vencido"
    report.lines.any { it.evaluation.status == MaintenanceStatus.DUE_SOON } -> "Agendar mantenimiento"
    else -> null
}

private fun serviceCount(count: Int): String = if (count == 1) "1 servicio" else "$count servicios"
