package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.MaintenanceStatus

data class MaintenanceEvaluation(
    val status: MaintenanceStatus,
    val recommendedMileage: Int?,
    val mileageDifference: Int?
)

class MaintenanceStatusRule {
    fun evaluate(
        currentMileage: Int,
        lastMaintenanceMileage: Int?,
        maintenanceInterval: Int,
        warningThreshold: Int = 500
    ): MaintenanceEvaluation {
        require(maintenanceInterval > 0) {
            "El intervalo de mantenimiento debe ser mayor que cero."
        }

        if (lastMaintenanceMileage == null) {
            return MaintenanceEvaluation(
                status = MaintenanceStatus.NO_PREVIOUS_RECORD,
                recommendedMileage = null,
                mileageDifference = null
            )
        }

        val recommendedMileage = lastMaintenanceMileage + maintenanceInterval
        val mileageDifference = recommendedMileage - currentMileage
        val status = when {
            currentMileage >= recommendedMileage -> MaintenanceStatus.OVERDUE
            mileageDifference <= warningThreshold -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.UP_TO_DATE
        }

        return MaintenanceEvaluation(
            status = status,
            recommendedMileage = recommendedMileage,
            mileageDifference = mileageDifference
        )
    }
}
