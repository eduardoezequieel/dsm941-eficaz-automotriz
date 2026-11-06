package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Detail of a client appointment (route client/appointments/{id}): cancel, or answer a reschedule. */
class AppointmentDetailViewModel(
    graph: AppGraph,
    private val actor: User,
    private val appointmentId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<AppointmentRow>>(UiState.Loading)
    val state: StateFlow<UiState<AppointmentRow>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            // listVisibleTo already filters by owner: someone else's appointment does not show up.
            val appointment = graph.appointmentService.listVisibleTo(actor).firstOrNull { it.id == appointmentId }
                ?: return@run Outcome.failure(DomainError.NotFound("cita", appointmentId))
            Outcome.success(graph.appointmentRows(listOf(appointment), graph.vehicleService.listVisibleTo(actor)).first())
        }
    }

    fun cancel() = act("Cita cancelada.") { graph.appointmentService.cancel(actor, appointmentId) }

    fun acceptReschedule() =
        act("Nueva fecha aceptada. La cita quedó confirmada.") {
            graph.appointmentService.acceptReschedule(actor, appointmentId)
        }

    fun declineReschedule() =
        act("Reprogramación rechazada. La cita quedó cancelada.") {
            graph.appointmentService.declineReschedule(actor, appointmentId)
        }

    fun consumeMessage() {
        _message.value = null
    }

    private fun act(successMessage: String, call: () -> Outcome<Appointment>) {
        run({ outcome ->
            when (outcome) {
                is Outcome.Success -> {
                    _message.value = successMessage
                    load()
                }
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }, call)
    }
}
