package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.ServiceStatus

sealed interface TransitionResult {
    data object Allowed : TransitionResult
    data class Denied(val reason: String) : TransitionResult
}

/**
 * Rule 3: governs how a service order advances.
 *
 * The cycle is linear: Received -> In progress -> Completed. No steps backward and no
 * transitions into the same state. Completed is terminal.
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
