package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.ServiceStatus

/** Outcome of validating a service-order status transition or closure mileage. */
sealed interface TransitionResult {
    data object Allowed : TransitionResult
    data class Denied(val reason: String) : TransitionResult
}

/**
 * Rule 3 (`docs/etapa2/REGLAS-Y-DECISIONES.md` §3.3 — "¿puede avanzar esta orden?"):
 * "`COMPLETED` es terminal" and "el odómetro no decrece", enforcing a strict linear flow
 * (`RECEIVED -> IN_PROGRESS -> COMPLETED`) and protecting maintenance calculations on closure.
 */
class ServiceTransitionRule {

    /** Exhaustive when on the source state: adding a state breaks compilation. */
    fun canTransition(from: ServiceStatus, to: ServiceStatus): TransitionResult =
        when (from) {
            ServiceStatus.RECEIVED -> allowOnly(from, to, ServiceStatus.IN_PROGRESS)
            ServiceStatus.IN_PROGRESS -> allowOnly(from, to, ServiceStatus.COMPLETED)
            ServiceStatus.COMPLETED -> TransitionResult.Denied(
                "La orden ya está finalizada y no admite más cambios."
            )
        }

    /** Closing requires a mileage and protects the odometer's increasing direction. */
    fun validateClosure(mileageAtService: Int?, lastKnownMileage: Int): TransitionResult = when {
        mileageAtService == null -> TransitionResult.Denied(
            "Debe registrar el kilometraje del vehículo para finalizar la orden."
        )
        mileageAtService < lastKnownMileage -> TransitionResult.Denied(
            "El kilometraje $mileageAtService es menor al último conocido ($lastKnownMileage km)."
        )
        else -> TransitionResult.Allowed
    }

    private fun allowOnly(
        from: ServiceStatus,
        to: ServiceStatus,
        expected: ServiceStatus,
    ): TransitionResult =
        if (to == expected) TransitionResult.Allowed
        else TransitionResult.Denied("No se puede pasar de ${from.label} a ${to.label}.")
}