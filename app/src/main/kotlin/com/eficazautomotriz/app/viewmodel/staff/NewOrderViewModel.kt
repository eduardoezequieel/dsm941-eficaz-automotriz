package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.validation.TextFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Plate search ignores the display hyphen, so "P123-456" and "P123456" find the same vehicle.
 * A blank query keeps the full list. Make and model match as typed.
 */
internal fun filterReceptionVehicles(vehicles: List<Vehicle>, query: String): List<Vehicle> {
    val needle = query.trim()
    if (needle.isEmpty()) return vehicles
    val compactNeedle = ValidationError.normalizePlate(needle)
    return vehicles.filter { vehicle ->
        // A query made only of hyphens normalizes to empty, and every plate contains "".
        (compactNeedle.isNotEmpty() && ValidationError.normalizePlate(vehicle.plate).contains(compactNeedle)) ||
            vehicle.make.contains(needle, ignoreCase = true) ||
            vehicle.model.contains(needle, ignoreCase = true) ||
            "${vehicle.make} ${vehicle.model}".contains(needle, ignoreCase = true)
    }
}

data class NewOrderState(
    val loading: Boolean = true,
    val loadError: String? = null,
    /** Set when the order comes from a confirmed appointment: vehicle and service are locked. */
    val appointment: Appointment? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val serviceTypes: List<ServiceType> = emptyList(),
    val vehicleId: String? = null,
    val serviceTypeId: String? = null,
    val notes: String = "",
    val notesError: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val createdId: String? = null,
) {
    val fromAppointment: Boolean get() = appointment != null
    val canSubmit: Boolean get() = vehicleId != null && serviceTypeId != null && !saving
}

/** Vehicle reception: with an appointment it preloads vehicle and service, otherwise both are picked. */
class NewOrderViewModel(
    graph: AppGraph,
    private val actor: User,
    private val appointmentId: String?,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(NewOrderState())
    val state: StateFlow<NewOrderState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> outcome.value.copy(notes = it.notes)
                    is Outcome.Failure -> it.copy(loading = false, loadError = outcome.error.userMessage())
                }
            }
        }) {
            val vehicles = graph.vehicleService.listVisibleTo(actor)
            val types = graph.appointmentService.activeServiceTypes()
            val appointment = appointmentId?.let { id ->
                graph.appointmentService.listVisibleTo(actor).firstOrNull { it.id == id }
                    ?: return@run Outcome.failure(DomainError.NotFound("cita", id))
            }
            Outcome.success(
                NewOrderState(
                    loading = false,
                    appointment = appointment,
                    vehicles = vehicles,
                    serviceTypes = types,
                    vehicleId = appointment?.vehicleId,
                    serviceTypeId = appointment?.serviceTypeId,
                )
            )
        }
    }

    fun selectVehicle(id: String) {
        if (!_state.value.fromAppointment) _state.update { it.copy(vehicleId = id, message = null) }
    }

    fun selectServiceType(id: String) {
        if (!_state.value.fromAppointment) _state.update { it.copy(serviceTypeId = id, message = null) }
    }

    fun onNotes(notes: String) = _state.update { it.copy(notes = notes, notesError = null) }

    fun submit() {
        val form = _state.value
        val vehicleId = form.vehicleId ?: return
        val serviceTypeId = form.serviceTypeId ?: return
        if (form.saving) return
        ValidationError.validateOptionalText(TextFields.NOTES, form.notes.trim())?.let { error ->
            _state.update { it.copy(notesError = error.message) }
            return
        }
        _state.update { it.copy(saving = true, message = null) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(saving = false, createdId = outcome.value.id)
                    is Outcome.Failure -> it.copy(saving = false, message = outcome.error.userMessage())
                }
            }
        }) {
            graph.serviceOrderService.create(actor, vehicleId, serviceTypeId, form.appointment?.id, form.notes.trim(), now())
        }
    }
}