package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.validation.FieldErrors
import com.eficazautomotriz.app.viewmodel.validation.VehicleFormValidator
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.validation.TextFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VehicleFormState(
    val plate: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val mileage: String = "",
    val errors: FieldErrors = FieldErrors(),
    val saving: Boolean = false,
    val message: String? = null,
    /** Id of the created vehicle: the screen navigates to its detail. */
    val createdId: String? = null,
)

/**
 * Vehicle registration. It only calls `register`: with no baseline, the detail shows every
 * maintenance type as "Sin registro previo"; the real baseline comes from closing an order.
 */
class VehicleFormViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(VehicleFormState())
    val state: StateFlow<VehicleFormState> = _state.asStateFlow()

    /** Digits of the odometer ceiling plus its thousands commas (`2,000,000` → 9). */
    val mileageMaxLength: Int = TextFields.mileageMaxLength(graph.config.maxMileage)

    fun update(transform: (VehicleFormState) -> VehicleFormState) =
        _state.update { transform(it).copy(message = null) }

    fun submit() {
        val form = _state.value
        if (form.saving) return
        val (errors, input) = VehicleFormValidator.validate(
            form.plate, form.make, form.model, form.year, form.mileage, graph.config, today(),
        )
        _state.update { it.copy(errors = errors) }
        if (input == null) return

        _state.update { it.copy(saving = true) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(saving = false, createdId = outcome.value.id)
                    is Outcome.Failure -> it.copy(saving = false, message = outcome.error.userMessage())
                }
            }
        }) {
            graph.vehicleService.register(
                actor, actor.id, input.plate, input.make, input.model, input.year, input.mileage, today(),
            )
        }
    }
}
