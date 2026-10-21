package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.StaffOrderRow
import com.eficazautomotriz.app.viewmodel.appointmentRows
import com.eficazautomotriz.app.viewmodel.staffOrderRows
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.app.viewmodel.validation.ReasonValidator
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime

data class StaffPanel(
    val pending: List<AppointmentRow>,
    val agenda: List<AppointmentRow>,
    val orders: List<StaffOrderRow>,
    val counts: StaffPanelCounts,
    /** Date the panel was loaded for; cards say "recibido a las 8:30 a.m." for orders of this day. */
    val today: LocalDate,
)

/** P8: pending requests, schedule (from today on) and service orders. */
class StaffPanelViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<StaffPanel>>(UiState.Loading)
    val state: StateFlow<UiState<StaffPanel>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** True while confirm, reject or start is in flight. A second tap must not send it again. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** A reload started before a write must not overwrite the list that write just changed. */
    private var loadJob: Job? = null

    /** Called when the screen resumes; existing data is refreshed without flickering. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        loadJob?.cancel()
        loadJob = run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            val pending = when (val result = graph.appointmentService.listPending(actor)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> return@run result
            }
            val orders = when (val result = graph.serviceOrderService.listAll(actor)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> return@run result
            }
            val today = today()
            val vehicles = graph.vehicleService.listVisibleTo(actor)
            val agendaRows = graph.appointmentRows(
                graph.appointmentService.listVisibleTo(actor).filter { !it.date.isBefore(today) },
                vehicles,
                withClientNames = true,
            ).sortedWith(compareBy({ it.appointment.date }, { it.slot?.startTime ?: LocalTime.MAX }))
            val pendingRows = graph.appointmentRows(pending, vehicles, withClientNames = true)
            val orderRows = graph.staffOrderRows(orders, vehicles)
            Outcome.success(
                StaffPanel(
                    pending = pendingRows,
                    agenda = agendaRows,
                    orders = orderRows,
                    counts = staffPanelCounts(
                        pendingCount = pendingRows.size,
                        agendaDates = agendaRows.map { it.appointment.date },
                        orderStatuses = orderRows.map { it.order.status },
                        today = today,
                    ),
                    today = today,
                ),
            )
        }
    }

    fun confirm(appointmentId: String) = act("Cita confirmada.") {
        graph.appointmentService.confirm(actor, appointmentId, today())
    }

    /** Returns false when the reason is invalid (the screen keeps the dialog open). */
    fun reject(appointmentId: String, reason: String): Boolean {
        if (_busy.value) return false
        ReasonValidator.error(reason)?.let {
            _message.value = it
            return false
        }
        act("Cita rechazada.") { graph.appointmentService.reject(actor, appointmentId, reason.trim()) }
        return true
    }

    /** RECEIVED → IN_PROGRESS. Completing an order stays on the closing screen. */
    fun startOrder(orderId: String) = act("Orden en proceso.") {
        graph.serviceOrderService.advance(actor, orderId, ServiceStatus.IN_PROGRESS, now())
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun <T> act(success: String, block: () -> Outcome<T>) {
        if (_busy.value) return
        _busy.value = true
        loadJob?.cancel()
        run({ outcome ->
            _busy.value = false
            when (outcome) {
                is Outcome.Success -> {
                    _message.value = success
                    load()
                }
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }, block)
    }
}
