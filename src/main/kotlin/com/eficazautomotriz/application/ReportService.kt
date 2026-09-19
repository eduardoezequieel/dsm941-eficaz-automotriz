package com.eficazautomotriz.application

import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.data.ServiceOrderRepository
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDate

/** Una fila de reporte: etiqueta, conteo y porcentaje sobre el total del periodo. */
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
 * Construye los agregados de los reportes. Devuelve estructuras de datos:
 * el dibujo de barras y tablas es responsabilidad de la capa de consola.
 */
class ReportService(
    private val appointments: AppointmentRepository,
    private val serviceOrders: ServiceOrderRepository,
    private val serviceTypes: ServiceTypeRepository,
    private val vehicles: VehicleRepository,
    private val maintenanceService: MaintenanceService,
) {

    /** Reporte 1: citas por estado dentro del periodo, contadas por su fecha de atencion. */
    fun appointmentsByStatus(from: LocalDate, to: LocalDate): PeriodReport {
        val inPeriod = appointments.findByDateRange(from, to)
        val counts = inPeriod.groupingBy { it.status }.eachCount()
        return buildReport("CITAS POR ESTADO", from, to, inPeriod.size) {
            AppointmentStatus.entries
                .mapNotNull { status -> counts[status]?.let { status.label to it } }
        }
    }

    /** Reporte 2: solo ordenes finalizadas dentro del periodo, agrupadas por tipo de servicio. */
    fun completedServicesByType(from: LocalDate, to: LocalDate): PeriodReport {
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

    /** Resumen general del sistema, siempre calculado sobre el estado actual de los datos. */
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
