package com.eficazautomotriz.app.ui.client

import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.Vehicle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Texts of the client mockups (f2–f7): Spanish day and month names, 12-hour times with
 * "a.m."/"p.m." and the vehicle lines. Names are hardcoded on purpose: locale data differs
 * between the JVM (CLDR) and Android (ICU) ("vie." vs "vie", "a. m." with narrow spaces).
 */
internal object ClientText {
    private val fuelByPlate = mapOf(
        "P123456" to "Gasolina",
        "P987321" to "Diésel",
        "P456789" to "Gasolina",
    )

    private val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    private val shortDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    /** Calendar header: "Agosto 2026". */
    fun monthYear(date: LocalDate): String =
        "${months[date.monthValue - 1].replaceFirstChar { it.uppercase() }} ${date.year}"

    /** Weekday initials of the Monday-first calendar grid. */
    val weekInitials = listOf("L", "M", "M", "J", "V", "S", "D")

    /** f2 next appointment: "Viernes 21 de agosto". */
    fun longDayDate(date: LocalDate): String =
        "${days[date.dayOfWeek.value - 1]} ${date.dayOfMonth} de ${months[date.monthValue - 1]}"

    /** f5 card: "Vie 21 ago 2026". */
    fun shortDate(date: LocalDate): String = "${shortDayMonth(date)} ${date.year}"

    /** f6 slot header and summary: "Vie 21 ago". */
    fun shortDayMonth(date: LocalDate): String =
        "${shortDays[date.dayOfWeek.value - 1]} ${date.dayOfMonth} ${shortMonth(date)}"

    /** f7 associated appointment: "12 ago 2026". */
    fun compactDate(date: LocalDate): String = "${date.dayOfMonth} ${shortMonth(date)} ${date.year}"

    /** f2 last service: "12 de julio de 2026". */
    fun dayMonthYear(date: LocalDate): String = "${date.dayOfMonth} de ${months[date.monthValue - 1]} de ${date.year}"

    private fun shortMonth(date: LocalDate): String = months[date.monthValue - 1].take(3)

    /** f6 slot pill: "9:00", "1:00" (12-hour clock, no suffix). */
    fun hour12(time: LocalTime): String {
        val hour = time.hour % 12
        return "${if (hour == 0) 12 else hour}:${"%02d".format(time.minute)}"
    }

    private fun suffix(time: LocalTime): String = if (time.hour < 12) "a.m." else "p.m."

    /** "9:12 a.m.", "3:40 p.m.". */
    fun time12(time: LocalTime): String = "${hour12(time)} ${suffix(time)}"

    /** "10:00 – 11:00 a.m." when both ends share the suffix, else "11:00 a.m. – 12:00 p.m.". */
    fun range12(start: LocalTime, end: LocalTime): String =
        if (suffix(start) == suffix(end)) {
            "${hour12(start)} – ${hour12(end)} ${suffix(end)}"
        } else {
            "${time12(start)} – ${time12(end)}"
        }

    fun slotRange(slot: TimeSlot?): String = slot?.let { range12(it.startTime, it.endTime) } ?: "Franja no disponible"

    /** f7 timeline: "12/07/2026 · 9:12 a.m.". */
    fun dateTime12(value: LocalDateTime): String = "${Formats.date(value.toLocalDate())} · ${time12(value.toLocalTime())}"

    /** "Toyota Corolla 2016". */
    fun vehicleName(vehicle: Vehicle): String = "${vehicle.make} ${vehicle.model} ${vehicle.year}"

    /** "Toyota Corolla 2016 · P123-456". */
    fun vehicleLine(vehicle: Vehicle): String = "${vehicleName(vehicle)} · ${Formats.plate(vehicle.plate)}"

    /** f2 alert: "Nissan Frontier · P987-321". */
    fun vehicleShortLine(vehicle: Vehicle): String = "${vehicle.make} ${vehicle.model} · ${Formats.plate(vehicle.plate)}"

    /**
     * f3/f4 "Placa P123-456 · Gasolina". The domain vehicle has no fuel field, so only the
     * three mockup plates carry one; every other plate falls back to the model year.
     */
    fun plateDetail(vehicle: Vehicle): String {
        val fuel = fuelByPlate[vehicle.plate.trim().uppercase()]
        val extra = fuel ?: vehicle.year.toString()
        return "Placa ${Formats.plate(vehicle.plate)} · $extra"
    }

    /**
     * f2 alert sentence start: "Cambio de aceite recomendado". The catalog keeps
     * "y filtro" on the maintenance plan (f4); the home alert uses the shorter name.
     */
    fun alertAction(typeName: String): String {
        val name = if (typeName.startsWith("Cambio de aceite")) "Cambio de aceite" else typeName
        val head = name.substringBefore(" ").lowercase()
        val adjective = if (head.endsWith("ción") || head.endsWith("sión")) "recomendada" else "recomendado"
        return "$name $adjective"
    }

    fun vehicleCount(count: Int): String = if (count == 1) "1 vehículo registrado" else "$count vehículos registrados"

    fun imageCount(count: Int): String = when (count) {
        0 -> "Sin imágenes"
        1 -> "1 imagen"
        else -> "$count imágenes"
    }
}
