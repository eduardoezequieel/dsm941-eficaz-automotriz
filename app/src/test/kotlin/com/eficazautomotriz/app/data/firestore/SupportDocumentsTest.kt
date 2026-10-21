package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.SupportDocuments.OccupancyChange
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SupportDocumentsTest {

    private val day = LocalDate.of(2026, 10, 19)
    private val nextDay = LocalDate.of(2026, 10, 20)

    private fun appointment(
        status: AppointmentStatus,
        date: LocalDate = day,
        slotId: String = "SLT-002",
        id: String = "APT-010",
    ) = Appointment(
        id = id, clientId = "uid-1", vehicleId = "VEH-001", serviceTypeId = "SVT-001",
        date = date, slotId = slotId, status = status, reason = null,
        requestedAt = LocalDateTime.of(2026, 10, 11, 9, 0),
    )

    @Test
    fun `occupancy key is the ISO date and the slot id`() {
        assertEquals("2026-10-19_SLT-002", SupportDocuments.occupancyKey(day, "SLT-002"))
    }

    @Test
    fun `plate key is trimmed and upper case`() {
        assertEquals("P123456", SupportDocuments.plateKey(" p123456 "))
    }

    @Test
    fun `plate claim stores the vehicle and its owner`() {
        val vehicle = Vehicle("VEH-007", "uid-1", "P123456", "Kia", "Rio", 2022, 1_000, day)

        assertEquals(mapOf("vehicleId" to "VEH-007", "ownerId" to "uid-1"), SupportDocuments.plateClaim(vehicle))
    }

    @Test
    fun `a new pending appointment takes one place`() {
        val changes = SupportDocuments.occupancyChanges(null, appointment(AppointmentStatus.PENDING))

        assertEquals(listOf(OccupancyChange(day, "SLT-002", +1)), changes)
    }

    @Test
    fun `confirming keeps the same place`() {
        val changes = SupportDocuments.occupancyChanges(
            appointment(AppointmentStatus.PENDING), appointment(AppointmentStatus.CONFIRMED),
        )

        assertTrue(changes.isEmpty())
    }

    @Test
    fun `cancelling, rejecting or attending releases the place`() {
        listOf(AppointmentStatus.CANCELLED, AppointmentStatus.REJECTED, AppointmentStatus.ATTENDED).forEach { final ->
            val changes = SupportDocuments.occupancyChanges(appointment(AppointmentStatus.CONFIRMED), appointment(final))

            assertEquals(listOf(OccupancyChange(day, "SLT-002", -1)), changes, "status $final")
        }
    }

    @Test
    fun `rescheduling moves the place to the new date and slot`() {
        val changes = SupportDocuments.occupancyChanges(
            appointment(AppointmentStatus.PENDING),
            appointment(AppointmentStatus.RESCHEDULED, date = nextDay, slotId = "SLT-010"),
        )

        assertEquals(
            listOf(OccupancyChange(day, "SLT-002", -1), OccupancyChange(nextDay, "SLT-010", +1)),
            changes,
        )
    }

    @Test
    fun `saving a final appointment again changes nothing`() {
        val changes = SupportDocuments.occupancyChanges(
            appointment(AppointmentStatus.CANCELLED), appointment(AppointmentStatus.CANCELLED),
        )

        assertTrue(changes.isEmpty())
    }

    @Test
    fun `next count adds and subtracts one`() {
        assertEquals(1L, SupportDocuments.nextCount(null, +1))
        assertEquals(3L, SupportDocuments.nextCount(2, +1))
        assertEquals(1L, SupportDocuments.nextCount(2, -1))
    }

    @Test
    fun `releasing a place that was never counted is skipped instead of going negative`() {
        assertNull(SupportDocuments.nextCount(null, -1))
        assertNull(SupportDocuments.nextCount(0, -1))
    }

    @Test
    fun `occupancy by slot ignores empty counters and malformed documents`() {
        val documents = listOf(
            mapOf("date" to "2026-10-19", "slotId" to "SLT-002", "count" to 2L),
            mapOf("date" to "2026-10-19", "slotId" to "SLT-003", "count" to 0L),
            mapOf("date" to "2026-10-19", "count" to 1L),
        )

        assertEquals(mapOf("SLT-002" to 2), SupportDocuments.occupancyBySlot(documents))
    }

    @Test
    fun `backfill rebuilds the counters from the occupying appointments only`() {
        val appointments = listOf(
            appointment(AppointmentStatus.PENDING, id = "APT-001"),
            appointment(AppointmentStatus.CONFIRMED, id = "APT-002"),
            appointment(AppointmentStatus.CANCELLED, id = "APT-003"),
            appointment(AppointmentStatus.RESCHEDULED, date = nextDay, slotId = "SLT-010", id = "APT-004"),
        )

        assertEquals(
            mapOf(
                "2026-10-19_SLT-002" to mapOf("date" to "2026-10-19", "slotId" to "SLT-002", "count" to 2L),
                "2026-10-20_SLT-010" to mapOf("date" to "2026-10-20", "slotId" to "SLT-010", "count" to 1L),
            ),
            SupportDocuments.occupancyFrom(appointments),
        )
    }

    @Test
    fun `readable ids keep the PREFIX-NNN format`() {
        assertEquals("VEH-007", SupportDocuments.formatId("VEH", 7))
        assertEquals("VEH-1234", SupportDocuments.formatId("VEH", 1234))
        assertEquals(7, SupportDocuments.idNumber("VEH-007", "VEH"))
        assertEquals(0, SupportDocuments.idNumber("seed-vehicle", "VEH"))
    }
}
