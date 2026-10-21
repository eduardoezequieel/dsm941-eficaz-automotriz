package com.eficazautomotriz.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.domain.validation.TextFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class InputMasksTest {

    private fun typePlate(typed: String) = InputFilters.apply(typed, InputFilters.Plate, PLATE_MAX_LENGTH)

    @Test
    fun `the plate filter uppercases, drops symbols and forces the leading P`() {
        assertEquals("P123AB2", typePlate("p123-ab2"))
        assertEquals("PA12BB3", typePlate("PA12 BB3"))
        assertEquals("P123", typePlate("123"))
        assertEquals("", typePlate("-- "))
    }

    @Test
    fun `typing a plate without the P keeps the digits in order`() {
        // Replays the IME: each key goes in at the cursor, then the filter rewrites the text.
        var text = ""
        var cursor = 0
        for (key in "123AB2") {
            val typed = text.take(cursor) + key + text.drop(cursor)
            cursor = InputFilters.cursorAfter(typed, cursor + 1, InputFilters.Plate, PLATE_MAX_LENGTH)
            text = typePlate(typed)
        }
        assertEquals("P123AB2", text)
        assertEquals(text.length, cursor)
    }

    @Test
    fun `the cursor stays put when the filter drops or keeps characters`() {
        fun cursor(typed: String, at: Int) = InputFilters.cursorAfter(typed, at, InputFilters.Plate, PLATE_MAX_LENGTH)
        assertEquals(2, cursor("1", 1))
        assertEquals(2, cursor("P1-", 3))
        assertEquals(1, cursor("P23", 1))
        assertEquals(PLATE_MAX_LENGTH, cursor("P1234567", 8))
    }

    @Test
    fun `the plate filter stops at seven characters`() {
        assertEquals("P123456", typePlate("P1234567890"))
    }

    @Test
    fun `the plate mask draws the hyphen after the fourth character`() {
        fun shown(raw: String) = PlateVisualTransformation.filter(AnnotatedString(raw)).text.text
        assertEquals("P12", shown("P12"))
        assertEquals("P123", shown("P123"))
        assertEquals("P123-A", shown("P123A"))
        assertEquals("P123-AB2", shown("P123AB2"))
    }

    @Test
    fun `every cursor offset maps both ways inside the text`() {
        for (length in 0..PLATE_MAX_LENGTH) {
            val mapping = PlateOffsetMapping(length)
            val shownLength = if (length > 4) length + 1 else length
            for (offset in 0..length) {
                val transformed = mapping.originalToTransformed(offset)
                assertTrue(transformed in 0..shownLength, "length=$length offset=$offset -> $transformed")
                assertEquals(offset, mapping.transformedToOriginal(transformed))
            }
            for (offset in 0..shownLength) {
                assertTrue(mapping.transformedToOriginal(offset) in 0..length)
            }
        }
    }

    @Test
    fun `digit filters keep only what the parsers accept`() {
        assertEquals("2019", InputFilters.apply("20a1-9", InputFilters.Digits, 4))
        assertEquals("30,000", InputFilters.apply("30,000 km", InputFilters.DigitsWithThousands, null))
        assertEquals("a b", InputFilters.apply("a\nb", InputFilters.SingleLine, null))
    }

    @Test
    fun `typing and pasting a plate keep the compact value and a cursor that round-trips`() {
        var typed = ""
        for (ch in "p123ab2") {
            typed = InputFilters.apply(typed + ch, InputFilters.Plate, PLATE_MAX_LENGTH)
        }
        assertEquals("P123AB2", typed)
        assertEquals("P123AB2", typePlate("p123-ab2"))
        val shown = PlateVisualTransformation.filter(AnnotatedString(typed)).text.text
        assertEquals("P123-AB2", shown)
        val mapping = PlateOffsetMapping(typed.length)
        val end = mapping.originalToTransformed(typed.length)
        assertEquals(typed.length, mapping.transformedToOriginal(end))
    }

    @Test
    fun `backspace on the hyphen deletes the character before it`() {
        val raw = "P123AB2"
        val mapping = PlateOffsetMapping(raw.length)
        // Shown "P123-AB2": both sides of the hyphen are the same original position, the end of "P123".
        assertEquals(4, mapping.transformedToOriginal(4))
        assertEquals(4, mapping.transformedToOriginal(5))
        // Compose deletes in original coordinates, so a backspace there removes the "3".
        val cursor = mapping.transformedToOriginal(5)
        assertEquals("P12AB2", raw.removeRange(cursor - 1, cursor))
    }

    @Test
    fun `make, model, year, mileage and search stop at their catalog limits`() {
        val makeCap = TextFields.MAKE.maxLength
        assertEquals(makeCap, InputFilters.apply("A".repeat(makeCap + 10) + "\n", InputFilters.SingleLine, makeCap).length)
        assertEquals("Land Rover", InputFilters.apply("Land\nRover", InputFilters.SingleLine, makeCap))
        assertEquals("2019", InputFilters.apply("año 20199", InputFilters.Digits, TextFields.YEAR.maxLength))
        val mileageCap = TextFields.mileageMaxLength(2_000_000)
        assertEquals(9, mileageCap)
        assertEquals("2,000,000", InputFilters.apply("2,000,000 km", InputFilters.DigitsWithThousands, mileageCap))
        assertEquals("200000099", InputFilters.apply("2000000999", InputFilters.DigitsWithThousands, mileageCap))
        val query = InputFilters.apply("P123-456\n" + "x".repeat(80), InputFilters.SingleLine, TextFields.SEARCH.maxLength)
        assertEquals(TextFields.SEARCH.maxLength, query.length)
        assertTrue(query.startsWith("P123-456"))
    }

    @Test
    fun `catalog names keep accents and drop characters the catalog rejects`() {
        assertEquals("Alineación", InputFilters.apply("Alineación", InputFilters.NameCharacters, 60))
        assertEquals("Aceite 1", InputFilters.apply("Aceite #1", InputFilters.NameCharacters, null))
        assertEquals("Mercedes- Benz", InputFilters.apply("Mercedes-\nBenz", InputFilters.NameCharacters, null))
    }

    @Test
    fun `make and model drop symbols and cannot start with a sign`() {
        val make = TextFields.MAKE.maxLength
        assertEquals("j", InputFilters.apply("+++++(((///??!!!!!j", InputFilters.NameCharacters, make))
        assertEquals("Land Rover", InputFilters.apply("Land Rover", InputFilters.NameCharacters, make))
        assertEquals("Mercedes-Benz", InputFilters.apply(" Mercedes-Benz", InputFilters.NameCharacters, make))
    }

    @Test
    fun `slot capacity keeps digits inside the configured ceiling`() {
        val capacity = InputFilters.capacity(20)
        assertEquals("20", InputFilters.apply("2a0", capacity, 2))
        assertEquals("2", InputFilters.apply("25", capacity, 2))
        assertEquals("10", InputFilters.apply("100", capacity, 2))
        assertEquals("0", InputFilters.apply("00", capacity, 2))
        assertEquals("", InputFilters.apply("cupos", capacity, 2))
        assertEquals("9", InputFilters.apply("99", capacity, 2))
    }

    @Test
    fun `stored plates are displayed with the hyphen`() {
        assertEquals("P123-456", Formats.plate("P123456"))
        assertEquals("PA12-BB3", Formats.plate("pa12bb3"))
    }

    @Test
    fun `the phone filter keeps eight digits and drops the hyphen`() {
        val cap = TextFields.PHONE.maxLength
        assertEquals("71234567", InputFilters.apply("7123-4567", InputFilters.Phone, cap))
        assertEquals("71234567", InputFilters.apply("71234567890", InputFilters.Phone, null))
        assertEquals("50371234", InputFilters.apply("+503 7123-4567", InputFilters.Phone, cap))
        assertEquals("", InputFilters.apply("----", InputFilters.Phone, cap))
    }

    @Test
    fun `the phone mask draws the hyphen after the fourth digit`() {
        fun shown(raw: String) = PhoneVisualTransformation.filter(AnnotatedString(raw)).text.text
        assertEquals("7123", shown("7123"))
        assertEquals("7123-4", shown("71234"))
        assertEquals("7123-4567", shown("71234567"))
    }

    @Test
    fun `every phone cursor offset maps both ways inside the text`() {
        for (length in 0..TextFields.PHONE.maxLength) {
            val mapping = PhoneOffsetMapping(length)
            val shownLength = if (length > 4) length + 1 else length
            for (offset in 0..length) {
                val transformed = mapping.originalToTransformed(offset)
                assertTrue(transformed in 0..shownLength, "length=$length offset=$offset -> $transformed")
                assertEquals(offset, mapping.transformedToOriginal(transformed))
            }
            for (offset in 0..shownLength) {
                assertTrue(mapping.transformedToOriginal(offset) in 0..length)
            }
        }
    }

    @Test
    fun `email and name filters drop characters the field can never keep`() {
        assertEquals(
            "ana@ejemplo.com",
            InputFilters.apply(" ana @ejemplo.com\n", InputFilters.Email, TextFields.EMAIL.maxLength),
        )
        assertEquals(
            "a".repeat(TextFields.EMAIL.maxLength),
            InputFilters.apply("a".repeat(TextFields.EMAIL.maxLength + 20), InputFilters.Email, TextFields.EMAIL.maxLength),
        )
        assertEquals("José Pérez", InputFilters.apply("José\nPérez2!", InputFilters.PersonName, TextFields.PERSON_NAME.maxLength))
        assertEquals(
            "A".repeat(TextFields.PERSON_NAME.maxLength),
            InputFilters.apply("A".repeat(80), InputFilters.PersonName, TextFields.PERSON_NAME.maxLength),
        )
    }

    @Test
    fun `a stored phone is shown as four digits, a hyphen and four digits`() {
        assertEquals("7012-3456", displayPhone("70123456"))
        assertEquals("7012-3456", displayPhone("7012-3456"))
        assertEquals("2250-1188", displayPhone("2250-1188"))
        assertEquals("", displayPhone("  "))
        assertEquals("123", displayPhone("123"))
    }
}
