package com.eficazautomotriz.application

import com.eficazautomotriz.data.MaintenanceRecordRepository
import com.eficazautomotriz.data.MaintenanceTypeRepository
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.flatMap
import com.eficazautomotriz.domain.model.MaintenanceRecord
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.rules.MaintenanceEvaluation
import com.eficazautomotriz.domain.rules.MaintenanceStatusRule
import com.eficazautomotriz.logging.ErrorLogger

/** One line of the maintenance report: the type, its baseline, and the computed evaluation. */
data class MaintenanceLine(
    val type: MaintenanceType,
    val lastServiceMileage: Int?,
    val evaluation: MaintenanceEvaluation,
)

/** Maintenance state of a vehicle, with the per-type detail and the aggregate state. */
data class VehicleMaintenanceReport(
    val vehicle: Vehicle,
    val lines: List<MaintenanceLine>,
    val overallStatus: MaintenanceStatus,
)

/**
 * Processing module: it computes the preventive-maintenance classification.
 * Nothing it returns is stored; it is recomputed on every query.
 */
class MaintenanceService(
    private val vehicles: VehicleRepository,
    private val maintenanceRecords: MaintenanceRecordRepository,
    private val maintenanceTypes: MaintenanceTypeRepository,
    private val statusRule: MaintenanceStatusRule,
    private val config: SystemConfig,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    fun activeTypes(): List<MaintenanceType> = maintenanceTypes.findActive().sortedBy { it.name }

    fun reportFor(actor: User, vehicleId: String): Outcome<VehicleMaintenanceReport> {
        val vehicle = vehicles.findById(vehicleId)
            ?: return fail("reportFor", DomainError.NotFound("vehículo", vehicleId))

        return requireOwnershipOrStaff("reportFor", actor, vehicle.ownerId, "consultar el mantenimiento de $vehicleId")
            .flatMap { Outcome.success(buildReport(vehicle)) }
    }

    /** Report without a permission check, for internal aggregates and system summaries. */
    fun buildReport(vehicle: Vehicle): VehicleMaintenanceReport {
        val recordsByType = maintenanceRecords.findByVehicle(vehicle.id).associateBy { it.maintenanceTypeId }
        // Every active type is listed: those without a record show NO_PREVIOUS_RECORD.
        val lines = maintenanceTypes.findActive()
            .sortedBy { it.name }
            .map { type ->
                val record = recordsByType[type.id]
                MaintenanceLine(
                    type = type,
                    lastServiceMileage = record?.lastServiceMileage,
                    evaluation = statusRule.evaluate(
                        currentMileage = vehicle.currentMileage,
                        lastServiceMileage = record?.lastServiceMileage,
                        intervalKm = type.intervalKm,
                        warningThresholdKm = config.warningThresholdKm,
                    ),
                )
            }
        return VehicleMaintenanceReport(vehicle, lines, aggregate(lines))
    }

    /** Records or updates the baseline mileage of one maintenance type on the vehicle. */
    fun registerBaseline(vehicleId: String, maintenanceTypeId: String, mileage: Int): MaintenanceRecord {
        val existing = maintenanceRecords.findByVehicleAndType(vehicleId, maintenanceTypeId)
        val record = existing?.copy(lastServiceMileage = mileage)
            ?: MaintenanceRecord(
                id = maintenanceRecords.nextId(),
                vehicleId = vehicleId,
                maintenanceTypeId = maintenanceTypeId,
                lastServiceMileage = mileage,
            )
        return maintenanceRecords.save(record)
    }

    /** How many vehicles fall into each classification, for the system summary. */
    fun statusCounts(): Map<MaintenanceStatus, Int> {
        val counts = vehicles.findAll()
            .groupingBy { buildReport(it).overallStatus }
            .eachCount()
        return MaintenanceStatus.entries.associateWith { counts[it] ?: 0 }
    }

    /**
     * The vehicle inherits the most severe classification among its maintenance types.
     * With no evaluable line, there is no baseline from which to project anything.
     */
    private fun aggregate(lines: List<MaintenanceLine>): MaintenanceStatus {
        val statuses = lines.map { it.evaluation.status }
        return when {
            statuses.isEmpty() -> MaintenanceStatus.NO_PREVIOUS_RECORD
            statuses.contains(MaintenanceStatus.OVERDUE) -> MaintenanceStatus.OVERDUE
            statuses.contains(MaintenanceStatus.DUE_SOON) -> MaintenanceStatus.DUE_SOON
            statuses.contains(MaintenanceStatus.UP_TO_DATE) -> MaintenanceStatus.UP_TO_DATE
            else -> MaintenanceStatus.NO_PREVIOUS_RECORD
        }
    }
}
