package com.eficazautomotriz.app.demo

import com.eficazautomotriz.app.viewmodel.mostSevereMaintenanceAlert
import com.eficazautomotriz.app.viewmodel.staff.ReportPeriodPreset
import com.eficazautomotriz.app.viewmodel.staff.reportPresetRange
import com.eficazautomotriz.application.AppointmentService
import com.eficazautomotriz.application.AuthService
import com.eficazautomotriz.application.MaintenanceService
import com.eficazautomotriz.application.ReportService
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.model.enums.UserRole
import com.eficazautomotriz.domain.rules.AppointmentAvailabilityRule
import com.eficazautomotriz.domain.rules.MaintenanceStatusRule
import com.eficazautomotriz.logging.ErrorLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

/** The demo boots with the mockup data: f3/f4 maintenance, f5/f8 appointments, f7 service, f9 reports. */
class MockupSeedDataTest {

    private val today = LocalDate.of(2026, 10, 11)
    private val config = SystemConfig(warningThresholdKm = MockupSeedData.DEMO_WARNING_THRESHOLD_KM)
    private val logger = object : ErrorLogger {
        override fun warn(context: String, message: String, data: String) = Unit
        override fun error(context: String, message: String, data: String) = Unit
    }

    private fun seeded(on: LocalDate = today): Repositories = demoBackend(on).repositories

    private fun maintenance(repositories: Repositories) = MaintenanceService(
        repositories.vehicles, repositories.maintenanceRecords, repositories.maintenanceTypes,
        MaintenanceStatusRule(), config, logger,
    )

    private fun appointments(repositories: Repositories) = AppointmentService(
        repositories.appointments, repositories.vehicles, repositories.timeSlots, repositories.serviceTypes,
        AppointmentAvailabilityRule(), config, logger,
    )

    private fun user(repositories: Repositories, id: String): User = repositories.users.findById(id)!!

    @Test
    fun `demo accounts sign in as Diego (client) and Carlos (staff)`() = runBlocking {
        val backend = demoBackend(today)
        val auth = AuthService(backend.repositories.users, logger)
        assertEquals(MockupSeedData.CLIENT_ID, backend.authGateway.currentUid(), "boots signed in as Diego")

        val clientUid = backend.authGateway.signInWithEmail(MockupSeedData.CLIENT_EMAIL, MockupSeedData.DEMO_PASSWORD)
        val diego = (auth.signIn(clientUid) as Outcome.Success).value
        assertEquals("Diego Escobar", diego.name)
        assertEquals(UserRole.CLIENT, diego.role)

        val staffUid = backend.authGateway.signInWithEmail(MockupSeedData.STAFF_EMAIL, MockupSeedData.DEMO_PASSWORD)
        val carlos = (auth.signIn(staffUid) as Outcome.Success).value
        assertEquals("Carlos Cruz", carlos.name)
        assertEquals(UserRole.STAFF, carlos.role)
    }

    @Test
    fun `Nissan Frontier is due soon only for the oil change`() {
        val repositories = seeded()
        val report = maintenance(repositories).buildReport(repositories.vehicles.findById(MockupSeedData.FRONTIER)!!)
        val byName = report.lines.associate { it.type.name to it.evaluation.status }
        assertEquals(
            mapOf(
                "Cambio de aceite y filtro" to MaintenanceStatus.DUE_SOON,
                "Rotación de neumáticos" to MaintenanceStatus.UP_TO_DATE,
                "Revisión de frenos" to MaintenanceStatus.UP_TO_DATE,
                "Cambio de filtro de aire" to MaintenanceStatus.UP_TO_DATE,
            ),
            byName,
        )
        assertEquals(84_100, report.vehicle.currentMileage)
        val oil = report.lines.first { it.type.name.startsWith("Cambio de aceite") }
        assertEquals(85_000, oil.evaluation.recommendedMileage)
    }

    @Test
    fun `Diego has the three mockup vehicles with their overall status`() {
        val repositories = seeded()
        val service = maintenance(repositories)
        val vehicles = repositories.vehicles.findByOwner(MockupSeedData.CLIENT_ID).sortedBy { it.id }
        assertEquals(listOf("Corolla", "Frontier", "Accent"), vehicles.map { it.model })
        assertEquals(listOf(78_400, 84_100, 142_900), vehicles.map { it.currentMileage })
        // f2/f3: Corolla "Vigente", Frontier "Próximo", Accent "Vencido".
        assertEquals(
            listOf(MaintenanceStatus.UP_TO_DATE, MaintenanceStatus.DUE_SOON, MaintenanceStatus.OVERDUE),
            vehicles.map { service.buildReport(it).overallStatus },
        )
    }

    @Test
    fun `the home alert names the Frontier oil change nearest in km`() {
        val repositories = seeded()
        val service = maintenance(repositories)
        val reports = repositories.vehicles.findByOwner(MockupSeedData.CLIENT_ID).map(service::buildReport)
        val alert = mostSevereMaintenanceAlert(reports)!!
        // f3 still marks the Accent overdue. The alert follows the nearest due-soon line (f2).
        assertEquals(MockupSeedData.FRONTIER, alert.vehicle.id)
        assertEquals(MaintenanceStatus.DUE_SOON, alert.status)
        assertEquals(85_000, alert.recommendedMileage)
        assertEquals(84_100, alert.currentMileage)
        assertEquals("Cambio de aceite y filtro", alert.typeName)
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-10-11", "2026-10-01", "2026-10-16", "2026-12-31"])
    fun `the confirmed preventive maintenance is Diego's next appointment`(date: String) {
        val day = LocalDate.parse(date)
        val repositories = seeded(day)
        val diego = user(repositories, MockupSeedData.CLIENT_ID)
        val next = appointments(repositories).listVisibleTo(diego)
            .filter { !it.status.isFinal && !it.date.isBefore(day) }
            .minByOrNull { it.date }!!
        assertEquals("CT-1042", next.id)
        assertEquals(AppointmentStatus.CONFIRMED, next.status)
        assertTrue(next.date.isAfter(day))
    }

    @Test
    fun `Diego's appointments cover the f5 states`() {
        val repositories = seeded()
        val statuses = repositories.appointments.findAll()
            .filter { it.clientId == MockupSeedData.CLIENT_ID }
            .associate { it.id to it.status }
        assertEquals(AppointmentStatus.CONFIRMED, statuses["CT-1042"])
        assertEquals(AppointmentStatus.PENDING, statuses["CT-1051"])
        assertEquals(AppointmentStatus.RESCHEDULED, statuses["CT-1039"])
        assertEquals(AppointmentStatus.REJECTED, statuses["CT-1028"])
    }

    @Test
    fun `staff panel counters match f8`() {
        val repositories = seeded()
        val carlos = user(repositories, MockupSeedData.STAFF_ID)
        val pending = (appointments(repositories).listPending(carlos) as Outcome.Success).value
        assertEquals(listOf("CT-1051", "CT-1055"), pending.map { it.id })
        assertEquals(4, repositories.appointments.findAll().count { it.date == today })
        assertEquals(3, repositories.serviceOrders.findAll().count { it.status == ServiceStatus.IN_PROGRESS })
    }

    @Test
    fun `brake service of f7 has three evidences and its attended appointment`() {
        val repositories = seeded()
        val order = repositories.serviceOrders.findById("SV-2087")!!
        assertEquals(ServiceStatus.COMPLETED, order.status)
        assertEquals(78_400, order.mileageAtService)
        assertEquals("Sistema de frenos", repositories.serviceTypes.nameOf(order.serviceTypeId))
        assertEquals(
            listOf(EvidenceStage.RECEPTION, EvidenceStage.DIAGNOSIS, EvidenceStage.RESULT),
            repositories.evidences.findByServiceOrder("SV-2087").sortedBy { it.uploadedAt }.map { it.stage },
        )
        assertEquals(AppointmentStatus.ATTENDED, repositories.appointments.findById("CT-1030")!!.status)
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-10-11", "2026-10-01", "2026-11-02"])
    fun `monthly report has appointments and completed services of every mockup type`(date: String) {
        val day = LocalDate.parse(date)
        val repositories = seeded(day)
        val reports = ReportService(
            repositories.appointments, repositories.serviceOrders, repositories.serviceTypes,
            repositories.vehicles, maintenance(repositories),
        )
        val (from, to) = reportPresetRange(ReportPeriodPreset.MONTH, day)
        val byType = reports.completedServicesByType(from, to).rows.map { it.label }.toSet()
        assertEquals(
            setOf("Mantenimiento preventivo", "Sistema de frenos", "Diagnóstico por escáner", "Sistema eléctrico", "Aire acondicionado"),
            byType,
        )
        assertTrue(reports.appointmentsByStatus(from, to).total >= 20)
    }
}
