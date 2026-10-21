package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.ServiceOrderRow
import com.eficazautomotriz.app.viewmodel.serviceOrderRows
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Staff actions on an order; each one maps to a transition the domain actually allows. */
enum class OrderAction { START, ADD_EVIDENCE, COMPLETE }

/**
 * Mirrors `ServiceTransitionRule` (RECEIVED → IN_PROGRESS → COMPLETED) and
 * `ServiceOrderService.addEvidence` (any order that is not completed).
 */
fun orderActionsFor(status: ServiceStatus): Set<OrderAction> = when (status) {
    ServiceStatus.RECEIVED -> setOf(OrderAction.START, OrderAction.ADD_EVIDENCE)
    ServiceStatus.IN_PROGRESS -> setOf(OrderAction.ADD_EVIDENCE, OrderAction.COMPLETE)
    ServiceStatus.COMPLETED -> emptySet()
}

data class OrderDetail(val row: ServiceOrderRow, val vehicle: Vehicle?) {
    val status: ServiceStatus get() = row.detail.order.status
    val actions: Set<OrderAction> get() = orderActionsFor(status)
}

/** Service order detail for staff: start work, evidence and closing shortcuts. */
class OrderDetailViewModel(
    graph: AppGraph,
    private val actor: User,
    private val orderId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<OrderDetail>>(UiState.Loading)
    val state: StateFlow<UiState<OrderDetail>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** True while the start transition is in flight: a double tap must not send it twice. */
    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()

    /** Called on resume: coming back from evidence or closing changes the order. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            when (val detail = graph.serviceOrderService.detailOf(actor, orderId)) {
                is Outcome.Success -> {
                    val vehicle = (graph.vehicleService.findAccessible(actor, detail.value.order.vehicleId) as? Outcome.Success)?.value
                    Outcome.success(OrderDetail(graph.serviceOrderRows(listOf(detail.value)).first(), vehicle))
                }
                is Outcome.Failure -> detail
            }
        }
    }

    fun start() {
        if (_starting.value) return
        _starting.value = true
        run({ outcome ->
            _starting.value = false
            when (outcome) {
                is Outcome.Success -> {
                    _message.value = "Orden en proceso."
                    load()
                }
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }) { graph.serviceOrderService.advance(actor, orderId, ServiceStatus.IN_PROGRESS, now()) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
