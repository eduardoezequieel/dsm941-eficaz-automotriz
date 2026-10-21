package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** P3: the client's vehicles (registration order, by id) with their maintenance label. Reloaded on resume. */
class VehiclesViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<List<VehicleSummary>>>(UiState.Loading)
    val state: StateFlow<UiState<List<VehicleSummary>>> = _state.asStateFlow()

    /** Called when the screen resumes; existing data is refreshed without flickering. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            Outcome.success(
                graph.vehicleService.listVisibleTo(actor).sortedBy { it.id }.map { vehicle ->
                    VehicleSummary(vehicle, graph.maintenanceService.buildReport(vehicle).overallStatus)
                }
            )
        }
    }
}
