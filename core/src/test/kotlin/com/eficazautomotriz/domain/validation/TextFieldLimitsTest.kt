package com.eficazautomotriz.domain.validation

import com.eficazautomotriz.domain.config.SystemConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Limits the client fields type against. The screens read these; they do not hardcode them. */
class TextFieldLimitsTest {

    @Test
    fun `mileage input length counts the digits and the thousands commas`() {
        assertEquals(9, TextFields.mileageMaxLength(2_000_000))
        assertEquals(9, TextFields.mileageMaxLength(SystemConfig().maxMileage))
        assertEquals(7, TextFields.mileageMaxLength(999_999))
        assertEquals(5, TextFields.mileageMaxLength(1_000))
        assertEquals(3, TextFields.mileageMaxLength(999))
        assertEquals(1, TextFields.mileageMaxLength(0))
    }

    @Test
    fun `year and search expose the limits the fields type against`() {
        assertEquals(4, TextFields.YEAR.maxLength)
        assertEquals(40, TextFields.SEARCH.maxLength)
    }
}