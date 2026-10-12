package com.eficazautomotriz.domain.model

import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDateTime

data class ServiceOrder(
    override val id: String,
    val vehicleId: String,
    // Nullable: a vehicle may enter the shop with no prior appointment.
    val appointmentId: String?,
    val serviceTypeId: String,
    // Filled in only when the order is completed.
    val mileageAtService: Int?,
    val status: ServiceStatus,
    val notes: String,
    val receivedAt: LocalDateTime,
    // Set when the order moves to IN_PROGRESS. Null while it is still RECEIVED.
    val startedAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
) : Identifiable
