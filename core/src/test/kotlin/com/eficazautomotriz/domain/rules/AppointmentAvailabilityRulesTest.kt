package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AppointmentAvailabilityRuleTest {

    private val rule = AppointmentAvailabilityRule()
    private val config = SystemConfig()

    // Wednesday 16 September 2026, used as "today" in every test.
    private val today = LocalDate.of(2026, 9, 16)
    private val wednesday = LocalDate.of(2026, 9, 16)
    private val thursday = LocalDate.of(2026, 9, 17)

    private fun slot(
        id: String = "SLT-001",
        dayOfWeek: DayOfWeek = DayOfWeek.WEDNESDAY,
        enabled: Boolean = true,
        capacity: Int = 1,
    ) = TimeSlot(
        id = id,
        dayOfWeek = dayOfWeek,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        enabled = enabled,
        capacity = capacity,
    )

    private fun appointment(
        id: String,
        date: LocalDate,
        slotId: String = "SLT-001",
        status: AppointmentStatus,
    ) = Appointment(
        id = id,
        clientId = "USR-001",
        vehicleId = "VEH-001",
        serviceTypeId = "SVT-001",
        date = date,
        slotId = slotId,
        status = status,
        reason = null,
        requestedAt = LocalDateTime.of(2026, 9, 10, 8, 0),
    )

    private fun assertUnavailable(expected: UnavailabilityReason, actual: AvailabilityResult) {
        assertEquals(AvailabilityResult.Unavailable(expected), actual)
    }

    @Test
    fun `an enabled empty slot is available`() {
        val result = rule.check(slot(), wednesday, emptyList(), today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `a disabled slot returns SLOT_DISABLED`() {
        val result = rule.check(slot(enabled = false), wednesday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.SLOT_DISABLED, result)
    }

    @Test
    fun `a disabled slot wins over any other reason`() {
        // A past date and a mismatched day at the same time: it must report the first condition in the order.
        val result = rule.check(
            slot(enabled = false, dayOfWeek = DayOfWeek.MONDAY),
            LocalDate.of(2026, 9, 1),
            emptyList(),
            today,
            config,
        )

        assertUnavailable(UnavailabilityReason.SLOT_DISABLED, result)
    }

    @Test
    fun `a date before today returns DATE_IN_PAST`() {
        val lastWednesday = LocalDate.of(2026, 9, 9)

        val result = rule.check(slot(), lastWednesday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.DATE_IN_PAST, result)
    }

    @Test
    fun `today's date is available`() {
        val result = rule.check(slot(), today, emptyList(), today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `a past date wins over the day that does not match`() {
        val pastMonday = LocalDate.of(2026, 9, 7)

        val result = rule.check(slot(), pastMonday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.DATE_IN_PAST, result)
    }

    @Test
    fun `a weekday different from the slot's returns DAY_MISMATCH`() {
        val result = rule.check(slot(), thursday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.DAY_MISMATCH, result)
    }

    @Test
    fun `capacity reached by one pending appointment returns CAPACITY_REACHED`() {
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.PENDING))

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `a confirmed appointment occupies a place`() {
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED))

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `a rescheduled appointment also occupies a place`() {
        // Documented decision: RESCHEDULED points at a new date and slot, and those stay occupied.
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.RESCHEDULED))

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `appointments in a final state free the place`() {
        val existing = listOf(
            appointment("APT-001", wednesday, status = AppointmentStatus.REJECTED),
            appointment("APT-002", wednesday, status = AppointmentStatus.CANCELLED),
            appointment("APT-003", wednesday, status = AppointmentStatus.ATTENDED),
        )

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `appointments in another slot do not consume the capacity`() {
        val existing = listOf(
            appointment("APT-001", wednesday, slotId = "SLT-999", status = AppointmentStatus.CONFIRMED),
        )

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `appointments on another date do not consume the capacity`() {
        val nextWednesday = LocalDate.of(2026, 9, 23)
        val existing = listOf(
            appointment("APT-001", nextWednesday, status = AppointmentStatus.CONFIRMED),
        )

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `with a capacity of two, a single appointment leaves a free place`() {
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED))

        val result = rule.check(slot(capacity = 2), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `with a capacity of two and two appointments the slot is full`() {
        val existing = listOf(
            appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED),
            appointment("APT-002", wednesday, status = AppointmentStatus.PENDING),
        )

        val result = rule.check(slot(capacity = 2), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `a slot with no declared capacity uses the system default capacity`() {
        val relaxed = SystemConfig(defaultSlotCapacity = 3)
        val existing = listOf(
            appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED),
            appointment("APT-002", wednesday, status = AppointmentStatus.PENDING),
        )

        val result = rule.check(slot(capacity = 0), wednesday, existing, today, config = relaxed)

        assertEquals(AvailabilityResult.Available, result)
        assertEquals(3, rule.effectiveCapacity(slot(capacity = 0), relaxed))
    }

    @Test
    fun `confirming excludes the appointment itself so it is not compared with itself`() {
        val target = appointment("APT-001", wednesday, status = AppointmentStatus.PENDING)
        val others = listOf(target).filter { it.id != target.id }

        val result = rule.check(slot(capacity = 1), wednesday, others, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `the count overload reports CAPACITY_REACHED when the places taken equal the capacity`() {
        val result = rule.check(slot(capacity = 2), wednesday, occupied = 2, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `the count overload is available while a place is left`() {
        val result = rule.check(slot(capacity = 2), wednesday, occupied = 1, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `the count overload keeps the order of the checks`() {
        val disabledAndFull = rule.check(slot(enabled = false), wednesday, occupied = 5, today, config)
        val wrongDayAndFull = rule.check(slot(), thursday, occupied = 5, today, config)

        assertUnavailable(UnavailabilityReason.SLOT_DISABLED, disabledAndFull)
        assertUnavailable(UnavailabilityReason.DAY_MISMATCH, wrongDayAndFull)
    }

    @Test
    fun `the list version agrees with the count overload`() {
        val existing = listOf(
            appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED),
            appointment("APT-002", wednesday, status = AppointmentStatus.CANCELLED),
        )
        val occupied = rule.countOccupying("SLT-001", wednesday, existing)

        assertEquals(
            rule.check(slot(capacity = 1), wednesday, existing, today, config),
            rule.check(slot(capacity = 1), wednesday, occupied, today, config),
        )
    }
}