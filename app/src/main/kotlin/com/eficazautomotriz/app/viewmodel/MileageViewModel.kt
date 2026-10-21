package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.validation.MileageValidator
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.validation.TextFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MileageState(
    val vehicle: Vehicle? = null,
    val loadError: String? = null,
    val input: String = "",
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

/** Mileage update: early warning when it decreases; the service validates it again. */
class MileageViewModel(
    graph: AppGraph,
    private val actor: User,
    private val vehicleId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(MileageState())
    val state: StateFlow<MileageState> = _state.asStateFlow()

    /** Same cap as the registration form: digits of the ceiling plus the thousands commas. */
    val mileageMaxLength: Int = TextFields.mileageMaxLength(graph.config.maxMileage)

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loadError = null) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(vehicle = outcome.value, input = outcome.value.currentMileage.toString())
                    is Outcome.Failure -> it.copy(loadError = outcome.error.userMessage())
                }
            }
        }) { graph.vehicleService.findAccessible(actor, vehicleId) }
    }

    fun onInput(value: String) = _state.update { it.copy(input = value, error = null) }

    fun submit() {
        val current = _state.value
        val vehicle = current.vehicle ?: return
        if (current.saving) return
        val (error, mileage) = MileageValidator.validate(current.input, vehicle.currentMileage, graph.config)
        if (mileage == null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(saving = true) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(saving = false, saved = true, vehicle = outcome.value)
                    is Outcome.Failure -> it.copy(saving = false, error = outcome.error.userMessage())
                }
            }
        }) { graph.vehicleService.updateMileage(actor, vehicleId, mileage, today()) }
    }
}
