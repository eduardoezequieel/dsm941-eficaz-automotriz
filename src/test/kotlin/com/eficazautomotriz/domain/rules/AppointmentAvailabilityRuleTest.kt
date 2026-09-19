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

    // Miercoles 16 de septiembre de 2026, usado como «hoy» en todas las pruebas.
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
    fun `franja habilitada y vacia esta disponible`() {
        val result = rule.check(slot(), wednesday, emptyList(), today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `franja deshabilitada devuelve SLOT_DISABLED`() {
        val result = rule.check(slot(enabled = false), wednesday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.SLOT_DISABLED, result)
    }

    @Test
    fun `una franja deshabilitada gana sobre cualquier otro motivo`() {
        // Fecha pasada y dia distinto a la vez: debe reportar la primera condicion del orden.
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
    fun `fecha anterior a hoy devuelve DATE_IN_PAST`() {
        val lastWednesday = LocalDate.of(2026, 9, 9)

        val result = rule.check(slot(), lastWednesday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.DATE_IN_PAST, result)
    }

    @Test
    fun `la fecha de hoy si esta disponible`() {
        val result = rule.check(slot(), today, emptyList(), today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `la fecha pasada gana sobre el dia que no coincide`() {
        val pastMonday = LocalDate.of(2026, 9, 7)

        val result = rule.check(slot(), pastMonday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.DATE_IN_PAST, result)
    }

    @Test
    fun `dia de la semana distinto al de la franja devuelve DAY_MISMATCH`() {
        val result = rule.check(slot(), thursday, emptyList(), today, config)

        assertUnavailable(UnavailabilityReason.DAY_MISMATCH, result)
    }

    @Test
    fun `capacidad alcanzada con una cita pendiente devuelve CAPACITY_REACHED`() {
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.PENDING))

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `una cita confirmada ocupa cupo`() {
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED))

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `una cita reprogramada tambien ocupa cupo`() {
        // Decision documentada: RESCHEDULED apunta a una fecha y franja nuevas que quedan ocupadas.
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.RESCHEDULED))

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `las citas en estado final liberan el cupo`() {
        val existing = listOf(
            appointment("APT-001", wednesday, status = AppointmentStatus.REJECTED),
            appointment("APT-002", wednesday, status = AppointmentStatus.CANCELLED),
            appointment("APT-003", wednesday, status = AppointmentStatus.ATTENDED),
        )

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `las citas de otra franja no consumen la capacidad`() {
        val existing = listOf(
            appointment("APT-001", wednesday, slotId = "SLT-999", status = AppointmentStatus.CONFIRMED),
        )

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `las citas de otra fecha no consumen la capacidad`() {
        val nextWednesday = LocalDate.of(2026, 9, 23)
        val existing = listOf(
            appointment("APT-001", nextWednesday, status = AppointmentStatus.CONFIRMED),
        )

        val result = rule.check(slot(capacity = 1), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `con capacidad de dos una sola cita deja cupo libre`() {
        val existing = listOf(appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED))

        val result = rule.check(slot(capacity = 2), wednesday, existing, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }

    @Test
    fun `con capacidad de dos y dos citas la franja se llena`() {
        val existing = listOf(
            appointment("APT-001", wednesday, status = AppointmentStatus.CONFIRMED),
            appointment("APT-002", wednesday, status = AppointmentStatus.PENDING),
        )

        val result = rule.check(slot(capacity = 2), wednesday, existing, today, config)

        assertUnavailable(UnavailabilityReason.CAPACITY_REACHED, result)
    }

    @Test
    fun `una franja sin capacidad declarada usa la capacidad por omision del sistema`() {
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
    fun `al confirmar se excluye la propia cita para no compararla consigo misma`() {
        val target = appointment("APT-001", wednesday, status = AppointmentStatus.PENDING)
        val others = listOf(target).filter { it.id != target.id }

        val result = rule.check(slot(capacity = 1), wednesday, others, today, config)

        assertEquals(AvailabilityResult.Available, result)
    }
}
