package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.StaffOrderRow
import com.eficazautomotriz.app.viewmodel.staffOrderRows
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Orders grouped for the "Servicios" tab: in progress first, then received, then completed. */
data class StaffServices(
    val inProgress: List<StaffOrderRow>,
    val received: List<StaffOrderRow>,
    val completed: List<StaffOrderRow>,
) {
    val isEmpty: Boolean get() = inProgress.isEmpty() && received.isEmpty() && completed.isEmpty()
}

/** Pure grouping (keeps the newest-first order of `listAll`), unit-tested. */
fun groupStaffServices(rows: List<StaffOrderRow>): StaffServices = StaffServices(
    inProgress = rows.filter { it.order.status == ServiceStatus.IN_PROGRESS },
    received = rows.filter { it.order.status == ServiceStatus.RECEIVED },
    completed = rows.filter { it.order.status == ServiceStatus.COMPLETED },
)

/** Bottom bar "Servicios" of the staff: every service order of the shop. */
class StaffServicesViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<StaffServices>>(UiState.Loading)
    val state: StateFlow<UiState<StaffServices>> = _state.asStateFlow()

    /** Called when the screen resumes; existing data is refreshed without flickering. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            when (val orders = graph.serviceOrderService.listAll(actor)) {
                is Outcome.Success -> Outcome.success(
                    groupStaffServices(graph.staffOrderRows(orders.value, graph.vehicleService.listVisibleTo(actor))),
                )
                is Outcome.Failure -> orders
            }
        }
    }
}
