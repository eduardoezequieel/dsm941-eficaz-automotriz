package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One service of the combined history, with the vehicle it belongs to. */
data class ClientHistoryEntry(
    val row: ServiceOrderRow,
    val vehicle: Vehicle,
)

/**
 * Merges the per-vehicle histories into one list, most recent reception first.
 * Pure so it can be unit-tested without a graph.
 */
fun mergeClientHistory(
    vehicles: List<Vehicle>,
    historyByVehicle: Map<String, List<ServiceOrderRow>>,
): List<ClientHistoryEntry> =
    vehicles
        .flatMap { vehicle -> historyByVehicle[vehicle.id].orEmpty().map { ClientHistoryEntry(it, vehicle) } }
        .sortedByDescending { it.row.detail.order.receivedAt }

/** Bottom bar "Historial": every service order of every vehicle of the client. */
class ClientHistoryViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<List<ClientHistoryEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ClientHistoryEntry>>> = _state.asStateFlow()

    /** Called when the screen resumes; existing data is refreshed without flickering. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            val vehicles = graph.vehicleService.listVisibleTo(actor)
            val histories = mutableMapOf<String, List<ServiceOrderRow>>()
            for (vehicle in vehicles) {
                when (val history: Outcome<List<ServiceOrderDetail>> = graph.serviceOrderService.historyFor(actor, vehicle.id)) {
                    is Outcome.Success -> histories[vehicle.id] = graph.serviceOrderRows(history.value)
                    is Outcome.Failure -> return@run history
                }
            }
            Outcome.success(mergeClientHistory(vehicles, histories))
        }
    }
}
