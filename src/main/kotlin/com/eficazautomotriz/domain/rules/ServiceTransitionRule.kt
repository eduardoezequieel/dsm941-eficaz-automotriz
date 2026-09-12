package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.ServiceStatus

sealed interface TransitionResult {
    object Allowed : TransitionResult

    class Denied(
        val message: String
    ) : TransitionResult
}

class ServiceTransitionRule {
    fun canTransition(
        currentStatus: ServiceStatus,
        nextStatus: ServiceStatus
    ): TransitionResult {
        if (currentStatus == nextStatus) {
            return TransitionResult.Denied("El servicio ya se encuentra en ese estado.")
        }

        if (currentStatus == ServiceStatus.COMPLETED) {
            return TransitionResult.Denied("No se pueden realizar cambios despues de completar el servicio.")
        }

        val allowed = when (currentStatus) {
            ServiceStatus.RECEIVED -> nextStatus == ServiceStatus.IN_PROGRESS
            ServiceStatus.IN_PROGRESS -> nextStatus == ServiceStatus.COMPLETED
            ServiceStatus.COMPLETED -> false
        }

        return if (allowed) {
            TransitionResult.Allowed
        } else {
            TransitionResult.Denied("La transicion solicitada no sigue el flujo permitido del servicio.")
        }
    }

    fun validateClosure(
        closingMileage: Int?,
        previousKnownMileage: Int?
    ): TransitionResult {
        if (closingMileage == null) {
            return TransitionResult.Denied("Debe registrar el kilometraje para finalizar el servicio.")
        }

        if (previousKnownMileage != null && closingMileage < previousKnownMileage) {
            return TransitionResult.Denied(
                "El kilometraje de cierre no puede ser menor al kilometraje registrado anteriormente."
            )
        }

        return TransitionResult.Allowed
    }
}
