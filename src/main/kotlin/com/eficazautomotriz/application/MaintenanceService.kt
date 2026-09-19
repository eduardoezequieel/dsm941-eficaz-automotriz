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

/** Una linea del reporte de mantenimiento: el tipo, su base y la evaluacion calculada. */
data class MaintenanceLine(
    val type: MaintenanceType,
    val lastServiceMileage: Int?,
    val evaluation: MaintenanceEvaluation,
)

/** Estado de mantenimiento de un vehiculo, con el detalle por tipo y el estado agregado. */
data class VehicleMaintenanceReport(
    val vehicle: Vehicle,
    val lines: List<MaintenanceLine>,
    val overallStatus: MaintenanceStatus,
)

/**
 * Modulo de procesamiento: calcula la clasificacion del mantenimiento preventivo.
 * Nada de lo que devuelve se almacena; se recalcula en cada consulta.
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
            ?: return fail("reportFor", DomainError.NotFound("vehiculo", vehicleId))

        return requireOwnershipOrStaff("reportFor", actor, vehicle.ownerId, "consultar el mantenimiento de $vehicleId")
            .flatMap { Outcome.success(buildReport(vehicle)) }
    }

    /** Reporte sin verificacion de permiso, para agregados internos y resumenes del sistema. */
    fun buildReport(vehicle: Vehicle): VehicleMaintenanceReport {
        val recordsByType = maintenanceRecords.findByVehicle(vehicle.id).associateBy { it.maintenanceTypeId }
        // Se listan todos los tipos activos: los que no tienen registro exhiben NO_PREVIOUS_RECORD.
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

    /** Registra o actualiza el kilometraje base de un tipo de mantenimiento del vehiculo. */
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

    /** Cuantos vehiculos hay en cada clasificacion, para el resumen del sistema. */
    fun statusCounts(): Map<MaintenanceStatus, Int> {
        val counts = vehicles.findAll()
            .groupingBy { buildReport(it).overallStatus }
            .eachCount()
        return MaintenanceStatus.entries.associateWith { counts[it] ?: 0 }
    }

    /**
     * El vehiculo hereda la clasificacion mas severa de sus tipos de mantenimiento.
     * Sin ninguna linea evaluable, no hay base sobre la cual proyectar nada.
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
