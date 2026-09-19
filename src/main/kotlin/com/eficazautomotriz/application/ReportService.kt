package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.ServiceStatus
import com.eficazautomotriz.domain.repository.AppointmentRepository
import com.eficazautomotriz.domain.repository.ServiceOrderRepository
import com.eficazautomotriz.domain.repository.VehicleRepository
import java.time.LocalDate

data class ReportRow(
    val label: String,
    val quantity: Int,
    val percentage: Double
)

data class PeriodReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val total: Int,
    val rows: List<ReportRow>
)

data class SystemSummary(
    val totalVehicles: Int,
    val appointmentsByStatus: List<ReportRow>,
    val ordersByStatus: List<ReportRow>,
    val vehiclesByMaintenanceStatus: List<ReportRow>
)

class ReportService(
    private val vehicleRepository: VehicleRepository,
    private val appointmentRepository: AppointmentRepository,
    private val serviceOrderRepository: ServiceOrderRepository,
    private val maintenanceService: MaintenanceService
) {
    fun appointmentsByStatusReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Outcome<PeriodReport> {
        val periodError = validatePeriod(startDate, endDate)
        if (periodError != null) {
            return Outcome.Failure(periodError)
        }

        val appointments = appointmentRepository.findByPeriod(startDate, endDate)
        return Outcome.Success(
            PeriodReport(
                startDate = startDate,
                endDate = endDate,
                total = appointments.size,
                rows = rowsFrom(appointments) { appointment -> appointment.status.toString() }
            )
        )
    }

    fun completedOrdersByServiceTypeReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Outcome<PeriodReport> {
        val periodError = validatePeriod(startDate, endDate)
        if (periodError != null) {
            return Outcome.Failure(periodError)
        }

        val completedOrders = serviceOrderRepository.findByPeriod(startDate, endDate)
            .filter { order -> order.status == ServiceStatus.COMPLETED }

        return Outcome.Success(
            PeriodReport(
                startDate = startDate,
                endDate = endDate,
                total = completedOrders.size,
                rows = rowsFrom(completedOrders) { order -> order.serviceType.toString() }
            )
        )
    }

    fun systemSummary(): Outcome<SystemSummary> {
        val vehicles = vehicleRepository.findAll()
        val appointments = appointmentRepository.findAll()
        val orders = serviceOrderRepository.findAll()

        val maintenanceCounts = when (val result = maintenanceService.countVehiclesByMaintenanceStatus()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }

        return Outcome.Success(
            SystemSummary(
                totalVehicles = vehicles.size,
                appointmentsByStatus = rowsFrom(appointments) { appointment -> appointment.status.toString() },
                ordersByStatus = rowsFrom(orders) { order -> order.status.toString() },
                vehiclesByMaintenanceStatus = rowsFromCounts(
                    counts = maintenanceCounts.mapKeys { entry -> entry.key.toString() },
                    total = vehicles.size
                )
            )
        )
    }

    private fun validatePeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): DomainError? {
        return if (startDate.isAfter(endDate)) {
            DomainError.OperationNotAllowed("La fecha inicial no puede ser posterior a la fecha final.")
        } else {
            null
        }
    }

    private fun <T> rowsFrom(
        items: List<T>,
        labelSelector: (T) -> String
    ): List<ReportRow> {
        val counts = items.groupingBy(labelSelector).eachCount()
        return rowsFromCounts(counts, items.size)
    }

    private fun rowsFromCounts(
        counts: Map<String, Int>,
        total: Int
    ): List<ReportRow> {
        return counts.map { (label, quantity) ->
            ReportRow(
                label = label,
                quantity = quantity,
                percentage = percentage(quantity, total)
            )
        }
    }

    private fun percentage(
        quantity: Int,
        total: Int
    ): Double {
        return if (total == 0) 0.0 else quantity * 100.0 / total
    }
}
