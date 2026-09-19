package com.eficazautomotriz.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class TimeSlot(
    override val id: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val enabled: Boolean,
    val capacity: Int,
) : Identifiable