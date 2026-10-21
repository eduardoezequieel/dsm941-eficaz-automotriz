package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.TimeSlotRepository
import com.eficazautomotriz.domain.model.TimeSlot
import java.time.DayOfWeek

class InMemoryTimeSlotRepository : InMemoryRepository<TimeSlot>("SLT"), TimeSlotRepository {

    override fun findByDay(dayOfWeek: DayOfWeek): List<TimeSlot> =
        findAll().filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
}