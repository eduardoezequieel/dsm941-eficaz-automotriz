package com.eficazautomotriz.app.ui.staff

import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.Vehicle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Short date and 12-hour time formats of the staff mockups (f8): "Lun 24 ago",
 * "9:00 – 10:00 a.m.", "recibido a las 8:30 a.m.". Fixed tables instead of the JVM locale,
 * whose Spanish abbreviations carry a trailing dot ("lun.", "ago.").
 */
object StaffFormats {
    private val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    private val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** "Lun 24 ago". */
    fun shortDate(date: LocalDate): String =
        "${days[date.dayOfWeek.value - 1]} ${date.dayOfMonth} ${months[date.monthValue - 1]}"

    /** "9:00 a.m.", "12:30 p.m." (noon is p.m., midnight is 12:00 a.m.). */
    fun time12(time: LocalTime): String = "${clock(time)} ${meridiem(time)}"

    /** "9:00 – 10:00 a.m." when both ends share the meridiem, "11:00 a.m. – 12:00 p.m." otherwise. */
    fun timeRange12(start: LocalTime, end: LocalTime): String =
        if (meridiem(start) == meridiem(end)) {
            "${clock(start)} – ${clock(end)} ${meridiem(end)}"
        } else {
            "${time12(start)} – ${time12(end)}"
        }

    /** Date line of an appointment card: "Lun 24 ago · 9:00 – 10:00 a.m.". */
    fun appointmentLine(date: LocalDate, slot: TimeSlot?): String =
        "${shortDate(date)} · " + (slot?.let { timeRange12(it.startTime, it.endTime) } ?: "Franja no disponible")

    /** "recibido a las 8:30 a.m." for today, "recibido el Lun 24 ago · 8:30 a.m." for other days. */
    fun receivedLine(receivedAt: LocalDateTime, today: LocalDate): String =
        if (receivedAt.toLocalDate() == today) {
            "recibido a las ${time12(receivedAt.toLocalTime())}"
        } else {
            "recibido el ${shortDate(receivedAt.toLocalDate())} · ${time12(receivedAt.toLocalTime())}"
        }

    /** "Nissan Frontier · P987-321". */
    fun vehicleLine(vehicle: Vehicle?, fallback: String): String =
        vehicle?.let { "${it.make} ${it.model} · ${Formats.plate(it.plate)}" } ?: fallback

    private fun clock(time: LocalTime): String {
        val hour = time.hour % 12
        return "${if (hour == 0) 12 else hour}:${time.minute.toString().padStart(2, '0')}"
    }

    private fun meridiem(time: LocalTime): String = if (time.hour < 12) "a.m." else "p.m."
}
