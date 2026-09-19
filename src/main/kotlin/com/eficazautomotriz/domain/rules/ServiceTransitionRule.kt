package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.ServiceStatus

sealed interface TransitionResult {
    data object Allowed : TransitionResult
    data class Denied(val reason: String) : TransitionResult
}

/**
 * Regla 3: gobierna el avance de una orden de servicio.
 *
 * El ciclo es lineal: Recibido -> En proceso -> Finalizado. Sin retrocesos y sin
 * transiciones al mismo estado. Finalizado es terminal.
 */
class ServiceTransitionRule {

    /** when exhaustivo sobre el estado de origen: agregar un estado rompe la compilacion. */
    fun canTransition(from: ServiceStatus, to: ServiceStatus): TransitionResult =
        when (from) {
            ServiceStatus.RECEIVED -> allowOnly(from, to, ServiceStatus.IN_PROGRESS)
            ServiceStatus.IN_PROGRESS -> allowOnly(from, to, ServiceStatus.COMPLETED)
            ServiceStatus.COMPLETED -> TransitionResult.Denied(
                "La orden ya esta finalizada y no admite mas cambios."
            )
        }

    /** Al cerrar se exige kilometraje y se protege el sentido creciente del odometro. */
    fun validateClosure(mileageAtService: Int?, lastKnownMileage: Int): TransitionResult = when {
        mileageAtService == null -> TransitionResult.Denied(
            "Debe registrar el kilometraje del vehiculo para finalizar la orden."
        )
        mileageAtService < lastKnownMileage -> TransitionResult.Denied(
            "El kilometraje $mileageAtService es menor al ultimo conocido ($lastKnownMileage km)."
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
