package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.errorOrNull
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.validation.TextFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServiceOrderServiceTest {

    @Test
    fun `receiving a vehicle from an appointment does not yet mark it attended`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.create(
            actor = env.staff,
            vehicleId = "VEH-003",
            serviceTypeId = "SVT-005",
            appointmentId = "APT-005",
            notes = "Ingresa por ruido en el motor.",
            now = env.now,
        )

        assertIs<Outcome.Success<ServiceOrder>>(result)
        assertEquals(ServiceStatus.RECEIVED, result.value.status)
        assertEquals("APT-005", result.value.appointmentId)
        assertEquals(null, result.value.startedAt)
        // The appointment is marked attended when the service is completed, not when the vehicle is received.
        val appointment = requireNotNull(env.repositories.appointments.findById("APT-005"))
        assertEquals(AppointmentStatus.CONFIRMED, appointment.status)
        assertTrue(appointment.status.occupiesSlot)
    }

    @Test
    fun `completing the order marks the appointment it came from as attended`() {
        val env = TestEnvironment()
        val created = env.serviceOrderService.create(
            env.staff, "VEH-003", "SVT-005", "APT-005", "Ingresa por ruido en el motor.", env.now,
        )
        assertIs<Outcome.Success<ServiceOrder>>(created)
        env.serviceOrderService.advance(env.staff, created.value.id, ServiceStatus.IN_PROGRESS, env.now)

        val closed = env.serviceOrderService.complete(
            actor = env.staff,
            orderId = created.value.id,
            mileageAtService = 97_000,
            performedMaintenanceTypeIds = listOf("MNT-001"),
            today = env.today,
            now = env.now,
        )

        assertIs<Outcome.Success<ServiceOrder>>(closed)
        assertEquals(env.now, closed.value.startedAt)
        val appointment = requireNotNull(env.repositories.appointments.findById("APT-005"))
        assertEquals(AppointmentStatus.ATTENDED, appointment.status)
        assertTrue(appointment.status.isFinal)
    }

    @Test
    fun `completing an order with no appointment does not touch any appointment`() {
        val env = TestEnvironment()
        val statusesBefore = env.repositories.appointments.findAll().associate { it.id to it.status }

        // SRV-003 is a walk-in with no appointment: appointmentId is null.
        val closed = env.serviceOrderService.complete(
            env.staff, "SRV-003", 97_000, emptyList(), env.today, env.now,
        )

        assertIs<Outcome.Success<ServiceOrder>>(closed)
        assertEquals(
            statusesBefore,
            env.repositories.appointments.findAll().associate { it.id to it.status },
        )
    }

    @Test
    fun `a client cannot create service orders`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.create(
            env.firstClient, "VEH-001", "SVT-001", null, "Intento indebido", env.now,
        )

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `the order advances from received to in progress`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-004", ServiceStatus.IN_PROGRESS, env.now)

        assertIs<Outcome.Success<ServiceOrder>>(result)
        assertEquals(ServiceStatus.IN_PROGRESS, result.value.status)
        assertEquals(env.now, result.value.startedAt)
    }

    @Test
    fun `advance cannot jump from received straight to completed`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-004", ServiceStatus.COMPLETED, env.now)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `an in-progress order cannot move backward`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-003", ServiceStatus.RECEIVED, env.now)

        assertIs<DomainError.InvalidTransition>(result.errorOrNull())
    }

    @Test
    fun `a completed order accepts no further changes`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-001", ServiceStatus.IN_PROGRESS, env.now)

        assertIs<DomainError.InvalidTransition>(result.errorOrNull())
    }

    @Test
    fun `completing updates the vehicle mileage and restarts the maintenance cycle`() {
        val env = TestEnvironment()
        val before = requireNotNull(env.repositories.vehicles.findById("VEH-003"))
        assertEquals(
            MaintenanceStatus.OVERDUE,
            env.maintenanceService.buildReport(before).overallStatus,
            "El vehiculo semilla debe partir vencido",
        )

        val closed = env.serviceOrderService.complete(
            actor = env.staff,
            orderId = "SRV-003",
            mileageAtService = 97_000,
            performedMaintenanceTypeIds = listOf("MNT-001", "MNT-004"),
            today = env.today,
            now = env.now,
        )

        assertIs<Outcome.Success<ServiceOrder>>(closed)
        assertEquals(ServiceStatus.COMPLETED, closed.value.status)
        assertEquals(97_000, closed.value.mileageAtService)
        assertNotNull(closed.value.closedAt)

        val after = requireNotNull(env.repositories.vehicles.findById("VEH-003"))
        assertEquals(97_000, after.currentMileage)
        assertEquals(env.today, after.mileageUpdatedAt)

        val record = requireNotNull(env.repositories.maintenanceRecords.findByVehicleAndType("VEH-003", "MNT-001"))
        assertEquals(97_000, record.lastServiceMileage)
        assertEquals(
            MaintenanceStatus.UP_TO_DATE,
            env.maintenanceService.buildReport(after).overallStatus,
            "Tras reiniciar el ciclo el vehiculo debe quedar al dia",
        )
    }

    @Test
    fun `completing with a mileage above the ceiling is rejected`() {
        val env = TestEnvironment()
        val before = requireNotNull(env.repositories.vehicles.findById("VEH-003")).currentMileage

        val result = env.serviceOrderService.complete(
            env.staff, "SRV-003", env.config.maxMileage + 1, emptyList(), env.today, env.now,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.MileageOutOfRange>(error.validation)
        assertEquals(before, requireNotNull(env.repositories.vehicles.findById("VEH-003")).currentMileage)
    }

    @Test
    fun `completing with a mileage below the known value is rejected`() {
        val env = TestEnvironment()
        val vehicle = requireNotNull(env.repositories.vehicles.findById("VEH-003"))

        val result = env.serviceOrderService.complete(
            env.staff, "SRV-003", vehicle.currentMileage - 1, emptyList(), env.today, env.now,
        )

        val error = assertIs<DomainError.MileageDecrease>(result.errorOrNull())
        assertEquals(vehicle.currentMileage, error.lastKnown)
        assertEquals(vehicle.currentMileage, requireNotNull(env.repositories.vehicles.findById("VEH-003")).currentMileage)
    }

    @Test
    fun `completing an order that was just received is rejected for an invalid transition`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.complete(
            env.staff, "SRV-004", 20_000, emptyList(), env.today, env.now,
        )

        assertIs<DomainError.InvalidTransition>(result.errorOrNull())
    }

    @Test
    fun `evidence with a valid URL is recorded`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.addEvidence(
            env.staff, "SRV-003", EvidenceStage.DIAGNOSIS, "https://ejemplo.local/foto.jpg", env.now,
        )

        assertIs<Outcome.Success<Evidence>>(result)
        assertEquals(EvidenceStage.DIAGNOSIS, result.value.stage)
    }

    @Test
    fun `an evidence URL with no protocol is rejected`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.addEvidence(
            env.staff, "SRV-003", EvidenceStage.DIAGNOSIS, "ejemplo.local/foto.jpg", env.now,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `the client looks up the history of their own vehicle`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.historyFor(env.firstClient, "VEH-001")

        val history = assertIs<Outcome.Success<List<ServiceOrderDetail>>>(result).value
        assertTrue(history.isNotEmpty())
        assertTrue(history.first().evidences.isNotEmpty())
    }

    @Test
    fun `a client cannot look up the history of someone else's vehicle`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.historyFor(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    // --- Limits on the notes and on the evidence URL ---

    @Test
    fun `reception notes longer than their limit are rejected`() {
        val env = TestEnvironment()
        val tooLong = "n".repeat(TextFields.NOTES.maxLength + 1)

        val result = env.serviceOrderService.create(
            env.staff, "VEH-001", "SVT-001", null, tooLong, env.now,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }

    @Test
    fun `empty notes are valid because the field is optional`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.create(env.staff, "VEH-001", "SVT-001", null, "", env.now)

        assertIs<Outcome.Success<ServiceOrder>>(result)
    }

    @Test
    fun `reception notes are stored without surrounding spaces`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.create(
            env.staff, "VEH-001", "SVT-001", null, "  golpe en la puerta  ", env.now,
        )

        assertEquals("golpe en la puerta", assertIs<Outcome.Success<ServiceOrder>>(result).value.notes)
    }

    @Test
    fun `an evidence URL longer than its limit is rejected`() {
        val env = TestEnvironment()
        val order = assertIs<Outcome.Success<ServiceOrder>>(
            env.serviceOrderService.create(env.staff, "VEH-001", "SVT-001", null, "", env.now)
        ).value
        val tooLong = "https://ejemplo.com/" + "a".repeat(TextFields.IMAGE_URL.maxLength)

        val result = env.serviceOrderService.addEvidence(
            env.staff, order.id, EvidenceStage.RECEPTION, tooLong, env.now,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }
}