package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.BoxedTextField
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.InputFilter
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.PLATE_MAX_LENGTH
import com.eficazautomotriz.app.ui.components.PlateVisualTransformation
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.VehicleFormViewModel
import com.eficazautomotriz.domain.validation.TextFields

/** Vehicle registration with the boxed fields of the login (f1). After saving it opens the detail (P4). */
@Composable
fun VehicleFormScreen(
    onCreated: (String) -> Unit,
    vm: VehicleFormViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdId) { state.createdId?.let(onCreated) }
    val enabled = !state.saving
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenPadding()),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
    ) {
        SectionHeader("Datos del vehículo")
        FormField("Placa", state.plate, { v -> vm.update { it.copy(plate = v) } }, state.errors["plate"], enabled,
            placeholder = "P123-456", hint = "Inicia con P; el guion se agrega solo. Ejemplos: P123-456, P123-AB2",
            maxLength = PLATE_MAX_LENGTH, inputFilter = InputFilters.Plate, visualTransformation = PlateVisualTransformation)
        FormField("Marca", state.make, { v -> vm.update { it.copy(make = v) } }, state.errors["make"], enabled,
            placeholder = "Toyota", maxLength = TextFields.MAKE.maxLength, inputFilter = InputFilters.NameCharacters)
        FormField("Modelo", state.model, { v -> vm.update { it.copy(model = v) } }, state.errors["model"], enabled,
            placeholder = "Corolla", maxLength = TextFields.MODEL.maxLength, inputFilter = InputFilters.NameCharacters)
        FormField("Año", state.year, { v -> vm.update { it.copy(year = v) } }, state.errors["year"], enabled,
            placeholder = "2016", keyboardType = KeyboardType.Number,
            maxLength = TextFields.YEAR.maxLength, inputFilter = InputFilters.Digits)
        FormField("Kilometraje actual", state.mileage, { v -> vm.update { it.copy(mileage = v) } }, state.errors["mileage"], enabled,
            placeholder = "78,400", keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
            hint = "Se admite separador de miles: 30,000",
            maxLength = vm.mileageMaxLength, inputFilter = InputFilters.DigitsWithThousands)
        state.message?.let { InfoBanner(it, tone = StatusTone.Danger) }
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        PrimaryButton("Guardar vehículo", onClick = vm::submit, busy = state.saving, modifier = Modifier.fillMaxWidth())
        Text(
            "El plan de mantenimiento se completa cuando el taller cierra el primer servicio de cada tipo.",
            style = MaterialTheme.typography.bodySmall,
            color = EficazColors.TextSecondary,
        )
    }
}

/** Boxed field with its error (red ⚠ line) or, when valid, an optional gray hint below. */
@Composable
internal fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    placeholder: String? = null,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    maxLength: Int? = null,
    inputFilter: InputFilter = InputFilters.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showCounter: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXs)) {
        BoxedTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            isError = error != null,
            enabled = enabled,
            keyboardType = keyboardType,
            imeAction = imeAction,
            maxLength = maxLength,
            inputFilter = inputFilter,
            visualTransformation = visualTransformation,
            showCounter = showCounter,
        )
        when {
            error != null -> FieldError(error)
            hint != null -> Text(hint, style = MaterialTheme.typography.bodySmall, color = EficazColors.TextSecondary)
        }
    }
}
