package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

data class MaintenanceEvaluation(
    val status: MaintenanceStatus,
    // null cuando no hay registro previo: no hay base sobre la cual proyectar.
    val recommendedMileage: Int?,
    // recommendedMileage - currentMileage; negativo cuando el intervalo ya se excedio.
    val differenceKm: Int?,
)

/**
 * Regla 2: clasifica el mantenimiento preventivo de un vehiculo.
 *
 * El resultado es siempre calculado y nunca se persiste: almacenar un valor derivado
 * abre la puerta a que quede desincronizado del kilometraje real.
 */
class MaintenanceStatusRule {

    /**
     * Evalua en este orden exacto: sin registro previo, luego vencido, luego proximo,
     * y en cualquier otro caso al dia.
     */
    fun evaluate(
        currentMileage: Int,
        lastServiceMileage: Int?,
        intervalKm: Int,
        warningThresholdKm: Int,
    ): MaintenanceEvaluation {
        // Un intervalo no positivo es un error de configuracion, no un caso de negocio.
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
