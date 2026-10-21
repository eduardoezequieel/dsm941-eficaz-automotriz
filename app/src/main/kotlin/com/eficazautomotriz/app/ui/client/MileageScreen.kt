package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.MileageViewModel

/** Mileage update: last recorded value in a card, boxed field and the navy "Guardar". */
@Composable
fun MileageScreen(
    onSaved: () -> Unit,
    vm: MileageViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // On return, the detail screen reloads and shows the new mileage and its status.
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    val vehicle = state.vehicle
    ScreenScaffold(
        header = { AppHeader("Actualizar kilometraje", vehicle?.let(ClientText::vehicleLine), showAvatar = false) },
    ) {
        when {
            state.loadError != null -> ErrorBox(state.loadError!!, onRetry = vm::load)
            vehicle == null -> LoadingBox()
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenPadding()),
                verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
            ) {
                EficazCard(contentPadding = PaddingValues(EficazDimens.CardPadding)) {
                    SectionLabel("Último registrado")
                    Text(Formats.km(vehicle.currentMileage), style = MaterialTheme.typography.headlineMedium, color = EficazColors.TextPrimary)
                    Text(
                        "Actualizado el ${Formats.date(vehicle.mileageUpdatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EficazColors.TextSecondary,
                    )
                }
                FormField(
                    label = "Kilometraje actual",
                    value = state.input,
                    onValueChange = vm::onInput,
                    error = state.error,
                    enabled = !state.saving,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    hint = "No puede ser menor que el último registrado. Se admite separador de miles.",
                    maxLength = vm.mileageMaxLength,
                    inputFilter = InputFilters.DigitsWithThousands,
                )
                PrimaryButton("Guardar kilometraje", onClick = vm::submit, busy = state.saving, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
