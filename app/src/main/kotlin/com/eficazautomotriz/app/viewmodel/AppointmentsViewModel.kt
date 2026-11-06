package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

    /** One listener per visit. A new [load] replaces it (resume, retry, or after a write). */
    private var observeJob: Job? = null

    /**
     * Called when the screen resumes. Subscribes to this client's appointments.
     * Row building (vehicles, names) runs upstream of [catch], so a denied or
     * offline read after a snapshot does not escape the collector. [catch]
     * rethrows [kotlinx.coroutines.CancellationException]. A failure keeps the
     * last ready list and surfaces the system message; otherwise it publishes
     * [UiState.Error].
     */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            graph.repositories.appointments.observeByClient(actor.id)
                .map { appointments ->
                    val rows = graph.appointmentRows(
                        appointments,
                        graph.vehicleService.listVisibleTo(actor),
                    )
                    splitClientAppointments(rows)
                }
                .flowOn(graph.io)
                .catch { error ->
                    if (error is CancellationException) throw error
                    onListenerError(error)
                }
                .collect { ready -> _state.value = UiState.Ready(ready) }
        }
    }

    private fun onListenerError(error: Throwable) {
        graph.errorLogger.error(
            this::class.simpleName ?: "ViewModel",
            error::class.simpleName ?: "Exception",
            error.message.orEmpty(),
        )
        val text = DomainError.NotFound(SYSTEM_ENTITY, error.message ?: "sin detalle").userMessage()
        if (_state.value is UiState.Ready) _message.value = text
        else _state.value = UiState.Error(text)
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
