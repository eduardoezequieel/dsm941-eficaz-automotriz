package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.errorOrNull
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.rules.UnavailabilityReason
import com.eficazautomotriz.domain.validation.TextFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppointmentServiceTest {

    @Test
    fun `un cliente agenda en una franja libre y la cita queda pendiente`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.request(
            actor = env.firstClient,
            vehicleId = "VEH-001",
            serviceTypeId = "SVT-001",
            date = date,
            slotId = slotId,
            today = env.today,
            now = env.now,
        )

        val appointment = assertIs<Outcome.Success<*>>(result).value
        assertIs<com.eficazautomotriz.domain.model.Appointment>(appointment)
        assertEquals(AppointmentStatus.PENDING, appointment.status)
        assertEquals("USR-001", appointment.clientId)
    }

    @Test
    fun `una segunda cita en la misma franja se rechaza por capacidad`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val first = env.appointmentService.request(
            env.firstClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now,
        )
        assertIs<Outcome.Success<*>>(first)

        val second = env.appointmentService.request(
            env.secondClient, "VEH-004", "SVT-002", date, slotId, env.today, env.now,
        )

        val error = assertIs<DomainError.SlotUnavailable>(second.errorOrNull())
        assertEquals(UnavailabilityReason.CAPACITY_REACHED, error.reason)
    }

    @Test
    fun `el conflicto de franja queda registrado en el log`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()
        env.appointmentService.request(env.firstClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now)

        env.appointmentService.request(env.secondClient, "VEH-004", "SVT-002", date, slotId, env.today, env.now)

        assertTrue(
            env.logger.entries.any { it.contains("SlotUnavailable(CAPACITY_REACHED)") },
            "El conflicto de franja debe quedar en el log: ${env.logger.entries}",
        )
    }

    @Test
    fun `agendar en una fecha pasada se rechaza`() {
        val env = TestEnvironment()

        val result = env.appointmentService.request(
            env.firstClient, "VEH-001", "SVT-001", env.today.minusDays(1), "SLT-001", env.today, env.now,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `agendar en una franja de otro dia de la semana se rechaza`() {
        val env = TestEnvironment()
        val mondaySlot = requireNotNull(env.repositories.timeSlots.findById("SLT-001"))
        val notMonday = generateSequence(env.today.plusDays(1)) { it.plusDays(1) }
            .first { it.dayOfWeek != mondaySlot.dayOfWeek }

        val result = env.appointmentService.request(
            env.firstClient, "VEH-001", "SVT-001", notMonday, "SLT-001", env.today, env.now,
        )

        val error = assertIs<DomainError.SlotUnavailable>(result.errorOrNull())
        assertEquals(UnavailabilityReason.DAY_MISMATCH, error.reason)
    }

    @Test
    fun `un cliente no puede agendar sobre el vehiculo de otro cliente`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.request(
            env.secondClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now,
        )

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `confirmar revalida la disponibilidad sin compararse consigo misma`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()
        val requested = assertIs<Outcome.Success<com.eficazautomotriz.domain.model.Appointment>>(
            env.appointmentService.request(env.firstClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now)
        ).value

        val confirmed = env.appointmentService.confirm(env.staff, requested.id, env.today)

        val appointment = assertIs<Outcome.Success<com.eficazautomotriz.domain.model.Appointment>>(confirmed).value
        assertEquals(AppointmentStatus.CONFIRMED, appointment.status)
    }

    @Test
    fun `un cliente no puede confirmar citas`() {
        val env = TestEnvironment()

        val result = env.appointmentService.confirm(env.firstClient, "APT-006", env.today)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `reprogramar deja la cita en RESCHEDULED y ocupa la franja nueva`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val rescheduled = env.appointmentService.reschedule(
            env.staff, "APT-006", date, slotId, "Se adelanta a peticion del taller.", env.today,
        )

        val appointment = assertIs<Outcome.Success<com.eficazautomotriz.domain.model.Appointment>>(rescheduled).value
        assertEquals(AppointmentStatus.RESCHEDULED, appointment.status)

        // La franja nueva ya no admite otra cita: RESCHEDULED consume cupo.
        val availability = env.appointmentService.slotsFor(date, env.today).first { it.slot.id == slotId }
        assertTrue(!availability.selectable)
    }

    @Test
    fun `el cliente cancela su propia cita`() {
        val env = TestEnvironment()

        val result = env.appointmentService.cancel(env.firstClient, "APT-007")

        val appointment = assertIs<Outcome.Success<com.eficazautomotriz.domain.model.Appointment>>(result).value
        assertEquals(AppointmentStatus.CANCELLED, appointment.status)
    }

    @Test
    fun `un cliente no puede cancelar la cita de otro`() {
        val env = TestEnvironment()

        val result = env.appointmentService.cancel(env.secondClient, "APT-007")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `una cita en estado final ya no se puede modificar`() {
        val env = TestEnvironment()

        val result = env.appointmentService.confirm(env.staff, "APT-003", env.today)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `cancelar una cita libera la franja de inmediato`() {
        val env = TestEnvironment()
        val target = requireNotNull(env.repositories.appointments.findById("APT-007"))
        val before = env.appointmentService.slotsFor(target.date, env.today).first { it.slot.id == target.slotId }

        env.appointmentService.cancel(env.firstClient, target.id)
        val after = env.appointmentService.slotsFor(target.date, env.today).first { it.slot.id == target.slotId }

        assertTrue(!before.selectable, "Antes de cancelar la franja estaba ocupada")
        assertTrue(after.selectable, "Tras cancelar la franja debe quedar libre")
    }

    @Test
    fun `las franjas del domingo no existen en el catalogo`() {
        val env = TestEnvironment()
        val sunday = generateSequence(env.today) { it.plusDays(1) }
            .first { it.dayOfWeek == java.time.DayOfWeek.SUNDAY }

        assertTrue(env.appointmentService.slotsFor(sunday, env.today).isEmpty())
    }

    @Test
    fun `deshabilitar una franja la saca de las seleccionables`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.setSlotEnabled(env.staff, slotId, enabled = false)

        assertIs<Outcome.Success<TimeSlot>>(result)
        assertTrue(!result.value.enabled)
        val availability = env.appointmentService.slotsFor(date, env.today).first { it.slot.id == slotId }
        val error = assertIs<DomainError.SlotUnavailable>(
            env.appointmentService.request(
                env.firstClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now,
            ).errorOrNull()
        )
        assertTrue(!availability.selectable)
        assertEquals(UnavailabilityReason.SLOT_DISABLED, error.reason)
    }

    @Test
    fun `volver a habilitar una franja la devuelve a las seleccionables`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()
        env.appointmentService.setSlotEnabled(env.staff, slotId, enabled = false)

        env.appointmentService.setSlotEnabled(env.staff, slotId, enabled = true)

        val availability = env.appointmentService.slotsFor(date, env.today).first { it.slot.id == slotId }
        assertTrue(availability.selectable)
    }

    @Test
    fun `ampliar la capacidad de una franja admite una segunda cita`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()
        env.appointmentService.request(env.firstClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now)

        assertIs<Outcome.Success<TimeSlot>>(env.appointmentService.setSlotCapacity(env.staff, slotId, 2))

        val second = env.appointmentService.request(
            env.secondClient, "VEH-004", "SVT-002", date, slotId, env.today, env.now,
        )
        assertIs<Outcome.Success<Appointment>>(second)
        assertEquals(2, env.appointmentService.occupancyOf(slotId, date))
    }

    @Test
    fun `capacidad cero hace que la franja caiga a la capacidad del sistema`() {
        val env = TestEnvironment()
        val (_, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.setSlotCapacity(env.staff, slotId, 0)

        val slot = assertIs<Outcome.Success<TimeSlot>>(result).value
        assertEquals(0, slot.capacity)
        assertEquals(env.config.defaultSlotCapacity, env.appointmentService.effectiveCapacityOf(slot))
    }

    @Test
    fun `una capacidad negativa se rechaza como error de negocio`() {
        val env = TestEnvironment()
        val (_, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.setSlotCapacity(env.staff, slotId, -1)

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertEquals("capacity", error.validation.field)
        assertEquals(1, requireNotNull(env.appointmentService.slotOf(slotId)).capacity)
    }

    @Test
    fun `un cliente no puede configurar las franjas horarias`() {
        val env = TestEnvironment()
        val (_, slotId) = env.freeSlotOnOrAfterTomorrow()

        assertIs<DomainError.Forbidden>(
            env.appointmentService.setSlotEnabled(env.firstClient, slotId, enabled = false).errorOrNull()
        )
        assertIs<DomainError.Forbidden>(
            env.appointmentService.setSlotCapacity(env.firstClient, slotId, 5).errorOrNull()
        )
        assertTrue(requireNotNull(env.appointmentService.slotOf(slotId)).enabled)
    }

    @Test
    fun `configurar una franja inexistente devuelve NotFound`() {
        val env = TestEnvironment()

        assertIs<DomainError.NotFound>(
            env.appointmentService.setSlotEnabled(env.staff, "SLT-999", enabled = false).errorOrNull()
        )
        assertIs<DomainError.NotFound>(
            env.appointmentService.setSlotCapacity(env.staff, "SLT-999", 3).errorOrNull()
        )
    }

    // --- Limites del motivo y de la capacidad ---

    @Test
    fun `rechazar sin motivo se rechaza`() {
        val env = TestEnvironment()

        val result = env.appointmentService.reject(env.staff, "APT-006", "   ")

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.BlankText>(error.validation)
    }

    @Test
    fun `un motivo mas largo que su limite se rechaza`() {
        val env = TestEnvironment()
        val tooLong = "x".repeat(TextFields.REASON.maxLength + 1)

        val result = env.appointmentService.reject(env.staff, "APT-006", tooLong)

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }

    @Test
    fun `reprogramar tambien exige un motivo dentro del limite`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.reschedule(
            env.staff, "APT-006", date, slotId, "x".repeat(TextFields.REASON.maxLength + 1), env.today,
        )

        assertIs<ValidationError.TextTooLong>(assertIs<DomainError.Invalid>(result.errorOrNull()).validation)
    }

    @Test
    fun `una capacidad por encima del techo se rechaza`() {
        val env = TestEnvironment()
        val slot = env.appointmentService.slotCatalog().first()

        val result = env.appointmentService.setSlotCapacity(env.staff, slot.id, env.config.maxSlotCapacity + 1)

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.SlotCapacityOutOfRange>(error.validation)
    }
}
