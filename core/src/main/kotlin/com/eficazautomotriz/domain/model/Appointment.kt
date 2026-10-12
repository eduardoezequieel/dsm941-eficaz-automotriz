package com.eficazautomotriz.domain.model

import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class Appointment(
    override val id: String,
    val clientId: String,
    val vehicleId: String,
    val serviceTypeId: String,
    val date: LocalDate,
    val slotId: String,
    val status: AppointmentStatus,
    // Present only when the appointment was rejected or rescheduled.
    val reason: String?,
    val requestedAt: LocalDateTime,
) : Identifiable
