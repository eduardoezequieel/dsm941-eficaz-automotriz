package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.SlotOption
import com.eficazautomotriz.app.viewmodel.appointmentRows
import com.eficazautomotriz.app.viewmodel.slotOptions
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.app.viewmodel.validation.NewAppointmentValidator
import com.eficazautomotriz.app.viewmodel.validation.ReasonValidator
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Reschedule panel state: date, that day's slots and the reason. */
data class RescheduleState(
    val date: LocalDate? = null,
    val slots: List<SlotOption> = emptyList(),
    val slotsLoading: Boolean = false,
    val slotId: String? = null,
    val reason: String = "",
    val reasonError: String? = null,
)

data class StaffAppointmentState(
    val row: UiState<AppointmentRow> = UiState.Loading,
    val busy: Boolean = false,
    val message: String? = null,
    val reschedule: RescheduleState? = null,
) {
    val status: AppointmentStatus? get() = (row as? UiState.Ready)?.data?.appointment?.status

    /** Pending and rescheduled only. A confirmed appointment is not final, but the decision is done. */
    val canDecide: Boolean get() = status?.let(::canDecideAppointment) == true
    val canCreateOrder: Boolean get() = status == AppointmentStatus.CONFIRMED
}

/** Staff appointment detail: confirm, reject with a reason, or reschedule. */
class StaffAppointmentViewModel(
    graph: AppGraph,
    private val actor: User,
    private val appointmentId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(StaffAppointmentState())
    val state: StateFlow<StaffAppointmentState> = _state.asStateFlow()

    /** Fixed when the screen opens; the DatePicker range and validation both use it. */
    val today: LocalDate = today()

    /** Only the slots of the latest chosen date may be shown. */
    private var slotsJob: Job? = null

    /** A slower reload must not restore the status from before confirm, reject or reschedule. */
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = run({ outcome ->
            _state.update {
                it.copy(
                    row = when (outcome) {
                        is Outcome.Success -> UiState.Ready(outcome.value)
                        is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
                    },
                )
            }
        }) {
            val appointment = graph.appointmentService.listVisibleTo(actor).firstOrNull { it.id == appointmentId }
                ?: return@run Outcome.failure(DomainError.NotFound("cita", appointmentId))
            Outcome.success(
                graph.appointmentRows(listOf(appointment), graph.vehicleService.listVisibleTo(actor), withClientNames = true).first()
            )
        }
    }

    fun confirm() = act("Cita confirmada.") { graph.appointmentService.confirm(actor, appointmentId, today()) }

    /** Returns false when the reason is invalid (the screen keeps the dialog open). */
    fun reject(reason: String): Boolean {
        ReasonValidator.error(reason)?.let {
            _state.update { s -> s.copy(message = it) }
            return false
        }
        act("Cita rechazada.") { graph.appointmentService.reject(actor, appointmentId, reason.trim()) }
        return true
    }

    fun startReschedule() = _state.update { it.copy(reschedule = RescheduleState(), message = null) }

    fun cancelReschedule() = _state.update { it.copy(reschedule = null) }

    fun isSchedulable(date: LocalDate): Boolean = NewAppointmentValidator.isSchedulable(date, today, graph.config)

    fun lastSchedulableDate(): LocalDate = NewAppointmentValidator.lastSchedulableDate(today, graph.config)

    fun rescheduleDate(date: LocalDate) {
        NewAppointmentValidator.dateError(date, today, graph.config)?.let {
            _state.update { s -> s.copy(message = it) }
            return
        }
        updateReschedule { it.copy(date = date, slotId = null, slots = emptyList(), slotsLoading = true) }
        slotsJob?.cancel()
        slotsJob = run({ outcome ->
            when (outcome) {
                is Outcome.Success -> updateReschedule { it.copy(slots = outcome.value, slotsLoading = false) }
                is Outcome.Failure -> {
                    updateReschedule { it.copy(slotsLoading = false) }
                    _state.update { it.copy(message = outcome.error.userMessage()) }
                }
            }
        }) {
            // The appointment itself does not count against the capacity of its current slot.
            Outcome.success(graph.slotOptions(graph.appointmentService.slotsFor(date, today, excludingAppointmentId = appointmentId)))
        }
    }

    fun rescheduleSlot(option: SlotOption) {
        if (option.selectable) updateReschedule { it.copy(slotId = option.slotId) }
    }

    fun rescheduleReason(reason: String) = updateReschedule { it.copy(reason = reason, reasonError = null) }

    fun submitReschedule() {
        val form = _state.value.reschedule ?: return
        val date = form.date ?: return
        val slotId = form.slotId ?: return
        ReasonValidator.error(form.reason)?.let { error ->
            updateReschedule { it.copy(reasonError = error) }
            return
        }
        act("Cita reprogramada.", closeReschedule = true) {
            graph.appointmentService.reschedule(actor, appointmentId, date, slotId, form.reason.trim(), today())
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun updateReschedule(transform: (RescheduleState) -> RescheduleState) =
        _state.update { s -> s.copy(reschedule = s.reschedule?.let(transform)) }

    private fun act(success: String, closeReschedule: Boolean = false, block: () -> Outcome<Appointment>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        loadJob?.cancel()
        run({ outcome ->
            _state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.withDecision(outcome.value).copy(
                        busy = false,
                        message = success,
                        reschedule = if (closeReschedule) null else current.reschedule,
                    )
                    is Outcome.Failure -> current.copy(busy = false, message = outcome.error.userMessage())
                }
            }
            if (outcome is Outcome.Success) load()
        }, block)
    }

    /** The buttons read this status immediately; [load] then refreshes the slot and the client name. */
    private fun StaffAppointmentState.withDecision(appointment: Appointment): StaffAppointmentState {
        val ready = row as? UiState.Ready ?: return this
        return copy(row = UiState.Ready(ready.data.copy(appointment = appointment)))
    }
}
