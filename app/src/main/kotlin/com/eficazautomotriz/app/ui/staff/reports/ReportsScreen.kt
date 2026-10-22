package com.eficazautomotriz.app.ui.staff.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.DatePickerField
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.KeyValueRow
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.components.maintenanceShortLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.staff.CounterTile
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.EficazTheme
import com.eficazautomotriz.app.viewmodel.staff.ReportPeriodPreset
import com.eficazautomotriz.app.viewmodel.staff.Reports
import com.eficazautomotriz.app.viewmodel.staff.ReportsState
import com.eficazautomotriz.app.viewmodel.staff.ReportsViewModel
import com.eficazautomotriz.application.PeriodReport
import com.eficazautomotriz.application.ReportRow
import com.eficazautomotriz.application.SystemSummary
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDate

private val presets = listOf(
    ReportPeriodPreset.WEEK to "Semana",
    ReportPeriodPreset.MONTH to "Mes",
    ReportPeriodPreset.QUARTER to "Trimestre",
)

private const val ExportSubject = "Resumen de reportes · Eficaz Automotriz"

/**
 * P9 (f9): Semana / Mes / Trimestre, appointments by status (columns), completed services by
 * type (donut), period totals, "Exportar resumen" and "Filtrar periodo" (custom dates in a
 * dialog). [onExport] receives the plain-text summary; by default it opens the share sheet.
 */
@Composable
fun ReportsScreen(
    vm: ReportsViewModel = viewModel(factory = LocalVmFactory.current),
    onExport: ((String) -> Unit)? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filtering by rememberSaveable { mutableStateOf(false) }
    ReloadOnResume(vm::load)
    val export = onExport ?: { text -> context.shareText(ExportSubject, text) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(EficazDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
    ) {
        SegmentedTabs(
            options = presets.map { it.second },
            selectedIndex = presets.indexOfFirst { it.first == state.preset },
            onSelect = { vm.selectPreset(presets[it].first) },
        )
        if (state.preset == null) {
            Text(
                "Periodo personalizado: ${Formats.date(state.from)} – ${Formats.date(state.to)}",
                style = MaterialTheme.typography.bodyMedium,
                color = EficazColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val reports = state.reports) {
            UiState.Loading -> LoadingBox(Modifier.height(240.dp))
            is UiState.Error -> ErrorBox(reports.message, onRetry = vm::load)
            is UiState.Ready -> ReportsContent(
                reports = reports.data,
                demoMode = vm.demoMode,
                onExport = { export(reportSummaryText(reports.data, state.from, state.to)) },
                onFilter = { filtering = true },
            )
        }
    }
    if (filtering) PeriodDialog(state, vm, onDismiss = { filtering = false })
}

@Composable
private fun ReportsContent(reports: Reports, demoMode: Boolean, onExport: () -> Unit, onFilter: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        EficazCard {
            SectionLabel("Citas por estado")
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            VerticalBarChart(
                groupAppointmentRows(reports.appointmentsByStatus.rows).map { (group, count) ->
                    ChartBar(group.label, count, ChartColors.forGroup(group))
                },
            )
        }
        EficazCard {
            SectionLabel("Servicios realizados por tipo")
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            val services = reports.completedServicesByType
            DonutChart(services.rows, total = services.total, colorFor = ChartColors.byPosition(services.rows))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            CounterTile("${reports.appointmentsByStatus.total}", "Citas", Modifier.weight(1f), centered = true)
            CounterTile("${reports.completedServicesByType.total}", "Servicios", Modifier.weight(1f), centered = true)
            CounterTile("${reports.summary.totalVehicles}", "Vehículos", Modifier.weight(1f), centered = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            OutlinedNavyButton("Exportar resumen", onClick = onExport, modifier = Modifier.weight(1f))
            PrimaryButton("Filtrar periodo", onClick = onFilter, modifier = Modifier.weight(1f))
        }
        if (demoMode) {
            Text(
                "Los valores mostrados son datos de ejemplo del prototipo.",
                style = MaterialTheme.typography.bodyMedium,
                color = EficazColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SummaryCard(reports.summary)
    }
}

/** Custom range behind "Filtrar periodo": each date applies at once, the validator's error shows below. */
@Composable
private fun PeriodDialog(state: ReportsState, vm: ReportsViewModel, onDismiss: () -> Unit) {
    val years = state.earliest.year..state.latest.year
    val inRange = { date: LocalDate -> !date.isBefore(state.earliest) && !date.isAfter(state.latest) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EficazColors.Surface,
        title = { Text("Filtrar periodo", style = MaterialTheme.typography.titleLarge, color = EficazColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                SectionLabel("Desde")
                DatePickerField("Desde", state.from, years, inRange, vm::setFrom)
                SectionLabel("Hasta")
                DatePickerField("Hasta", state.to, years, inRange, vm::setTo)
                state.periodError?.let { FieldError(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo", color = EficazColors.Navy, fontWeight = FontWeight.SemiBold) } },
    )
}

/** Live counts of the whole system (not limited to the period); below the f9 content. */
@Composable
private fun SummaryCard(summary: SystemSummary) {
    EficazCard {
        SectionLabel("Resumen del sistema")
        KeyValueRow("Vehículos registrados", "${summary.totalVehicles}")
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        SectionLabel("Citas")
        summary.appointmentsByStatus.forEach { (status, count) -> KeyValueRow(status.label, "$count") }
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        SectionLabel("Órdenes de servicio")
        summary.serviceOrdersByStatus.forEach { (status, count) -> KeyValueRow(status.label, "$count") }
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        SectionLabel("Mantenimiento de vehículos")
        summary.vehiclesByMaintenanceStatus.forEach { (status, count) -> KeyValueRow(maintenanceShortLabel(status), "$count") }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1400)
@Composable
private fun ReportsContentPreview() {
    val from = LocalDate.of(2026, 10, 1)
    val to = LocalDate.of(2026, 10, 18)
    EficazTheme {
        ReportsContent(
            Reports(
                appointmentsByStatus = PeriodReport(
                    "CITAS POR ESTADO", from, to,
                    listOf(ReportRow("Atendida", 16, 66.7), ReportRow("Pendiente", 4, 16.7), ReportRow("Rechazada", 4, 16.7)),
                    24,
                ),
                completedServicesByType = PeriodReport(
                    "SERVICIOS FINALIZADOS POR TIPO", from, to,
                    listOf(ReportRow("Mantenimiento preventivo", 3, 60.0), ReportRow("Sistema de frenos", 2, 40.0)),
                    5,
                ),
                summary = SystemSummary(
                    totalVehicles = 4,
                    appointmentsByStatus = AppointmentStatus.entries.associateWith { 1 },
                    serviceOrdersByStatus = ServiceStatus.entries.associateWith { 1 },
                    vehiclesByMaintenanceStatus = MaintenanceStatus.entries.associateWith { 1 },
                ),
            ),
            demoMode = true,
            onExport = {},
            onFilter = {},
        )
    }
}
