package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.viewmodel.validation.ReportPeriodValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Week, month and quarter ranges used by the reports segmented control. */
class ReportPeriodsTest {

    @Test
    fun `week runs from Monday through today, including across the new year`() {
        val sunday = LocalDate.of(2026, 10, 11)
        assertEquals(LocalDate.of(2026, 10, 5) to sunday, reportPresetRange(ReportPeriodPreset.WEEK, sunday))

        val monday = LocalDate.of(2026, 10, 5)
        assertEquals(monday to monday, reportPresetRange(ReportPeriodPreset.WEEK, monday))

        val newYear = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2025, 12, 29) to newYear, reportPresetRange(ReportPeriodPreset.WEEK, newYear))
    }

    @Test
    fun `month runs from the first through today and matches the default period`() {
        val today = LocalDate.of(2026, 10, 11)
        assertEquals(LocalDate.of(2026, 10, 1) to today, reportPresetRange(ReportPeriodPreset.MONTH, today))
        assertEquals(ReportPeriodValidator.defaultPeriod(today), reportPresetRange(ReportPeriodPreset.MONTH, today))

        val first = LocalDate.of(2026, 3, 1)
        assertEquals(first to first, reportPresetRange(ReportPeriodPreset.MONTH, first))
    }

    @Test
    fun `quarter starts on the first day of January, April, July or October`() {
        val expectedStartMonth = mapOf(
            1 to 1, 2 to 1, 3 to 1,
            4 to 4, 5 to 4, 6 to 4,
            7 to 7, 8 to 7, 9 to 7,
            10 to 10, 11 to 10, 12 to 10,
        )
        expectedStartMonth.forEach { (month, startMonth) ->
            val today = LocalDate.of(2026, month, 15)
            val (from, to) = reportPresetRange(ReportPeriodPreset.QUARTER, today)
            assertEquals(LocalDate.of(2026, startMonth, 1), from, "month $month")
            assertEquals(today, to, "month $month")
        }
    }
}
