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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OrangeFab
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.StatTile
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.components.initialsOf
import com.eficazautomotriz.app.ui.components.maintenanceCardLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.VehicleSummary
import com.eficazautomotriz.app.viewmodel.VehiclesViewModel
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

/** P3 my vehicles (f3): search, one card per vehicle, maintenance summary and the orange FAB. */
@Composable
fun VehiclesScreen(
    onVehicle: (String) -> Unit,
    onNewVehicle: () -> Unit,
    vm: VehiclesViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    ReloadOnResume(vm::load)
    val count = (state as? UiState.Ready)?.data?.size
    ScreenScaffold(
        header = {
            AppHeader(
                "Mis vehículos",
                count?.let(ClientText::vehicleCount) ?: "Vehículos registrados",
                showAvatar = false,
            )
        },
        fab = { OrangeFab(onClick = onNewVehicle, contentDescription = "Agregar vehículo") },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> {
                val shown = filterVehicles(s.data, query)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = screenPadding(bottom = 96),
                    verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
                ) {
                    item { SearchField(query, { query = it }, "Buscar por placa, marca o modelo") }
                    when {
                        s.data.isEmpty() -> item { EmptyCard("Aún no registra vehículos. Agregue el primero con el botón +.") }
                        shown.isEmpty() -> item { EmptyCard("Ningún vehículo coincide con la búsqueda.") }
                        else -> items(shown, key = { it.vehicle.id }) { summary ->
                            FleetVehicleCard(summary, onClick = { onVehicle(summary.vehicle.id) })
                        }
                    }
                    if (s.data.isNotEmpty()) {
                        item { MaintenanceSummary(s.data) }
                    }
                }
            }
        }
    }
}

/** f3 vehicle card: border tinted by status, initials tile, divider, current mileage and the long chip. */
@Composable
private fun FleetVehicleCard(summary: VehicleSummary, onClick: () -> Unit) {
    val vehicle = summary.vehicle
    EficazCard(onClick = onClick, borderColor = borderFor(summary.status), contentPadding = PaddingValues(EficazDimens.CardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
                Text(
                    ClientText.vehicleName(vehicle),
                    style = MaterialTheme.typography.titleMedium,
                    color = EficazColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ClientText.plateDetail(vehicle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(EficazDimens.SpaceSm))
            InitialsTile(initialsOf("${vehicle.make} ${vehicle.model}"))
        }
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        RowDivider()
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Kilometraje actual")
                Text(
                    Formats.km(vehicle.currentMileage),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 17.sp,
                    color = EficazColors.TextPrimary,
                )
            }
            StatusChip(maintenanceCardLabel(summary.status), StatusColors.toneForMaintenance(summary.status))
        }
    }
}

private fun borderFor(status: MaintenanceStatus): Color = when (status) {
    MaintenanceStatus.DUE_SOON -> EficazColors.WarningBorder
    MaintenanceStatus.OVERDUE -> EficazColors.DangerText.copy(alpha = 0.22f)
    else -> EficazColors.CardBorder
}

/** f3 "RESUMEN DE MANTENIMIENTO": three centered counters and the explanatory caption. */
@Composable
private fun MaintenanceSummary(vehicles: List<VehicleSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        SectionHeader("Resumen de mantenimiento")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            listOf(
                Triple("Vigente", MaintenanceStatus.UP_TO_DATE, EficazColors.SuccessText),
                Triple("Próximo", MaintenanceStatus.DUE_SOON, EficazColors.WarningText),
                Triple("Vencido", MaintenanceStatus.OVERDUE, EficazColors.DangerText),
            ).forEach { (label, status, color) ->
                StatTile(
                    value = vehicles.count { it.status == status }.toString(),
                    label = label,
                    valueColor = color,
                    centered = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "El estado se calcula comparando el kilometraje actual con el intervalo configurado de cada mantenimiento.",
            style = MaterialTheme.typography.bodySmall,
            color = EficazColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceXs),
        )
    }
}

/** Matches the plate with or without the display dash ("P123-456" finds "P123456"). */
internal fun filterVehicles(vehicles: List<VehicleSummary>, query: String): List<VehicleSummary> {
    val needle = query.trim()
    if (needle.isEmpty()) return vehicles
    val plateNeedle = ValidationError.normalizePlate(needle)
    return vehicles.filter { summary ->
        val vehicle = summary.vehicle
        (plateNeedle.isNotEmpty() && ValidationError.normalizePlate(vehicle.plate).contains(plateNeedle)) ||
            vehicle.make.contains(needle, ignoreCase = true) ||
            vehicle.model.contains(needle, ignoreCase = true) ||
            ClientText.vehicleName(vehicle).contains(needle, ignoreCase = true)
    }
}
