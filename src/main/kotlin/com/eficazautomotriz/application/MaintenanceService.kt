package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.MaintenanceStatus
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.SystemConfig
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.repository.MaintenanceRecordRepository
import com.eficazautomotriz.domain.repository.MaintenanceTypeRepository
import com.eficazautomotriz.domain.repository.VehicleRepository
import com.eficazautomotriz.domain.rules.MaintenanceEvaluation
import com.eficazautomotriz.domain.rules.MaintenanceStatusRule

data class MaintenanceLine(
    val maintenanceType: MaintenanceType,
    val lastServiceMileage: Int?,
    val evaluation: MaintenanceEvaluation
)

data class VehicleMaintenanceReport(
    val vehicle: Vehicle,
    val lines: List<MaintenanceLine>,
    val overallStatus: MaintenanceStatus
)

class MaintenanceService(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRecordRepository: MaintenanceRecordRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val systemConfig: SystemConfig,
    private val maintenanceStatusRule: MaintenanceStatusRule = MaintenanceStatusRule()
) {
    fun getActiveMaintenanceTypes(): Outcome<List<MaintenanceType>> {
        return Outcome.Success(maintenanceTypeRepository.findActive())
    }

    fun generateVehicleReport(
        vehicleId: String,
        canViewMaintenance: Boolean = true
    ): Outcome<VehicleMaintenanceReport> {
        if (!canViewMaintenance) {
            return Outcome.Failure(DomainError.UnauthorizedAccess)
        }

        val vehicle = vehicleRepository.findById(vehicleId)
            ?: return Outcome.Failure(DomainError.EntityNotFound("vehiculo", vehicleId))

        return Outcome.Success(buildReportWithoutPermissionCheck(vehicle))
    }

    fun buildReportWithoutPermissionCheck(vehicle: Vehicle): VehicleMaintenanceReport {
        val lines = maintenanceTypeRepository.findActive().map { maintenanceType ->
            val lastMileage = maintenanceRecordRepository.findLastMileage(
                vehicleId = vehicle.id,
                maintenanceTypeId = maintenanceType.id
            )
            val evaluation = maintenanceStatusRule.evaluate(
                currentMileage = vehicle.currentMileage,
                lastMaintenanceMileage = lastMileage,
                maintenanceInterval = maintenanceType.intervalMileage,
                warningThreshold = systemConfig.maintenanceWarningThreshold
            )

            MaintenanceLine(
                maintenanceType = maintenanceType,
                lastServiceMileage = lastMileage,
                evaluation = evaluation
            )
        }

        return VehicleMaintenanceReport(
            vehicle = vehicle,
            lines = lines,
            overallStatus = mostSevereStatus(lines)
        )
    }

    fun saveBaseMaintenanceMileage(
        vehicleId: String,
        maintenanceTypeId: String,
        mileage: Int
    ): Outcome<Unit> {
        if (vehicleRepository.findById(vehicleId) == null) {
            return Outcome.Failure(DomainError.EntityNotFound("vehiculo", vehicleId))
        }

        if (maintenanceTypeRepository.findById(maintenanceTypeId) == null) {
            return Outcome.Failure(DomainError.EntityNotFound("tipo de mantenimiento", maintenanceTypeId))
        }

        maintenanceRecordRepository.saveBaseMileage(
            vehicleId = vehicleId,
            maintenanceTypeId = maintenanceTypeId,
            mileage = mileage
        )

        return Outcome.Success(Unit)
    }

    fun countVehiclesByMaintenanceStatus(): Outcome<Map<MaintenanceStatus, Int>> {
        val reports = vehicleRepository.findAll().map { vehicle ->
            buildReportWithoutPermissionCheck(vehicle)
        }

        return Outcome.Success(
            reports.groupingBy { report -> report.overallStatus }.eachCount()
        )
    }

    private fun mostSevereStatus(lines: List<MaintenanceLine>): MaintenanceStatus {
        val statuses = lines.map { line -> line.evaluation.status }

        return when {
            MaintenanceStatus.OVERDUE in statuses -> MaintenanceStatus.OVERDUE
            MaintenanceStatus.DUE_SOON in statuses -> MaintenanceStatus.DUE_SOON
            MaintenanceStatus.UP_TO_DATE in statuses -> MaintenanceStatus.UP_TO_DATE
            else -> MaintenanceStatus.NO_PREVIOUS_RECORD
        }
    }
}
