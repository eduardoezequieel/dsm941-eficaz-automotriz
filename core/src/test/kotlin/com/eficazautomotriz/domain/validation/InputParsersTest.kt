package com.eficazautomotriz.domain.validation

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * First level of validation: the FORMAT of what was typed. Cases rescued from the stage 2
 * console-reader tests, now covering the pure functions the app uses.
 */
class InputParsersTest {

    // --- Dates ---

    @Test
    fun `a correctly written date is accepted as typed`() {
        assertEquals(LocalDate.of(2026, 9, 20), InputParsers.parseDate("20/09/2026"))
    }

    @Test
    fun `leftover spaces around the date are trimmed`() {
        assertEquals(LocalDate.of(2026, 9, 20), InputParsers.parseDate("  20/09/2026  "))
    }

    @Test
    fun `the 31st of February is rejected instead of being adjusted in silence`() {
        // With the default SMART style this would return 28/02/2026: a date the
        // user never typed.
        assertNull(InputParsers.parseDate("31/02/2026"))
    }

    @Test
    fun `the 29th of February in a non-leap year is rejected`() {
        assertNull(InputParsers.parseDate("29/02/2026"))
    }

    @Test
    fun `the 29th of February in a leap year is accepted`() {
        assertEquals(LocalDate.of(2028, 2, 29), InputParsers.parseDate("29/02/2028"))
    }

    @Test
    fun `month thirteen is rejected`() {
        assertNull(InputParsers.parseDate("01/13/2026"))
    }

    @Test
    fun `text that is not a date is rejected without throwing`() {
        assertNull(InputParsers.parseDate("ayer"))
        assertNull(InputParsers.parseDate("2026-09-20"))
        assertNull(InputParsers.parseDate(""))
    }

    // --- Numbers ---

    @Test
    fun `letters where a number belongs are rejected`() {
        assertNull(InputParsers.parseInt("hola"))
        assertNull(InputParsers.parseInt("12a"))
        assertEquals(3, InputParsers.parseInt("3"))
    }

    @Test
    fun `a well-placed thousands separator is accepted`() {
        assertEquals(30_000, InputParsers.parseInt("30,000"))
        assertEquals(1_250_000, InputParsers.parseInt(" 1,250,000 "))
    }

    @Test
    fun `misplaced commas do not slip through as a different number`() {
        // "1,2,3" used to become 123: the commas were stripped without looking at where they sat.
        assertNull(InputParsers.parseInt("1,2,3"))
        assertEquals(123, InputParsers.parseInt("123"))
    }

    @Test
    fun `a number larger than Int is rejected instead of overflowing`() {
        assertNull(InputParsers.parseInt("99999999999"))
    }

    @Test
    fun `negative numbers are accepted because the caller validates the range`() {
        assertEquals(-3, InputParsers.parseInt("-3"))
    }
}