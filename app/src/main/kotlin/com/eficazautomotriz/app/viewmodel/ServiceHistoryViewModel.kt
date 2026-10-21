package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** P7: service history of a vehicle (read-only for the client). */
class ServiceHistoryViewModel(
    graph: AppGraph,
    private val actor: User,
    private val vehicleId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<List<ServiceOrderRow>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ServiceOrderRow>>> = _state.asStateFlow()

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
            when (val history = graph.serviceOrderService.historyFor(actor, vehicleId)) {
                is Outcome.Success -> Outcome.success(graph.serviceOrderRows(history.value))
                is Outcome.Failure -> history
            }
        }
    }
}
