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
    fun `the seed data exhibits the four maintenance classifications`() {
        val env = TestEnvironment()

        val statuses = env.repositories.vehicles.findAll()
            .map { env.maintenanceService.buildReport(it).overallStatus }
            .toSet()

        assertEquals(MaintenanceStatus.entries.toSet(), statuses)
    }

    @Test
    fun `the vehicle with no records shows up as no previous record`() {
        val env = TestEnvironment()

        val result = env.maintenanceService.reportFor(env.secondClient, "VEH-004")

        val report = assertIs<Outcome.Success<VehicleMaintenanceReport>>(result).value
        assertEquals(MaintenanceStatus.NO_PREVIOUS_RECORD, report.overallStatus)
        assertTrue(report.lines.all { it.evaluation.recommendedMileage == null })
    }

    @Test
    fun `a client cannot look up maintenance for someone else's vehicle`() {
        val env = TestEnvironment()

        val result = env.maintenanceService.reportFor(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `raising the mileage changes the classification without touching any stored data`() {
        val env = TestEnvironment()
        val before = requireNotNull(env.repositories.vehicles.findById("VEH-001"))
        assertEquals(MaintenanceStatus.UP_TO_DATE, env.maintenanceService.buildReport(before).overallStatus)

        env.vehicleService.updateMileage(env.firstClient, "VEH-001", 33_500, env.today)

        val after = requireNotNull(env.repositories.vehicles.findById("VEH-001"))
        assertEquals(MaintenanceStatus.OVERDUE, env.maintenanceService.buildReport(after).overallStatus)
        // The record keeps only the baseline: the classification is never persisted.
        assertEquals(
            28_000,
            requireNotNull(env.repositories.maintenanceRecords.findByVehicleAndType("VEH-001", "MNT-001")).lastServiceMileage,
        )
    }

    @Test
    fun `the count by classification covers every registered vehicle`() {
        val env = TestEnvironment()

        val counts = env.maintenanceService.statusCounts()

        assertEquals(env.repositories.vehicles.findAll().size, counts.values.sum())
        assertEquals(MaintenanceStatus.entries.toSet(), counts.keys)
    }

    @Test
    fun `the appointments-by-status report adds up to one hundred percent`() {
        val env = TestEnvironment()

        val report = env.reportService.appointmentsByStatus(env.today.minusDays(30), env.today.plusDays(30))

        assertTrue(report.total > 0)
        assertEquals(report.total, report.rows.sumOf { it.count })
        assertEquals(100.0, report.rows.sumOf { it.percentage }, 0.001)
    }

    @Test
    fun `the appointments report drops whatever falls outside the period`() {
        val env = TestEnvironment()

        val narrow = env.reportService.appointmentsByStatus(env.today, env.today)
        val wide = env.reportService.appointmentsByStatus(env.today.minusDays(60), env.today.plusDays(60))

        assertTrue(narrow.total < wide.total)
    }

    @Test
    fun `an inverted period returns an empty report without querying`() {
        val env = TestEnvironment()

        val appointments = env.reportService.appointmentsByStatus(env.today, env.today.minusDays(1))
        val services = env.reportService.completedServicesByType(env.today, env.today.minusDays(1))

        assertEquals(0, appointments.total)
        assertTrue(appointments.rows.isEmpty())
        assertEquals(0, services.total)
        assertTrue(services.rows.isEmpty())
    }

    @Test
    fun `a period with no appointments returns an empty report and does not divide by zero`() {
        val env = TestEnvironment()

        val report = env.reportService.appointmentsByStatus(env.today.plusYears(5), env.today.plusYears(5).plusDays(7))

        assertEquals(0, report.total)
        assertTrue(report.rows.isEmpty())
    }

    @Test
    fun `the completed-services report counts only orders closed inside the period`() {
        val env = TestEnvironment()

        val report = env.reportService.completedServicesByType(env.today.minusDays(30), env.today)

        assertEquals(2, report.total)
        assertTrue(report.rows.any { it.label == "Mantenimiento preventivo" })
        assertTrue(report.rows.any { it.label == "Sistema de frenos" })
    }

    @Test
    fun `the system summary reflects changes immediately`() {
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
    fun `the summary counts service orders by status`() {
        val env = TestEnvironment()

        val summary = env.reportService.systemSummary()

        assertEquals(2, summary.serviceOrdersByStatus[ServiceStatus.COMPLETED])
        assertEquals(1, summary.serviceOrdersByStatus[ServiceStatus.IN_PROGRESS])
        assertEquals(1, summary.serviceOrdersByStatus[ServiceStatus.RECEIVED])
    }
}