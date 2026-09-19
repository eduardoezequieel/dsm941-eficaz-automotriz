package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.errorOrNull
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MaintenanceAndReportServiceTest {

    @Test
    fun `los datos semilla exhiben las cuatro clasificaciones de mantenimiento`() {
        val env = TestEnvironment()

        val statuses = env.repositories.vehicles.findAll()
            .map { env.maintenanceService.buildReport(it).overallStatus }
            .toSet()

        assertEquals(MaintenanceStatus.entries.toSet(), statuses)
    }

    @Test
    fun `el vehiculo sin registros aparece como sin registro previo`() {
        val env = TestEnvironment()

        val result = env.maintenanceService.reportFor(env.secondClient, "VEH-004")

        val report = assertIs<Outcome.Success<VehicleMaintenanceReport>>(result).value
        assertEquals(MaintenanceStatus.NO_PREVIOUS_RECORD, report.overallStatus)
        assertTrue(report.lines.all { it.evaluation.recommendedMileage == null })
    }

    @Test
    fun `un cliente no puede consultar el mantenimiento de un vehiculo ajeno`() {
        val env = TestEnvironment()

        val result = env.maintenanceService.reportFor(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `subir el kilometraje cambia la clasificacion sin tocar ningun dato almacenado`() {
        val env = TestEnvironment()
        val before = requireNotNull(env.repositories.vehicles.findById("VEH-001"))
        assertEquals(MaintenanceStatus.UP_TO_DATE, env.maintenanceService.buildReport(before).overallStatus)

        env.vehicleService.updateMileage(env.firstClient, "VEH-001", 33_500, env.today)

        val after = requireNotNull(env.repositories.vehicles.findById("VEH-001"))
        assertEquals(MaintenanceStatus.OVERDUE, env.maintenanceService.buildReport(after).overallStatus)
        // El registro conserva solo la base: la clasificacion nunca se persiste.
        assertEquals(
            28_000,
            requireNotNull(env.repositories.maintenanceRecords.findByVehicleAndType("VEH-001", "MNT-001")).lastServiceMileage,
        )
    }

    @Test
    fun `el conteo por clasificacion cubre todos los vehiculos registrados`() {
        val env = TestEnvironment()

        val counts = env.maintenanceService.statusCounts()

        assertEquals(env.repositories.vehicles.findAll().size, counts.values.sum())
        assertEquals(MaintenanceStatus.entries.toSet(), counts.keys)
    }

    @Test
    fun `el reporte de citas por estado suma cien por ciento`() {
        val env = TestEnvironment()

        val report = env.reportService.appointmentsByStatus(env.today.minusDays(30), env.today.plusDays(30))

        assertTrue(report.total > 0)
        assertEquals(report.total, report.rows.sumOf { it.count })
        assertEquals(100.0, report.rows.sumOf { it.percentage }, 0.001)
    }

    @Test
    fun `el reporte de citas descarta lo que cae fuera del periodo`() {
        val env = TestEnvironment()

        val narrow = env.reportService.appointmentsByStatus(env.today, env.today)
        val wide = env.reportService.appointmentsByStatus(env.today.minusDays(60), env.today.plusDays(60))

        assertTrue(narrow.total < wide.total)
    }

    @Test
    fun `un periodo sin citas devuelve un reporte vacio y no divide entre cero`() {
        val env = TestEnvironment()

        val report = env.reportService.appointmentsByStatus(env.today.plusYears(5), env.today.plusYears(5).plusDays(7))

        assertEquals(0, report.total)
        assertTrue(report.rows.isEmpty())
    }

    @Test
    fun `el reporte de servicios finalizados solo cuenta ordenes cerradas en el periodo`() {
        val env = TestEnvironment()

        val report = env.reportService.completedServicesByType(env.today.minusDays(30), env.today)

        assertEquals(2, report.total)
        assertTrue(report.rows.any { it.label == "Mantenimiento preventivo" })
        assertTrue(report.rows.any { it.label == "Sistema de frenos" })
    }

    @Test
    fun `el resumen del sistema refleja los cambios de inmediato`() {
        val env = TestEnvironment()
        val before = env.reportService.systemSummary()

        env.vehicleService.register(env.firstClient, "USR-001", "P010203", "Suzuki", "Swift", 2020, 5_000, env.today)
        env.appointmentService.cancel(env.firstClient, "APT-007")
        val after = env.reportService.systemSummary()

        assertEquals(before.totalVehicles + 1, after.totalVehicles)
        assertEquals(
            (before.appointmentsByStatus[AppointmentStatus.CANCELLED] ?: 0) + 1,
            after.appointmentsByStatus[AppointmentStatus.CANCELLED],
        )
        assertEquals(
            before.vehiclesByMaintenanceStatus.values.sum() + 1,
            after.vehiclesByMaintenanceStatus.values.sum(),
        )
    }

    @Test
    fun `el resumen cuenta las ordenes de servicio por estado`() {
        val env = TestEnvironment()

        val summary = env.reportService.systemSummary()

        assertEquals(2, summary.serviceOrdersByStatus[ServiceStatus.COMPLETED])
        assertEquals(1, summary.serviceOrdersByStatus[ServiceStatus.IN_PROGRESS])
        assertEquals(1, summary.serviceOrdersByStatus[ServiceStatus.RECEIVED])
    }
}
