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
    fun `recibir un vehiculo desde una cita todavia no la marca como atendida`() {
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
        // La cita se atiende al finalizar el servicio, no al recibir el vehiculo.
        val appointment = requireNotNull(env.repositories.appointments.findById("APT-005"))
        assertEquals(AppointmentStatus.CONFIRMED, appointment.status)
        assertTrue(appointment.status.occupiesSlot)
    }

    @Test
    fun `finalizar la orden marca como atendida la cita de la que proviene`() {
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
        val appointment = requireNotNull(env.repositories.appointments.findById("APT-005"))
        assertEquals(AppointmentStatus.ATTENDED, appointment.status)
        assertTrue(appointment.status.isFinal)
    }

    @Test
    fun `finalizar una orden sin cita no toca ninguna cita`() {
        val env = TestEnvironment()
        val statusesBefore = env.repositories.appointments.findAll().associate { it.id to it.status }

        // SRV-003 ingreso sin cita: appointmentId es null.
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
    fun `un cliente no puede crear ordenes de servicio`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.create(
            env.firstClient, "VEH-001", "SVT-001", null, "Intento indebido", env.now,
        )

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `la orden avanza de recibida a en proceso`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-004", ServiceStatus.IN_PROGRESS, env.now)

        assertIs<Outcome.Success<ServiceOrder>>(result)
        assertEquals(ServiceStatus.IN_PROGRESS, result.value.status)
    }

    @Test
    fun `no se puede saltar de recibida a finalizada por la via de avanzar`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-004", ServiceStatus.COMPLETED, env.now)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `no se puede retroceder una orden en proceso`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-003", ServiceStatus.RECEIVED, env.now)

        assertIs<DomainError.InvalidTransition>(result.errorOrNull())
    }

    @Test
    fun `una orden finalizada no admite mas cambios`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.advance(env.staff, "SRV-001", ServiceStatus.IN_PROGRESS, env.now)

        assertIs<DomainError.InvalidTransition>(result.errorOrNull())
    }

    @Test
    fun `finalizar actualiza el kilometraje del vehiculo y reinicia el ciclo de mantenimiento`() {
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
    fun `finalizar con kilometraje menor al conocido se rechaza`() {
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
    fun `finalizar una orden recien recibida se rechaza por transicion invalida`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.complete(
            env.staff, "SRV-004", 20_000, emptyList(), env.today, env.now,
        )

        assertIs<DomainError.InvalidTransition>(result.errorOrNull())
    }

    @Test
    fun `se registra evidencia con direccion valida`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.addEvidence(
            env.staff, "SRV-003", EvidenceStage.DIAGNOSIS, "https://ejemplo.local/foto.jpg", env.now,
        )

        assertIs<Outcome.Success<Evidence>>(result)
        assertEquals(EvidenceStage.DIAGNOSIS, result.value.stage)
    }

    @Test
    fun `una direccion de evidencia sin protocolo se rechaza`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.addEvidence(
            env.staff, "SRV-003", EvidenceStage.DIAGNOSIS, "ejemplo.local/foto.jpg", env.now,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `el cliente consulta el historial de su propio vehiculo`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.historyFor(env.firstClient, "VEH-001")

        val history = assertIs<Outcome.Success<List<ServiceOrderDetail>>>(result).value
        assertTrue(history.isNotEmpty())
        assertTrue(history.first().evidences.isNotEmpty())
    }

    @Test
    fun `un cliente no puede consultar el historial de un vehiculo ajeno`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.historyFor(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    // --- Limites de las notas y de la direccion de la evidencia ---

    @Test
    fun `unas notas de recepcion mas largas que su limite se rechazan`() {
        val env = TestEnvironment()
        val tooLong = "n".repeat(TextFields.NOTES.maxLength + 1)

        val result = env.serviceOrderService.create(
            env.staff, "VEH-001", "SVT-001", null, tooLong, env.now,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }

    @Test
    fun `unas notas vacias son validas porque el campo es opcional`() {
        val env = TestEnvironment()

        val result = env.serviceOrderService.create(env.staff, "VEH-001", "SVT-001", null, "", env.now)

        assertIs<Outcome.Success<ServiceOrder>>(result)
    }

    @Test
    fun `una direccion de evidencia mas larga que su limite se rechaza`() {
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
