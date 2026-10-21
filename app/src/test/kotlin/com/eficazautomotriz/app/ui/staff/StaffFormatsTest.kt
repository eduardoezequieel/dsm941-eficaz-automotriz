package com.eficazautomotriz.app.ui.staff

import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.Vehicle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Short dates and 12-hour times exactly as the f8 cards print them. */
class StaffFormatsTest {

    @Test
    fun `short date uses fixed Spanish abbreviations without dots`() {
        assertEquals("Lun 24 ago", StaffFormats.shortDate(LocalDate.of(2026, 8, 24)))
        assertEquals("Mié 7 oct", StaffFormats.shortDate(LocalDate.of(2026, 10, 7)))
        assertEquals("Dom 11 oct", StaffFormats.shortDate(LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun `twelve hour clock handles noon and midnight`() {
        assertEquals("8:30 a.m.", StaffFormats.time12(LocalTime.of(8, 30)))
        assertEquals("12:00 p.m.", StaffFormats.time12(LocalTime.NOON))
        assertEquals("12:05 a.m.", StaffFormats.time12(LocalTime.of(0, 5)))
        assertEquals("2:00 p.m.", StaffFormats.time12(LocalTime.of(14, 0)))
    }

    @Test
    fun `time range shares the meridiem when both ends have the same one`() {
        assertEquals("9:00 – 10:00 a.m.", StaffFormats.timeRange12(LocalTime.of(9, 0), LocalTime.of(10, 0)))
        assertEquals("2:00 – 3:00 p.m.", StaffFormats.timeRange12(LocalTime.of(14, 0), LocalTime.of(15, 0)))
        assertEquals("11:00 a.m. – 12:00 p.m.", StaffFormats.timeRange12(LocalTime.of(11, 0), LocalTime.NOON))
    }

    @Test
    fun `appointment line joins date and slot, or says the slot is missing`() {
        val slot = TimeSlot("S1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), true, 2)
        val date = LocalDate.of(2026, 8, 24)
        assertEquals("Lun 24 ago · 9:00 – 10:00 a.m.", StaffFormats.appointmentLine(date, slot))
        assertEquals("Lun 24 ago · Franja no disponible", StaffFormats.appointmentLine(date, null))
    }

    @Test
    fun `received line names only the hour for today`() {
        val today = LocalDate.of(2026, 10, 11)
        assertEquals("recibido a las 8:30 a.m.", StaffFormats.receivedLine(LocalDateTime.of(2026, 10, 11, 8, 30), today))
        assertEquals(
            "recibido el Vie 9 oct · 3:15 p.m.",
            StaffFormats.receivedLine(LocalDateTime.of(2026, 10, 9, 15, 15), today),
        )
    }

    @Test
    fun `vehicle line shows make, model and the display plate`() {
        val vehicle = Vehicle("V1", "C1", "P987321", "Nissan", "Frontier", 2019, 84_100, LocalDate.of(2026, 10, 1))
        assertEquals("Nissan Frontier · P987-321", StaffFormats.vehicleLine(vehicle, "V1"))
        assertEquals("V1", StaffFormats.vehicleLine(null, "V1"))
    }
}
