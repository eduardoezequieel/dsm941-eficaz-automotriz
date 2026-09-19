package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.AppointmentStatus
import com.eficazautomotriz.domain.model.MaintenanceStatus
import com.eficazautomotriz.domain.model.ServiceStatus
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReportServiceTest {
    private val startDate = LocalDate.of(2026, 9, 1)
    private val endDate = LocalDate.of(2026, 9, 30)

    @Test
    fun `agrupa citas por estado dentro de un periodo`() {
        val env = TestEnvironment(
            appointmentsInPeriod = mutableListOf(
                TestEnvironment.appointment(AppointmentStatus.SCHEDULED),
                TestEnvironment.appointment(AppointmentStatus.SCHEDULED),
                TestEnvironment.appointment(AppointmentStatus.CANCELLED)
            )
        )

        val report = successValue(env.reportService.appointmentsByStatusReport(startDate, endDate))

        assertEquals(3, report.total)
        assertEquals(2, row(report.rows, AppointmentStatus.SCHEDULED.toString()).quantity)
        assertEquals(1, row(report.rows, AppointmentStatus.CANCELLED.toString()).quantity)
    }

    @Test
    fun `agrupa ordenes completadas por tipo de servicio`() {
        val env = TestEnvironment(
            ordersInPeriod = mutableListOf(
                TestEnvironment.serviceOrder(ServiceStatus.COMPLETED, "preventivo"),
                TestEnvironment.serviceOrder(ServiceStatus.COMPLETED, "correctivo"),
                TestEnvironment.serviceOrder(ServiceStatus.IN_PROGRESS, "preventivo")
            )
        )

        val report = successValue(env.reportService.completedOrdersByServiceTypeReport(startDate, endDate))

        assertEquals(2, report.total)
        assertEquals(1, row(report.rows, "preventivo").quantity)
        assertEquals(1, row(report.rows, "correctivo").quantity)
    }

    @Test
    fun `calcula cantidades y porcentajes`() {
        val env = TestEnvironment(
            appointmentsInPeriod = mutableListOf(
                TestEnvironment.appointment(AppointmentStatus.SCHEDULED),
                TestEnvironment.appointment(AppointmentStatus.SCHEDULED),
                TestEnvironment.appointment(AppointmentStatus.SCHEDULED),
                TestEnvironment.appointment(AppointmentStatus.CANCELLED)
            )
        )

        val report = successValue(env.reportService.appointmentsByStatusReport(startDate, endDate))

        assertEquals(4, report.total)
        assertEquals(3, row(report.rows, AppointmentStatus.SCHEDULED.toString()).quantity)
        assertEquals(75.0, row(report.rows, AppointmentStatus.SCHEDULED.toString()).percentage)
        assertEquals(25.0, row(report.rows, AppointmentStatus.CANCELLED.toString()).percentage)
    }

    @Test
    fun `porcentaje es cero cuando no existen registros`() {
        val env = TestEnvironment()

        val report = successValue(env.reportService.appointmentsByStatusReport(startDate, endDate))

        assertEquals(0, report.total)
        assertTrue(report.rows.isEmpty())
        assertTrue(report.rows.all { row -> row.percentage == 0.0 })
    }

    @Test
    fun `genera resumen general del sistema`() {
        val oilChange = TestEnvironment.maintenanceType(id = "oil", intervalMileage = 5_000)
        val env = TestEnvironment(
            vehicles = mutableListOf(
                TestEnvironment.vehicle(id = "overdue", currentMileage = 16_000),
                TestEnvironment.vehicle(id = "up-to-date", currentMileage = 12_000)
            ),
            maintenanceTypes = mutableListOf(oilChange),
            maintenanceMileage = mutableMapOf(
                ("overdue" to "oil") to 10_000,
                ("up-to-date" to "oil") to 10_000
            ),
            allAppointments = mutableListOf(
                TestEnvironment.appointment(AppointmentStatus.SCHEDULED),
                TestEnvironment.appointment(AppointmentStatus.CANCELLED)
            ),
            allOrders = mutableListOf(
                TestEnvironment.serviceOrder(ServiceStatus.COMPLETED, "preventivo"),
                TestEnvironment.serviceOrder(ServiceStatus.IN_PROGRESS, "correctivo")
            )
        )

        val summary = successValue(env.reportService.systemSummary())

        assertEquals(2, summary.totalVehicles)
        assertEquals(1, row(summary.appointmentsByStatus, AppointmentStatus.SCHEDULED.toString()).quantity)
        assertEquals(1, row(summary.ordersByStatus, ServiceStatus.COMPLETED.toString()).quantity)
        assertEquals(1, row(summary.vehiclesByMaintenanceStatus, MaintenanceStatus.OVERDUE.toString()).quantity)
        assertEquals(50.0, row(summary.vehiclesByMaintenanceStatus, MaintenanceStatus.OVERDUE.toString()).percentage)
    }

    private fun row(
        rows: List<ReportRow>,
        label: String
    ): ReportRow {
        return rows.first { row -> row.label == label }
    }

    private fun <T> successValue(outcome: Outcome<T>): T {
        assertTrue(outcome is Outcome.Success)
        return (outcome as Outcome.Success<T>).value
    }
}
