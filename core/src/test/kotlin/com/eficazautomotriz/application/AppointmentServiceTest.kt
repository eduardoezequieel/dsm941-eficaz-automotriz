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
    fun `a client books a free slot and the appointment stays pending`() {
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
    fun `a second appointment in the same slot is rejected for capacity`() {
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
    fun `the slot conflict is recorded in the log`() {
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
    fun `booking on a past date is rejected`() {
        val env = TestEnvironment()

        val result = env.appointmentService.request(
            env.firstClient, "VEH-001", "SVT-001", env.today.minusDays(1), "SLT-001", env.today, env.now,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `booking a slot for another day of the week is rejected`() {
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
    fun `a client cannot book another client's vehicle`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.request(
            env.secondClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now,
        )

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `confirming rechecks availability without comparing the appointment to itself`() {
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
    fun `a client cannot confirm appointments`() {
        val env = TestEnvironment()

        val result = env.appointmentService.confirm(env.firstClient, "APT-006", env.today)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `rescheduling leaves the appointment RESCHEDULED and occupies the new slot`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val rescheduled = env.appointmentService.reschedule(
            env.staff, "APT-006", date, slotId, "Se adelanta a peticion del taller.", env.today,
        )

        val appointment = assertIs<Outcome.Success<com.eficazautomotriz.domain.model.Appointment>>(rescheduled).value
        assertEquals(AppointmentStatus.RESCHEDULED, appointment.status)

        // The new slot no longer admits another appointment: RESCHEDULED consumes a place.
        val availability = env.appointmentService.slotsFor(date, env.today).first { it.slot.id == slotId }
        assertTrue(!availability.selectable)
    }

    @Test
    fun `the client cancels their own appointment`() {
        val env = TestEnvironment()

        val result = env.appointmentService.cancel(env.firstClient, "APT-007")

        val appointment = assertIs<Outcome.Success<com.eficazautomotriz.domain.model.Appointment>>(result).value
        assertEquals(AppointmentStatus.CANCELLED, appointment.status)
    }

    @Test
    fun `a client cannot cancel someone else's appointment`() {
        val env = TestEnvironment()

        val result = env.appointmentService.cancel(env.secondClient, "APT-007")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `an appointment in a final state can no longer be changed`() {
        val env = TestEnvironment()

        val result = env.appointmentService.confirm(env.staff, "APT-003", env.today)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `cancelling an appointment frees the slot immediately`() {
        val env = TestEnvironment()
        val target = requireNotNull(env.repositories.appointments.findById("APT-007"))
        val before = env.appointmentService.slotsFor(target.date, env.today).first { it.slot.id == target.slotId }

        env.appointmentService.cancel(env.firstClient, target.id)
        val after = env.appointmentService.slotsFor(target.date, env.today).first { it.slot.id == target.slotId }

        assertTrue(!before.selectable, "Antes de cancelar la franja estaba ocupada")
        assertTrue(after.selectable, "Tras cancelar la franja debe quedar libre")
    }

    // --- Client answer to a reschedule (APT-008 is RESCHEDULED and belongs to the first client) ---

    @Test
    fun `accepting a reschedule confirms the appointment and keeps the reason`() {
        val env = TestEnvironment()
        val before = requireNotNull(env.repositories.appointments.findById("APT-008"))

        val result = env.appointmentService.acceptReschedule(env.firstClient, "APT-008")

        val appointment = assertIs<Outcome.Success<Appointment>>(result).value
        assertEquals(AppointmentStatus.CONFIRMED, appointment.status)
        assertEquals(before.reason, appointment.reason)
        assertEquals(before.date, appointment.date)
        assertEquals(before.slotId, appointment.slotId)
    }

    @Test
    fun `declining a reschedule cancels the appointment and frees the slot`() {
        val env = TestEnvironment()
        val target = requireNotNull(env.repositories.appointments.findById("APT-008"))
        val before = env.appointmentService.occupancyOf(target.slotId, target.date)

        val result = env.appointmentService.declineReschedule(env.firstClient, target.id)

        val appointment = assertIs<Outcome.Success<Appointment>>(result).value
        assertEquals(AppointmentStatus.CANCELLED, appointment.status)
        assertEquals(before - 1, env.appointmentService.occupancyOf(target.slotId, target.date))
    }

    @Test
    fun `a client cannot answer the reschedule of someone else's appointment`() {
        val env = TestEnvironment()

        assertIs<DomainError.Forbidden>(env.appointmentService.acceptReschedule(env.secondClient, "APT-008").errorOrNull())
        assertIs<DomainError.Forbidden>(env.appointmentService.declineReschedule(env.secondClient, "APT-008").errorOrNull())
        assertEquals(
            AppointmentStatus.RESCHEDULED,
            requireNotNull(env.repositories.appointments.findById("APT-008")).status,
        )
    }

    @Test
    fun `accepting is forbidden when the appointment is not rescheduled`() {
        val env = TestEnvironment()

        // APT-007 pending, APT-005 confirmed, APT-004 cancelled: all owned by the first client.
        for (id in listOf("APT-007", "APT-005", "APT-004")) {
            val status = requireNotNull(env.repositories.appointments.findById(id)).status

            val result = env.appointmentService.acceptReschedule(env.firstClient, id)

            assertIs<DomainError.Forbidden>(result.errorOrNull(), "accept on $id")
            assertEquals(status, requireNotNull(env.repositories.appointments.findById(id)).status)
        }
    }

    @Test
    fun `declining is forbidden when the appointment is not rescheduled`() {
        val env = TestEnvironment()

        for (id in listOf("APT-007", "APT-005", "APT-004")) {
            val status = requireNotNull(env.repositories.appointments.findById(id)).status

            val result = env.appointmentService.declineReschedule(env.firstClient, id)

            assertIs<DomainError.Forbidden>(result.errorOrNull(), "decline on $id")
            assertEquals(status, requireNotNull(env.repositories.appointments.findById(id)).status)
        }
    }

    @Test
    fun `answering a reschedule of an appointment that does not exist returns NotFound`() {
        val env = TestEnvironment()

        assertIs<DomainError.NotFound>(env.appointmentService.acceptReschedule(env.firstClient, "APT-999").errorOrNull())
    }

    @Test
    fun `Sunday slots do not exist in the catalog`() {
        val env = TestEnvironment()
        val sunday = generateSequence(env.today) { it.plusDays(1) }
            .first { it.dayOfWeek == java.time.DayOfWeek.SUNDAY }

        assertTrue(env.appointmentService.slotsFor(sunday, env.today).isEmpty())
    }

    @Test
    fun `disabling a slot removes it from the selectable ones`() {
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
    fun `enabling a slot again puts it back among the selectable ones`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()
        env.appointmentService.setSlotEnabled(env.staff, slotId, enabled = false)

        env.appointmentService.setSlotEnabled(env.staff, slotId, enabled = true)

        val availability = env.appointmentService.slotsFor(date, env.today).first { it.slot.id == slotId }
        assertTrue(availability.selectable)
    }

    @Test
    fun `raising a slot's capacity admits a second appointment`() {
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
    fun `a capacity of zero makes the slot fall back to the system capacity`() {
        val env = TestEnvironment()
        val (_, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.setSlotCapacity(env.staff, slotId, 0)

        val slot = assertIs<Outcome.Success<TimeSlot>>(result).value
        assertEquals(0, slot.capacity)
        assertEquals(env.config.defaultSlotCapacity, env.appointmentService.effectiveCapacityOf(slot))
    }

    @Test
    fun `a negative capacity is rejected as a business error`() {
        val env = TestEnvironment()
        val (_, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.setSlotCapacity(env.staff, slotId, -1)

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertEquals("capacity", error.validation.field)
        assertEquals(1, requireNotNull(env.appointmentService.slotOf(slotId)).capacity)
    }

    @Test
    fun `a client cannot configure the time slots`() {
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
    fun `configuring a slot that does not exist returns NotFound`() {
        val env = TestEnvironment()

        assertIs<DomainError.NotFound>(
            env.appointmentService.setSlotEnabled(env.staff, "SLT-999", enabled = false).errorOrNull()
        )
        assertIs<DomainError.NotFound>(
            env.appointmentService.setSlotCapacity(env.staff, "SLT-999", 3).errorOrNull()
        )
    }

    // --- Limits on the reason and on capacity ---

    @Test
    fun `a reason is stored without surrounding spaces`() {
        val env = TestEnvironment()

        val result = env.appointmentService.reject(env.staff, "APT-006", "  Sin disponibilidad  ")

        assertEquals("Sin disponibilidad", assertIs<Outcome.Success<Appointment>>(result).value.reason)
    }

    @Test
    fun `rejecting without a reason is rejected`() {
        val env = TestEnvironment()

        val result = env.appointmentService.reject(env.staff, "APT-006", "   ")

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.BlankText>(error.validation)
    }

    @Test
    fun `a reason longer than its limit is rejected`() {
        val env = TestEnvironment()
        val tooLong = "x".repeat(TextFields.REASON.maxLength + 1)

        val result = env.appointmentService.reject(env.staff, "APT-006", tooLong)

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }

    @Test
    fun `rescheduling also requires a reason within the limit`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()

        val result = env.appointmentService.reschedule(
            env.staff, "APT-006", date, slotId, "x".repeat(TextFields.REASON.maxLength + 1), env.today,
        )

        assertIs<ValidationError.TextTooLong>(assertIs<DomainError.Invalid>(result.errorOrNull()).validation)
    }

    @Test
    fun `a capacity above the ceiling is rejected`() {
        val env = TestEnvironment()
        val slot = env.appointmentService.slotCatalog().first()

        val result = env.appointmentService.setSlotCapacity(env.staff, slot.id, env.config.maxSlotCapacity + 1)

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.SlotCapacityOutOfRange>(error.validation)
    }

    @Test
    fun `confirming is rejected when the other appointments already fill the slot`() {
        val env = TestEnvironment()
        val (date, slotId) = env.freeSlotOnOrAfterTomorrow()
        env.appointmentService.setSlotCapacity(env.staff, slotId, 2)
        val first = env.appointmentService.request(
            env.firstClient, "VEH-001", "SVT-001", date, slotId, env.today, env.now,
        )
        env.appointmentService.request(env.secondClient, "VEH-004", "SVT-002", date, slotId, env.today, env.now)
        env.appointmentService.setSlotCapacity(env.staff, slotId, 1)

        val firstId = assertIs<Outcome.Success<Appointment>>(first).value.id
        val error = assertIs<DomainError.SlotUnavailable>(
            env.appointmentService.confirm(env.staff, firstId, env.today).errorOrNull()
        )
        assertEquals(UnavailabilityReason.CAPACITY_REACHED, error.reason)
    }

    @Test
    fun `rescheduling into the same date and slot does not count the appointment itself`() {
        val env = TestEnvironment()
        val target = requireNotNull(env.repositories.appointments.findById("APT-006"))

        val result = env.appointmentService.reschedule(
            env.staff, target.id, target.date, target.slotId, "Se confirma el mismo horario.", env.today,
        )

        assertIs<Outcome.Success<Appointment>>(result)
        assertEquals(1, env.appointmentService.occupancyOf(target.slotId, target.date))
    }

    @Test
    fun `the slots shown for a reschedule leave out the appointment being moved`() {
        val env = TestEnvironment()
        val target = requireNotNull(env.repositories.appointments.findById("APT-006"))

        val plain = env.appointmentService.slotsFor(target.date, env.today).first { it.slot.id == target.slotId }
        val excluding = env.appointmentService.slotsFor(target.date, env.today, excludingAppointmentId = target.id)
            .first { it.slot.id == target.slotId }

        assertEquals(1, plain.occupied)
        assertTrue(!plain.selectable)
        assertEquals(0, excluding.occupied)
        assertTrue(excluding.selectable)
    }

    @Test
    fun `occupancy counts only the statuses that occupy a slot`() {
        val env = TestEnvironment()
        val target = requireNotNull(env.repositories.appointments.findById("APT-007"))
        assertEquals(mapOf(target.slotId to 1), env.repositories.appointments.occupancyOn(target.date))

        env.appointmentService.cancel(env.firstClient, target.id)

        assertEquals(emptyMap(), env.repositories.appointments.occupancyOn(target.date))
    }
}
