package com.eficazautomotriz.domain.error

import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.rules.UnavailabilityReason

/**
 * Expected business errors. They are returned inside an Outcome and never thrown:
 * a full slot is not a program failure, it is a foreseen outcome.
 */
sealed class DomainError(val message: String) {

    data class SlotUnavailable(val reason: UnavailabilityReason) :
        DomainError("Franja no disponible: ${reason.label}")

    data class InvalidTransition(val from: ServiceStatus, val to: ServiceStatus) :
        DomainError("Transicion no permitida: de ${from.label} a ${to.label}")

    data class MileageDecrease(val attempted: Int, val lastKnown: Int) :
        DomainError("El kilometraje no puede decrecer: $attempted es menor que $lastKnown km")

    data class NotFound(val entity: String, val id: String) :
        DomainError("No se encontro $entity con identificador $id")

    data class Forbidden(val action: String) :
        DomainError("No tiene permiso para: $action")

    data class MissingBaseline(val vehicleId: String) :
        DomainError("El vehiculo $vehicleId no tiene kilometraje base de mantenimiento registrado")

    data class Invalid(val validation: ValidationError) :
        DomainError(validation.message)
}
