package com.eficazautomotriz.app.viewmodel.staff

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Shortcuts above the custom date pickers on the reports screen. */
enum class ReportPeriodPreset { WEEK, MONTH, QUARTER }

/**
 * Inclusive range ending [today].
 * Week starts on Monday. Quarter starts on the first day of January, April, July or October.
 */
fun reportPresetRange(preset: ReportPeriodPreset, today: LocalDate): Pair<LocalDate, LocalDate> {
    val start = when (preset) {
        ReportPeriodPreset.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        ReportPeriodPreset.MONTH -> today.withDayOfMonth(1)
        ReportPeriodPreset.QUARTER -> LocalDate.of(today.year, today.month.firstMonthOfQuarter(), 1)
    }
    return start to today
}
