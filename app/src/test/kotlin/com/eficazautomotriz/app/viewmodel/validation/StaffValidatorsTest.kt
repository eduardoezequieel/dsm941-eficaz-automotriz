package com.eficazautomotriz.app.viewmodel.validation

import com.eficazautomotriz.domain.config.SystemConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Early validation used by the staff screens (closing orders, reasons, report periods). */
class StaffValidatorsTest {

    private val config = SystemConfig()
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun `final mileage below the vehicle odometer is rejected`() {
        val (error, value) = MileageValidator.validate("62,699", lastKnown = 62_700, config = config)
        assertNotNull(error)
        assertNull(value)
    }

    @Test
    fun `final mileage equal to or above the odometer is accepted with thousands separator`() {
        assertEquals(null to 62_700, MileageValidator.validate("62,700", lastKnown = 62_700, config = config))
        assertEquals(null to 65_000, MileageValidator.validate(" 65,000 ", lastKnown = 62_700, config = config))
    }

    @Test
    fun `final mileage above the odometer limit is rejected`() {
        assertNotNull(MileageValidator.validate("${config.maxMileage + 1}", lastKnown = 0, config = config).first)
    }

    @Test
    fun `the mileage field is long enough for the ceiling written with thousands separators`() {
        assertEquals("2,000,000".length, MileageValidator.inputMaxLength(config.maxMileage))
        assertEquals(5, MileageValidator.inputMaxLength(1_000))
        assertEquals(3, MileageValidator.inputMaxLength(999))
        assertEquals(1, MileageValidator.inputMaxLength(0))
    }

    @Test
    fun `reject and reschedule reasons cannot be blank or too long`() {
        assertNotNull(ReasonValidator.error("   "))
        assertNotNull(ReasonValidator.error("x".repeat(201)))
        assertNull(ReasonValidator.error("El cliente pidio otro dia"))
    }

    @Test
    fun `default report period runs from the first day of the month to today`() {
        assertEquals(LocalDate.of(2026, 10, 1) to today, ReportPeriodValidator.defaultPeriod(today))
        val (from, to) = ReportPeriodValidator.defaultPeriod(today)
        assertNull(ReportPeriodValidator.error(from, to, today, config))
    }

    @Test
    fun `report period must be ordered`() {
        assertNotNull(ReportPeriodValidator.error(today, today.minusDays(1), today, config))
        assertNull(ReportPeriodValidator.error(today, today, today, config))
    }

    @Test
    fun `report period stays inside the history window and scheduling horizon`() {
        val earliest = today.minusYears(config.reportHistoryYears)
        assertNull(ReportPeriodValidator.error(earliest, today, today, config))
        assertNotNull(ReportPeriodValidator.error(earliest.minusDays(1), today, today, config))
        val latest = today.plusYears(config.schedulingHorizonYears)
        assertNull(ReportPeriodValidator.error(today, latest, today, config))
        assertNotNull(ReportPeriodValidator.error(today, latest.plusDays(1), today, config))
    }
}
