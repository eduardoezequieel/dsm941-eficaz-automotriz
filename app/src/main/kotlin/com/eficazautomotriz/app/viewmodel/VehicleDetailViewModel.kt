package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.application.VehicleMaintenanceReport
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P4 payload: the maintenance report, the three most recent orders ("Mantenimiento" tab), the
 * whole history ("Historial" tab) and the vehicle's appointments, newest first ("Citas" tab).
 */
data class VehicleDetail(
    val report: VehicleMaintenanceReport,
    val recentServices: List<ServiceOrderRow>,
    val history: List<ServiceOrderRow> = recentServices,
    val appointments: List<AppointmentRow> = emptyList(),
)

/** P4: vehicle data, maintenance per type, services, appointments and vehicle removal. */
class VehicleDetailViewModel(
    graph: AppGraph,
    private val actor: User,
    private val vehicleId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<VehicleDetail>>(UiState.Loading)
    val state: StateFlow<UiState<VehicleDetail>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** true once the vehicle was deleted: the screen goes back to the list. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Called by the screen on resume: coming back from "Actualizar km" changes the status. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            val report = when (val outcome = graph.maintenanceService.reportFor(actor, vehicleId)) {
                is Outcome.Success -> outcome.value
                is Outcome.Failure -> return@run outcome
            }
            val history = when (val outcome = graph.serviceOrderService.historyFor(actor, vehicleId)) {
                is Outcome.Success -> graph.serviceOrderRows(outcome.value)
                is Outcome.Failure -> return@run outcome
            }
            val appointments = graph.appointmentRows(
                graph.appointmentService.listVisibleTo(actor).filter { it.vehicleId == vehicleId },
                listOf(report.vehicle),
            ).sortedByDescending { it.appointment.date }
            Outcome.success(VehicleDetail(report, history.take(3), history, appointments))
        }
    }

    fun delete() {
        run({ outcome ->
            when (outcome) {
                is Outcome.Success -> {
                    _message.value = "Vehículo ${Formats.plate(outcome.value.plate)} eliminado."
                    _deleted.value = true
                }
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }) { graph.vehicleService.delete(actor, vehicleId) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
