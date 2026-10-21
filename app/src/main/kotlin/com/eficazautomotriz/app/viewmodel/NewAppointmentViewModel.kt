package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.validation.NewAppointmentValidator
import com.eficazautomotriz.application.SlotAvailability
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.rules.AvailabilityResult
import com.eficazautomotriz.domain.rules.UnavailabilityReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate

enum class AppointmentStep { VEHICLE, SERVICE, DATE, SLOT, SUMMARY, DONE }

/** Slot of the chosen day, with a label explaining why it cannot be taken. */
data class SlotOption(val availability: SlotAvailability, val unavailableLabel: String?) {
    val slotId: String get() = availability.slot.id
    val selectable: Boolean get() = availability.selectable
}

data class NewAppointmentState(
    val step: AppointmentStep = AppointmentStep.VEHICLE,
    val loading: Boolean = true,
    val loadError: String? = null,
    /** No service types or no slots: staff has not loaded the catalog yet. */
    val catalogMissing: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val serviceTypes: List<ServiceType> = emptyList(),
    val vehicle: Vehicle? = null,
    val serviceType: ServiceType? = null,
    val date: LocalDate? = null,
    val slots: List<SlotOption> = emptyList(),
    val slotsLoading: Boolean = false,
    val slot: SlotOption? = null,
    val submitting: Boolean = false,
    val message: String? = null,
    /** Shown under the calendar after a submit that has no date. */
    val dateError: String? = null,
    /** Shown under the slot grid after a submit that has a date but no slot. */
    val slotError: String? = null,
    val created: Appointment? = null,
    val today: LocalDate = LocalDate.now(),
    val lastDate: LocalDate = LocalDate.now(),
    /** Weekdays with at least one enabled slot: the calendar disables the rest (f6 Sundays). */
    val serviceDays: Set<DayOfWeek> = emptySet(),
) {
    /** Everything chosen and nothing in flight: "Solicitar cita" is enabled. */
    val canSubmit: Boolean
        get() = vehicle != null && serviceType != null && date != null && slot != null && !submitting

    /** f6 header "Paso N de 4 · …", derived from what has been chosen so far. */
    val progress: Pair<Int, String>
        get() = when {
            step == AppointmentStep.DONE -> 4 to "Solicitud enviada"
            vehicle == null -> 1 to "Vehículo"
            serviceType == null -> 2 to "Servicio"
            else -> 3 to "Fecha y horario"
        }

    /** First step still missing a choice; earlier choices can change without losing later ones. */
    internal fun firstOpenStep(): AppointmentStep = when {
        vehicle == null -> AppointmentStep.VEHICLE
        serviceType == null -> AppointmentStep.SERVICE
        date == null -> AppointmentStep.DATE
        slot == null -> AppointmentStep.SLOT
        else -> AppointmentStep.SUMMARY
    }
}

/**
 * P6: request an appointment choosing vehicle, service, date and slot. The f6 screen shows
 * them on one page, so a choice can be changed without discarding the others.
 */
class NewAppointmentViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(
        NewAppointmentState(today = today(), lastDate = NewAppointmentValidator.lastSchedulableDate(today(), graph.config))
    )
    val state: StateFlow<NewAppointmentState> = _state.asStateFlow()

    private var slotsJob: Job? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> {
                        val (vehicles, types, slots) = outcome.value
                        it.copy(
                            loading = false,
                            vehicles = vehicles,
                            serviceTypes = types,
                            catalogMissing = types.isEmpty() || slots.isEmpty(),
                            serviceDays = slots.filter { slot -> slot.enabled }.map { slot -> slot.dayOfWeek }.toSet(),
                        )
                    }
                    is Outcome.Failure -> it.copy(loading = false, loadError = outcome.error.userMessage())
                }
            }
        }) {
            Outcome.success(
                Triple(
                    graph.vehicleService.listVisibleTo(actor).sortedBy { it.id },
                    graph.appointmentService.activeServiceTypes(),
                    graph.appointmentService.slotCatalog(),
                )
            )
        }
    }

    fun selectVehicle(vehicle: Vehicle) = _state.update {
        val next = it.copy(vehicle = vehicle, message = null)
        next.copy(step = next.firstOpenStep())
    }

    fun selectServiceType(type: ServiceType) = _state.update {
        val next = it.copy(serviceType = type, message = null)
        next.copy(step = next.firstOpenStep())
    }

    /** Allowed range for the calendar (past dates and dates beyond the horizon are disabled). */
    fun isSchedulable(date: LocalDate): Boolean =
        NewAppointmentValidator.isSchedulable(date, _state.value.today, graph.config)

    /** Selectable in the f6 calendar: inside the range and on a day the shop attends. */
    fun isSelectableDay(date: LocalDate): Boolean = isSchedulable(date) && date.dayOfWeek in _state.value.serviceDays

    fun selectDate(date: LocalDate) {
        val error = NewAppointmentValidator.dateError(date, _state.value.today, graph.config)
        if (error != null) {
            _state.update { it.copy(dateError = error, message = null) }
            return
        }
        _state.update { it.copy(date = date, slot = null, step = AppointmentStep.SLOT, message = null, dateError = null, slotError = null) }
        loadSlots(date)
    }

    fun selectSlot(option: SlotOption) {
        if (!option.selectable) return
        _state.update { it.copy(slot = option, step = AppointmentStep.SUMMARY, message = null, slotError = null) }
    }

    fun back() = _state.update {
        val previous = when (it.step) {
            AppointmentStep.VEHICLE, AppointmentStep.DONE -> it.step
            AppointmentStep.SERVICE -> AppointmentStep.VEHICLE
            AppointmentStep.DATE -> AppointmentStep.SERVICE
            AppointmentStep.SLOT -> AppointmentStep.DATE
            AppointmentStep.SUMMARY -> AppointmentStep.SLOT
        }
        it.copy(step = previous, message = null)
    }

    /** After creating an appointment, start another one reusing the loaded catalogs. */
    fun startOver() = _state.update {
        it.copy(
            step = AppointmentStep.VEHICLE, vehicle = null, serviceType = null, date = null,
            slots = emptyList(), slot = null, created = null, message = null,
            dateError = null, slotError = null,
        )
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val missing = when {
            current.vehicle == null -> "Seleccione el vehículo."
            current.serviceType == null -> "Seleccione el servicio."
            else -> null
        }
        val dateError = if (missing == null) {
            NewAppointmentValidator.dateError(current.date, current.today, graph.config)
        } else {
            null
        }
        val slotError = if (missing == null && dateError == null) {
            NewAppointmentValidator.slotError(current.slot != null)
        } else {
            null
        }
        if (missing != null || dateError != null || slotError != null) {
            _state.update { it.copy(message = missing, dateError = dateError, slotError = slotError) }
            return
        }
        val vehicle = current.vehicle ?: return
        val type = current.serviceType ?: return
        val date = current.date ?: return
        val slot = current.slot ?: return
        _state.update { it.copy(submitting = true, message = null, dateError = null, slotError = null) }
        run({ outcome ->
            when (outcome) {
                is Outcome.Success -> _state.update {
                    it.copy(submitting = false, created = outcome.value, step = AppointmentStep.DONE)
                }
                is Outcome.Failure -> {
                    val error = outcome.error
                    if (error is DomainError.SlotUnavailable) {
                        // Someone took the slot meanwhile: pick again from the refreshed grid.
                        _state.update {
                            it.copy(submitting = false, slot = null, step = AppointmentStep.SLOT, message = error.message)
                        }
                        loadSlots(date)
                    } else {
                        _state.update { it.copy(submitting = false, message = error.userMessage()) }
                    }
                }
            }
        }) {
            graph.appointmentService.request(actor, vehicle.id, type.id, date, slot.slotId, today(), now())
        }
    }

    private fun loadSlots(date: LocalDate) {
        slotsJob?.cancel()
        _state.update { it.copy(slotsLoading = true, slots = emptyList()) }
        slotsJob = run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(slotsLoading = false, slots = outcome.value)
                    is Outcome.Failure -> it.copy(slotsLoading = false, message = outcome.error.userMessage())
                }
            }
        }) {
            Outcome.success(graph.slotOptions(graph.appointmentService.slotsFor(date, _state.value.today)))
        }
    }
}

/** "Lleno" only when capacity is exhausted; other reasons are shown as they are. */
internal fun AppGraph.slotOptions(availability: List<SlotAvailability>): List<SlotOption> =
    availability.map { SlotOption(it, unavailableLabel(it.result)) }

private fun unavailableLabel(result: AvailabilityResult): String? = when (result) {
    AvailabilityResult.Available -> null
    is AvailabilityResult.Unavailable ->
        if (result.reason == UnavailabilityReason.CAPACITY_REACHED) "Lleno" else result.reason.label
}
