package com.eficazautomotriz.app.ui.client

import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.domain.model.TimeSlot

internal fun slotLabel(slot: TimeSlot?): String =
    slot?.let { Formats.timeRange(it.startTime, it.endTime) } ?: "Franja no disponible"
