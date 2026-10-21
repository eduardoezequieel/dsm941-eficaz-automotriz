package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.TimeSlot
import java.time.DayOfWeek

interface TimeSlotRepository : Repository<TimeSlot> {
    /** Slots for the day, ordered by start time. */
    fun findByDay(dayOfWeek: DayOfWeek): List<TimeSlot>
}
