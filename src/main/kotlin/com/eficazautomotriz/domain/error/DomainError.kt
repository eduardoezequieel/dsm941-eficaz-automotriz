package com.eficazautomotriz.domain.error

import com.eficazautomotriz.domain.rules.UnavailabilityReason

private fun appointmentUnavailableMessage(reason: UnavailabilityReason): String {
    return when (reason) {
        UnavailabilityReason.SLOT_DISABLED -> "La franja seleccionada esta deshabilitada."
        UnavailabilityReason.DATE_IN_PAST -> "No se pueden agendar citas en fechas pasadas."
        UnavailabilityReason.DAY_MISMATCH -> "La fecha no coincide con el dia configurado para la franja."
        UnavailabilityReason.CAPACITY_REACHED -> "La franja seleccionada ya alcanzo su capacidad maxima."
    }
}

sealed class DomainError(
    val message: String
) {
    data class EntityNotFound(
        val entity: String,
        val id: String? = null
    ) : DomainError(
        message = if (id.isNullOrBlank()) {
            "No se encontro $entity."
        } else {
            "No se encontro $entity con identificador $id."
        }
    )

    object UnauthorizedAccess : DomainError(
        message = "No tienes autorizacion para realizar esta operacion."
    )

    data class OperationNotAllowed(
        val reason: String = "La operacion solicitada no esta permitida."
    ) : DomainError(
        message = reason
    )

    data class AppointmentUnavailable(
        val reason: UnavailabilityReason
    ) : DomainError(
        message = appointmentUnavailableMessage(reason)
    )

    data class InvalidStateTransition(
        val reason: String = "La transicion de estado no es valida."
    ) : DomainError(
        message = reason
    )

    data class Validation(
        val error: ValidationError
    ) : DomainError(
        message = error.message
    )
}
