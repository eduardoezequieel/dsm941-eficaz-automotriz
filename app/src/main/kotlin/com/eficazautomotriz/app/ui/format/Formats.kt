package com.eficazautomotriz.app.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Display formats for Spanish as used in El Salvador. */
object Formats {
    private val locale = Locale.forLanguageTag("es-SV")
    private val date = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    private val time = DateTimeFormatter.ofPattern("HH:mm", locale)

    fun date(value: LocalDate): String = date.format(value)
    fun dateTime(value: LocalDateTime): String = dateTime.format(value)
    fun time(value: LocalTime): String = time.format(value)
    fun timeRange(start: LocalTime, end: LocalTime): String = "${time(start)}–${time(end)}"
    fun km(value: Int): String = "%,d km".format(locale, value)
    fun day(value: DayOfWeek): String = value.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
    fun longDate(value: LocalDate): String = "${day(value.dayOfWeek)} ${date(value)}"

    /** Display form of a stored plate: `P123456` → `P123-456`, `PA12BB3` → `PA12-BB3`. */
    fun plate(value: String): String {
        val compact = value.trim().uppercase().replace("-", "")
        return if (compact.length == 7 && compact.all { it.isLetterOrDigit() }) {
            "${compact.take(4)}-${compact.drop(4)}"
        } else {
            compact
        }
    }
}
