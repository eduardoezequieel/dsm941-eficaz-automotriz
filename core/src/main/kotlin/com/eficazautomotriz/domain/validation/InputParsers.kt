package com.eficazautomotriz.domain.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Pure parsing of what the user types into a text field. It resolves only the
 * FORMAT: ranges and business meaning are validated by the domain and the use cases.
 *
 * Both functions trim the leftover spaces and return null for any
 * invalid input instead of throwing.
 */
object InputParsers {

    /** A date in dd/mm/yyyy form; a day that does not exist is rejected instead of being adjusted. */
    fun parseDate(raw: String): LocalDate? = try {
        LocalDate.parse(raw.trim(), DATE_FORMAT)
    } catch (e: DateTimeParseException) {
        null
    }

    /** An integer with an optional thousands separator (30,000); misplaced commas or overflow yield null. */
    fun parseInt(raw: String): Int? {
        val value = raw.trim()
        return if (INTEGER.matches(value)) value.replace(",", "").toIntOrNull() else null
    }

    /** Digits with a well-placed thousands separator; "1,2,3" and "12a" are not numbers. */
    private val INTEGER = Regex("^-?\\d{1,3}(,\\d{3})+$|^-?\\d{1,10}$")

    /**
     * `ResolverStyle.STRICT` is deliberate: with the default style, 31/02/2026 is
     * silently adjusted to 28/02 and the user books a day they never typed. The strict
     * style requires the era-independent year (`uuuu`, not `yyyy`).
     */
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)
}
