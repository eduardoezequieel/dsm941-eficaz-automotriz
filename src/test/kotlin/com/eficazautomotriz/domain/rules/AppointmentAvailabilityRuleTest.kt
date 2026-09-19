package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.SystemConfig
import com.eficazautomotriz.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentAvailabilityRuleTest {
    private val today = LocalDate.of(2026, 9, 14)
    private val rule = AppointmentAvailabilityRule(today = today)

    @Test
    fun `franja disponible`() {
        val result = rule.check(
            date = today,
            timeSlot = timeSlot(capacity = 0),
            appointments = listOf(appointment()),
            systemConfig = systemConfig(defaultSlotCapacity = 2)
        )

        assertTrue(result is AvailabilityResult.Available)
    }

    @Test
    fun `franja deshabilitada`() {
        val result = rule.check(
            date = today,
            timeSlot = timeSlot(enabled = false),
            appointments = emptyList(),
            systemConfig = systemConfig()
        )

        assertUnavailable(result, UnavailabilityReason.SLOT_DISABLED)
    }

    @Test
    fun `fecha pasada`() {
        val result = rule.check(
            date = today.minusDays(1),
            timeSlot = timeSlot(),
            appointments = emptyList(),
            systemConfig = systemConfig()
        )

        assertUnavailable(result, UnavailabilityReason.DATE_IN_PAST)
    }

    @Test
    fun `dia de semana incorrecto`() {
        val result = rule.check(
            date = today,
            timeSlot = timeSlot(dayOfWeek = DayOfWeek.TUESDAY),
            appointments = emptyList(),
            systemConfig = systemConfig()
        )

        assertUnavailable(result, UnavailabilityReason.DAY_MISMATCH)
    }

    @Test
    fun `capacidad alcanzada`() {
        val result = rule.check(
            date = today,
            timeSlot = timeSlot(capacity = 2),
            appointments = listOf(
                appointment(occupiesCapacity = true),
                appointment(occupiesCapacity = true),
                appointment(occupiesCapacity = false)
            ),
            systemConfig = systemConfig()
        )

        assertUnavailable(result, UnavailabilityReason.CAPACITY_REACHED)
    }

    private fun assertUnavailable(
        result: AvailabilityResult,
        expectedReason: UnavailabilityReason
    ) {
        assertTrue(result is AvailabilityResult.Unavailable)
        assertEquals(expectedReason, (result as AvailabilityResult.Unavailable).reason)
    }

    private fun timeSlot(
        enabled: Boolean = true,
        dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        capacity: Int = 2
    ): TimeSlot {
        return TimeSlot(
            enabled = enabled,
            dayOfWeek = dayOfWeek,
            capacity = capacity
        )
    }

    private fun appointment(
        occupiesCapacity: Boolean = true
    ): Appointment {
        return Appointment(
            occupiesCapacity = occupiesCapacity
        )
    }

    private fun systemConfig(
        defaultSlotCapacity: Int = 2
    ): SystemConfig {
        return SystemConfig(
            defaultSlotCapacity = defaultSlotCapacity
        )
    }
}
