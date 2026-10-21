package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.application.VehicleMaintenanceReport
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

data class VehicleSummary(val vehicle: Vehicle, val status: MaintenanceStatus)

/** The single most urgent preventive line across the client's vehicles. */
data class MaintenanceAlert(
    val vehicle: Vehicle,
    val typeName: String,
    val status: MaintenanceStatus,
    val recommendedMileage: Int,
    val currentMileage: Int,
)

/** Latest completed order, with the vehicle it belongs to. */
data class LastService(
    val row: ServiceOrderRow,
    val vehicle: Vehicle,
)

data class ClientHome(
    val user: User,
    val vehicles: List<VehicleSummary>,
    val nextAppointment: AppointmentRow?,
    val maintenanceAlert: MaintenanceAlert?,
    val lastService: LastService?,
)

/**
 * Home alert (f2). Picks the due-soon line whose recommended mileage is nearest
 * in km (smallest non-negative remainder). An overdue line does not outrank it:
 * f3 can show the Accent as "Vencido" while f2 still alerts on the Frontier.
 * With no due-soon line, the overdue line closest to its mark (least overrun) wins.
 */
internal fun mostSevereMaintenanceAlert(reports: List<VehicleMaintenanceReport>): MaintenanceAlert? {
    val candidates = reports.flatMap { report ->
        report.lines.mapNotNull { line ->
            val recommended = line.evaluation.recommendedMileage ?: return@mapNotNull null
            val status = line.evaluation.status
            if (status != MaintenanceStatus.OVERDUE && status != MaintenanceStatus.DUE_SOON) {
                return@mapNotNull null
            }
            MaintenanceAlert(
                vehicle = report.vehicle,
                typeName = line.type.name,
                status = status,
                recommendedMileage = recommended,
                currentMileage = report.vehicle.currentMileage,
            )
        }
    }
    val dueSoon = candidates.filter { it.status == MaintenanceStatus.DUE_SOON }
    val pool = dueSoon.ifEmpty { candidates }
    return pool.minByOrNull { it.recommendedMileage - it.currentMileage }
}

/** P2: next appointment, the sharpest maintenance alert, and the last closed service. */
class ClientHomeViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<ClientHome>>(UiState.Loading)
    val state: StateFlow<UiState<ClientHome>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Called when the screen resumes; existing data is refreshed without flickering. */
    fun load() {
        if (_state.value !is UiState.Ready) _state.value = UiState.Loading
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            val today = today()
            val vehicles = graph.vehicleService.listVisibleTo(actor).sortedBy { it.id }
            val reports = vehicles.map { vehicle ->
                when (val report = graph.maintenanceService.reportFor(actor, vehicle.id)) {
                    is Outcome.Success -> report.value
                    is Outcome.Failure -> return@run report
                }
            }
            val upcoming = graph.appointmentService.listVisibleTo(actor)
                .filter { !it.status.isFinal && !it.date.isBefore(today) }
            val nextAppointment = graph.appointmentRows(upcoming, vehicles)
                .sortedWith(compareBy({ it.appointment.date }, { it.slot?.startTime ?: LocalTime.MAX }))
                .firstOrNull()

            var newest: Pair<ServiceOrderDetail, Vehicle>? = null
            for (vehicle in vehicles) {
                when (val history = graph.serviceOrderService.historyFor(actor, vehicle.id)) {
                    is Outcome.Failure -> return@run history
                    is Outcome.Success -> history.value
                        .filter { it.order.status == ServiceStatus.COMPLETED }
                        .maxByOrNull { it.order.closedAt ?: it.order.receivedAt }
                        ?.let { detail ->
                            val moment = detail.order.closedAt ?: detail.order.receivedAt
                            val current = newest
                            if (current == null || moment > (current.first.order.closedAt ?: current.first.order.receivedAt)) {
                                newest = detail to vehicle
                            }
                        }
                }
            }
            val lastService = newest?.let { (detail, vehicle) ->
                LastService(graph.serviceOrderRows(listOf(detail)).first(), vehicle)
            }
            Outcome.success(
                ClientHome(
                    user = actor,
                    vehicles = reports.map { VehicleSummary(it.vehicle, it.overallStatus) },
                    nextAppointment = nextAppointment,
                    maintenanceAlert = mostSevereMaintenanceAlert(reports),
                    lastService = lastService,
                )
            )
        }
    }

    /** Cancellation rules stay in [com.eficazautomotriz.application.AppointmentService.cancel]. */
    fun cancel(appointmentId: String) {
        run({ outcome ->
            when (outcome) {
                is Outcome.Success -> {
                    _message.value = "Cita cancelada."
                    load()
                }
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }) { graph.appointmentService.cancel(actor, appointmentId) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
