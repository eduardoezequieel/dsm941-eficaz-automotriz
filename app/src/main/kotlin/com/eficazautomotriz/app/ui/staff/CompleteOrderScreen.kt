package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.BoxedTextField
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.staff.CompleteOrderViewModel

/** Closes an order with the final mileage and the maintenance jobs performed. */
@Composable
fun CompleteOrderScreen(
    onCompleted: () -> Unit,
    vm: CompleteOrderViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) { if (state.completed) onCompleted() }
    val vehicle = state.vehicle

    when {
        state.loading -> LoadingBox()
        state.loadError != null -> ErrorBox(state.loadError!!, onRetry = vm::load)
        vehicle != null -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(EficazDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
        ) {
            EficazCard {
                Text(
                    "${vehicle.make} ${vehicle.model} ${vehicle.year}",
                    style = MaterialTheme.typography.titleMedium,
                    color = EficazColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    Formats.plate(vehicle.plate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CardDivider()
                LabeledValue("Kilometraje registrado", Formats.km(vehicle.currentMileage))
            }
            BoxedTextField(
                label = "Kilometraje al entregar",
                value = state.mileage,
                onValueChange = vm::onMileage,
                placeholder = "Ej. ${vehicle.currentMileage}",
                isError = state.mileageError != null,
                enabled = !state.saving,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                maxLength = vm.mileageMaxLength,
                inputFilter = InputFilters.DigitsWithThousands,
            )
            state.mileageError?.let { FieldError(it) }
            SectionLabel("Mantenimientos realizados", Modifier.padding(top = EficazDimens.SpaceXs))
            if (state.maintenanceTypes.isEmpty()) {
                MutedText("Catálogo de mantenimiento no configurado.")
            } else {
                EficazCard(contentPadding = PaddingValues(vertical = EficazDimens.SpaceXs)) {
                    state.maintenanceTypes.forEachIndexed { index, type ->
                        if (index > 0) HorizontalDivider(color = EficazColors.CardBorder)
                        val checked = type.id in state.selectedTypeIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.saving) { vm.toggleType(type.id) }
                                .padding(horizontal = EficazDimens.SpaceSm, vertical = EficazDimens.SpaceXxs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { vm.toggleType(type.id) },
                                enabled = !state.saving,
                                colors = CheckboxDefaults.colors(checkedColor = EficazColors.Navy),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    type.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = EficazColors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "Cada ${Formats.km(type.intervalKm)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EficazColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            InfoBanner("Los marcados reinician su ciclo con este kilometraje; la cita de origen pasa a Atendida.")
            state.message?.let { FieldError(it) }
            PrimaryButton(
                "Finalizar orden",
                onClick = vm::submit,
                busy = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
