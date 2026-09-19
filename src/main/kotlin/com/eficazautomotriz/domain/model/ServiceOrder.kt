package com.eficazautomotriz.domain.model

import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDateTime

data class ServiceOrder(
    override val id: String,
    val vehicleId: String,
    // Nulable: un vehiculo puede ingresar al taller sin cita previa.
    val appointmentId: String?,
    val serviceTypeId: String,
    // Se llena unicamente al finalizar la orden.
    val mileageAtService: Int?,
    val status: ServiceStatus,
    val notes: String,
    val receivedAt: LocalDateTime,
    val closedAt: LocalDateTime?,
) : Identifiable