package com.eficazautomotriz.application

import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.data.ServiceOrderRepository
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDate

/** One report row: label, count, and percentage of the period total. */
data class ReportRow(
    val label: String,
    val count: Int,
    val percentage: Double,
)

data class PeriodReport(
    val title: String,
    val from: LocalDate,
    val to: LocalDate,
    val rows: List<ReportRow>,
    val total: Int,
)

data class SystemSummary(
    val totalVehicles: Int,
    val appointmentsByStatus: Map<AppointmentStatus, Int>,
    val serviceOrdersByStatus: Map<ServiceStatus, Int>,
    val vehiclesByMaintenanceStatus: Map<MaintenanceStatus, Int>,
)

/**
 * Builds the report aggregates. It returns data structures:
 * drawing bars and tables is the console layer's responsibility.
 */
class ReportService(
    private val appointments: AppointmentRepository,
    private val serviceOrders: ServiceOrderRepository,
    private val serviceTypes: ServiceTypeRepository,
    private val vehicles: VehicleRepository,
    private val maintenanceService: MaintenanceService,
) {

    /** Report 1: appointments by status inside the period, counted by their service date. */
    fun appointmentsByStatus(from: LocalDate, to: LocalDate): PeriodReport {
        if (from.isAfter(to)) return PeriodReport("CITAS POR ESTADO", from, to, emptyList(), 0)
        val inPeriod = appointments.findByDateRange(from, to)
        val counts = inPeriod.groupingBy { it.status }.eachCount()
        return buildReport("CITAS POR ESTADO", from, to, inPeriod.size) {
            AppointmentStatus.entries
                .mapNotNull { status -> counts[status]?.let { status.label to it } }
        }
    }

    /** Report 2: only orders completed inside the period, grouped by service type. */
    fun completedServicesByType(from: LocalDate, to: LocalDate): PeriodReport {
        if (from.isAfter(to)) return PeriodReport("SERVICIOS FINALIZADOS POR TIPO", from, to, emptyList(), 0)
        val typeNames = serviceTypes.findAll().associate { it.id to it.name }
        val completed = serviceOrders.findByStatus(ServiceStatus.COMPLETED)
            .filter { order -> order.closedAt?.toLocalDate()?.let { it >= from && it <= to } == true }
        val counts = completed.groupingBy { it.serviceTypeId }.eachCount()
        return buildReport("SERVICIOS FINALIZADOS POR TIPO", from, to, completed.size) {
            counts.entries
                .sortedByDescending { it.value }
                .map { (typeId, count) -> (typeNames[typeId] ?: typeId) to count }
        }
    }

    /** Overall system summary, always computed from the current state of the data. */
    fun systemSummary(): SystemSummary {
        val appointmentCounts = appointments.findAll().groupingBy { it.status }.eachCount()
        val serviceCounts = serviceOrders.findAll().groupingBy { it.status }.eachCount()
        return SystemSummary(
            totalVehicles = vehicles.findAll().size,
            appointmentsByStatus = AppointmentStatus.entries.associateWith { appointmentCounts[it] ?: 0 },
            serviceOrdersByStatus = ServiceStatus.entries.associateWith { serviceCounts[it] ?: 0 },
            vehiclesByMaintenanceStatus = maintenanceService.statusCounts(),
        )
    }

    private fun buildReport(
        title: String,
        from: LocalDate,
        to: LocalDate,
        total: Int,
        entries: () -> List<Pair<String, Int>>,
    ): PeriodReport {
        val rows = entries()
            .sortedByDescending { it.second }
            .map { (label, count) -> ReportRow(label, count, percentageOf(count, total)) }
        return PeriodReport(title, from, to, rows, total)
    }

    private fun percentageOf(count: Int, total: Int): Double =
        if (total == 0) 0.0 else count * 100.0 / total
}
