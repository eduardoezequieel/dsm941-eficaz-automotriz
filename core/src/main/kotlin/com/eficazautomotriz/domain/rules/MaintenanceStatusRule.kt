package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

/** Computed preventive-maintenance classification and remaining distance for one maintenance type. */
data class MaintenanceEvaluation(
    val status: MaintenanceStatus,
    // null when there is no previous record: there is no baseline from which to project.
    val recommendedMileage: Int?,
    // recommendedMileage - currentMileage; negative once the interval has already been exceeded.
    val differenceKm: Int?,
)

/**
 * Rule 2 (`docs/etapa2/REGLAS-Y-DECISIONES.md` §3.2 — "¿cómo está el mantenimiento preventivo?"):
 * "todo lo vencido cae también dentro de la ventana de aviso", so OVERDUE is checked before
 * DUE_SOON; results are always recomputed and never persisted so they cannot drift from mileage.
 */
class MaintenanceStatusRule {

    /**
     * Evaluates in this exact order: no previous record, then overdue, then due soon,
     * and in every other case up to date.
     */
    fun evaluate(
        currentMileage: Int,
        lastServiceMileage: Int?,
        intervalKm: Int,
        warningThresholdKm: Int,
    ): MaintenanceEvaluation {
        // A non-positive interval is a configuration error, not a business case.
        require(intervalKm > 0) { "El intervalo de mantenimiento debe ser mayor que cero: $intervalKm" }

        if (lastServiceMileage == null) {
            return MaintenanceEvaluation(
                status = MaintenanceStatus.NO_PREVIOUS_RECORD,
                recommendedMileage = null,
                differenceKm = null,
            )
        }

        val recommendedMileage = lastServiceMileage + intervalKm
        val status = when {
            currentMileage >= recommendedMileage -> MaintenanceStatus.OVERDUE
            currentMileage >= recommendedMileage - warningThresholdKm -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.UP_TO_DATE
        }
        return MaintenanceEvaluation(
            status = status,
            recommendedMileage = recommendedMileage,
            differenceKm = recommendedMileage - currentMileage,
        )
    }
}