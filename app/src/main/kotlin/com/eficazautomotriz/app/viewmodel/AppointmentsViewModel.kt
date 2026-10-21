package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The two P5 tabs. "Próximas" holds every card f5 draws there, including a rejection
 * (it is final, but the mockup keeps it on this tab). "Historial" holds attended and
 * cancelled appointments, most recent first.
 */
data class ClientAppointments(
    val upcoming: List<AppointmentRow>,
    val history: List<AppointmentRow>,
)

/** Open appointments by date, then rejections; attended and cancelled go to history. */
internal fun splitClientAppointments(rows: List<AppointmentRow>): ClientAppointments {
    val (history, upcoming) = rows.partition {
        val status = it.appointment.status
        status == AppointmentStatus.ATTENDED || status == AppointmentStatus.CANCELLED
    }
    return ClientAppointments(
        upcoming = upcoming.sortedWith(
            compareBy<AppointmentRow> { it.appointment.status == AppointmentStatus.REJECTED }
                .thenBy { it.appointment.date }
                .thenBy { it.appointment.id },
        ),
        history = history.sortedByDescending { it.appointment.date },
    )
}

/** P5: the client's appointments split into upcoming/history, cancellation and the answer to a reschedule. */
class AppointmentsViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<ClientAppointments>>(UiState.Loading)
    val state: StateFlow<UiState<ClientAppointments>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Called when the screen resumes; existing data is refreshed without flickering. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            val rows = graph.appointmentRows(
                graph.appointmentService.listVisibleTo(actor),
                graph.vehicleService.listVisibleTo(actor),
            )
            Outcome.success(splitClientAppointments(rows))
        }
    }

    fun cancel(appointmentId: String) =
        act("Cita cancelada.") { graph.appointmentService.cancel(actor, appointmentId) }

    fun acceptReschedule(appointmentId: String) =
        act("Nueva fecha aceptada. La cita quedó confirmada.") {
            graph.appointmentService.acceptReschedule(actor, appointmentId)
        }

    fun declineReschedule(appointmentId: String) =
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
