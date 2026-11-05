package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** P7: the order, its vehicle, and the linked appointment when there is one. */
data class ServiceDetail(
    val order: ServiceOrderRow,
    val vehicle: Vehicle,
    val appointment: AppointmentRow?,
)

/** P7: service order detail with evidence and the associated appointment. */
class ServiceDetailViewModel(
    graph: AppGraph,
    private val actor: User,
    private val orderId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<ServiceDetail>>(UiState.Loading)
    val state: StateFlow<UiState<ServiceDetail>> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        load()
    }

    /**
     * Subscribes to the order document and to its evidence. Building the detail
     * (vehicle, linked appointment) is upstream of [catch], so a denied read after
     * a snapshot does not escape the collector. [catch] rethrows
     * [kotlinx.coroutines.CancellationException]. This screen has no snackbar, so a
     * failure keeps the last ready detail, or publishes [UiState.Error] when there
     * is none yet. The linked appointment is still a one-shot read.
     */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                graph.repositories.serviceOrders.observeById(orderId),
                graph.repositories.evidences.observeByOrder(orderId),
            ) { orders, evidences -> orders to evidences }
                .map { (orders, evidences) ->
                    val order = orders.firstOrNull()
                        ?: return@map Outcome.failure(DomainError.NotFound("orden de servicio", orderId))
                    build(order, evidences)
                }
                .flowOn(graph.io)
                .catch { error ->
                    if (error is CancellationException) throw error
                    onListenerError(error)
                }
                .collect { built ->
                    when (built) {
                        is Outcome.Success -> _state.value = UiState.Ready(built.value)
                        is Outcome.Failure -> _state.value = UiState.Error(built.error.userMessage())
                    }
                }
        }
    }

    private fun build(order: ServiceOrder, evidences: List<Evidence>): Outcome<ServiceDetail> {
        val vehicle = when (val found = graph.vehicleService.findAccessible(actor, order.vehicleId)) {
            is Outcome.Success -> found.value
            is Outcome.Failure -> return found
        }
        val detail = ServiceOrderDetail(order, evidences)
        val appointment = order.appointmentId?.let { id ->
            graph.appointmentService.listVisibleTo(actor).find { it.id == id }
        }
        val appointmentRow = appointment?.let { graph.appointmentRows(listOf(it), listOf(vehicle)).first() }
        return Outcome.success(
            ServiceDetail(
                order = graph.serviceOrderRows(listOf(detail)).first(),
                vehicle = vehicle,
                appointment = appointmentRow,
            ),
        )
    }

    private fun onListenerError(error: Throwable) {
        graph.errorLogger.error(
            this::class.simpleName ?: "ViewModel",
            error::class.simpleName ?: "Exception",
            error.message.orEmpty(),
        )
        if (_state.value !is UiState.Ready) {
            _state.value = UiState.Error(
                DomainError.NotFound(SYSTEM_ENTITY, error.message ?: "sin detalle").userMessage(),
            )
        }
    }
}