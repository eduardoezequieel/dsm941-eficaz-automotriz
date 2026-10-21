package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.BoxedTextField
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.staff.NewOrderViewModel
import com.eficazautomotriz.app.viewmodel.staff.filterReceptionVehicles
import com.eficazautomotriz.domain.validation.TextFields

/** Vehicle reception: creates a service order, optionally from a confirmed appointment. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewOrderScreen(
    onCreated: (String) -> Unit,
    vm: NewOrderViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.createdId) { state.createdId?.let(onCreated) }

    when {
        state.loading -> LoadingBox()
        state.loadError != null -> ErrorBox(state.loadError!!, onRetry = vm::load)
        else -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(EficazDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
        ) {
            state.appointment?.let {
                InfoBanner("Desde la cita #${it.id}: el vehículo y el servicio quedan fijados por la cita.")
            }
            SectionLabel("Vehículo", Modifier.padding(top = EficazDimens.SpaceXs))
            if (!state.fromAppointment && state.vehicles.isNotEmpty()) {
                BoxedTextField(
                    label = "Buscar vehículo",
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Placa, marca o modelo",
                    enabled = !state.saving,
                    imeAction = ImeAction.Search,
                    maxLength = TextFields.VEHICLE_SEARCH.maxLength,
                    inputFilter = InputFilters.SingleLine,
                )
            }
            val shown = if (state.fromAppointment) state.vehicles else filterReceptionVehicles(state.vehicles, query)
            if (state.vehicles.isEmpty()) MutedText("No hay vehículos registrados.")
            else if (shown.isEmpty()) MutedText("Ningún vehículo coincide con la búsqueda.")
            shown.forEach { vehicle ->
                val selected = state.vehicleId == vehicle.id
                val enabled = !state.fromAppointment || selected
                OptionPill(selected, onClick = { vm.selectVehicle(vehicle.id) }, Modifier.fillMaxWidth(), enabled = enabled) {
                    OptionText(
                        "${vehicle.make} ${vehicle.model} ${vehicle.year}",
                        selected = selected,
                        enabled = enabled,
                        secondary = "${Formats.plate(vehicle.plate)} · ${Formats.km(vehicle.currentMileage)}",
                    )
                }
            }
            SectionLabel("Tipo de servicio", Modifier.padding(top = EficazDimens.SpaceSm))
            if (state.serviceTypes.isEmpty()) MutedText("Catálogo no configurado.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                state.serviceTypes.forEach { type ->
                    val selected = state.serviceTypeId == type.id
                    val enabled = !state.fromAppointment || selected
                    OptionPill(selected, onClick = { vm.selectServiceType(type.id) }, enabled = enabled) {
                        OptionText(type.name, selected = selected, enabled = enabled)
                    }
                }
            }
            SectionLabel("Notas de recepción", Modifier.padding(top = EficazDimens.SpaceSm))
            BoxedTextField(
                label = "Estado del vehículo",
                value = state.notes,
                onValueChange = vm::onNotes,
                placeholder = "Síntomas, golpes, pertenencias…",
                isError = state.notesError != null,
                enabled = !state.saving,
                singleLine = false,
                maxLength = TextFields.NOTES.maxLength,
                showCounter = true,
            )
            state.notesError?.let { FieldError(it) }
            state.message?.let { FieldError(it) }
            PrimaryButton(
                "Recibir vehículo",
                onClick = vm::submit,
                enabled = state.canSubmit,
                busy = state.saving,
                modifier = Modifier.fillMaxWidth().padding(top = EficazDimens.SpaceXs),
            )
        }
    }
}
