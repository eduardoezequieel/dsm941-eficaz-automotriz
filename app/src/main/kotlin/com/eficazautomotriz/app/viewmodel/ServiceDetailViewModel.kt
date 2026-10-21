package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            val detail = when (val outcome = graph.serviceOrderService.detailOf(actor, orderId)) {
                is Outcome.Success -> outcome.value
                is Outcome.Failure -> return@run outcome
            }
            val vehicle = when (val found = graph.vehicleService.findAccessible(actor, detail.order.vehicleId)) {
                is Outcome.Success -> found.value
                is Outcome.Failure -> return@run found
            }
            val appointment = detail.order.appointmentId?.let { id ->
                graph.appointmentService.listVisibleTo(actor).find { it.id == id }
            }
            val appointmentRow = appointment?.let { graph.appointmentRows(listOf(it), listOf(vehicle)).first() }
            Outcome.success(
                ServiceDetail(
                    order = graph.serviceOrderRows(listOf(detail)).first(),
                    vehicle = vehicle,
                    appointment = appointmentRow,
                ),
            )
        }
    }
}
