package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.TimeSlot
import java.time.DayOfWeek

class TimeSlotRepository : InMemoryRepository<TimeSlot>("SLT") {

    fun findByDay(dayOfWeek: DayOfWeek): List<TimeSlot> =
        findAll().filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
}
