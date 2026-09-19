package com.eficazautomotriz.application

import com.eficazautomotriz.data.MaintenanceRecordRepository
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.flatMap
import com.eficazautomotriz.domain.validation.TextFields
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.UserRole
import com.eficazautomotriz.logging.ErrorLogger
import java.time.LocalDate

/** Gestion completa del vehiculo: alta, consulta, edicion, baja y kilometraje. */
class VehicleService(
    private val vehicles: VehicleRepository,
    private val maintenanceRecords: MaintenanceRecordRepository,
    private val config: SystemConfig,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    /** El cliente ve los suyos; el personal ve todos. */
    fun listVisibleTo(actor: User): List<Vehicle> = when (actor.role) {
        UserRole.CLIENT -> vehicles.findByOwner(actor.id)
        UserRole.STAFF -> vehicles.findAll()
    }.sortedBy { it.plate }

    fun findAccessible(actor: User, vehicleId: String): Outcome<Vehicle> {
        val vehicle = vehicles.findById(vehicleId)
            ?: return fail("findAccessible", DomainError.NotFound("vehiculo", vehicleId))
        return requireOwnershipOrStaff("findAccessible", actor, vehicle.ownerId, "consultar el vehiculo $vehicleId")
            .flatMap { Outcome.success(vehicle) }
    }

    fun register(
        actor: User,
        ownerId: String,
        plate: String,
        make: String,
        model: String,
        year: Int,
        currentMileage: Int,
        today: LocalDate,
    ): Outcome<Vehicle> {
        val permission = requireOwnershipOrStaff("register", actor, ownerId, "registrar un vehiculo a nombre de $ownerId")
        if (permission is Outcome.Failure) return permission

        validateVehicleFields(plate, make, model, year, currentMileage, today)
            ?.let { return failInvalid("register", it, "plate=$plate") }

        if (vehicles.findByPlate(plate) != null) {
            return failInvalid("register", ValidationError.DuplicatePlate(plate), "ownerId=$ownerId plate=$plate")
        }

        val vehicle = Vehicle(
            id = vehicles.nextId(),
            ownerId = ownerId,
            plate = plate.uppercase(),
            make = make,
            model = model,
            year = year,
            currentMileage = currentMileage,
            mileageUpdatedAt = today,
        )
        return Outcome.success(vehicles.save(vehicle))
    }

    fun update(
        actor: User,
        vehicleId: String,
        make: String,
        model: String,
        year: Int,
        today: LocalDate,
    ): Outcome<Vehicle> = findAccessible(actor, vehicleId).flatMap { vehicle ->
        validateDetails(make, model, year, today)
            ?.let { return@flatMap failInvalid("update", it, "vehicleId=$vehicleId") }

        Outcome.success(vehicles.save(vehicle.copy(make = make, model = model, year = year)))
    }

    /** Al eliminar el vehiculo se descartan sus registros de mantenimiento asociados. */
    fun delete(actor: User, vehicleId: String): Outcome<Vehicle> =
        findAccessible(actor, vehicleId).flatMap { vehicle ->
            maintenanceRecords.findByVehicle(vehicleId).forEach { maintenanceRecords.deleteById(it.id) }
            vehicles.deleteById(vehicleId)
            Outcome.success(vehicle)
        }

    /** El odometro no decrece: es lo que protege el calculo de mantenimiento. */
    fun updateMileage(
        actor: User,
        vehicleId: String,
        newMileage: Int,
        today: LocalDate,
    ): Outcome<Vehicle> = findAccessible(actor, vehicleId).flatMap { vehicle ->
        ValidationError.validateMileage(newMileage, config.maxMileage)
            ?.let { return@flatMap failInvalid("updateMileage", it, "vehicleId=$vehicleId") }

        if (newMileage < vehicle.currentMileage) {
            return@flatMap fail(
                "updateMileage",
                DomainError.MileageDecrease(newMileage, vehicle.currentMileage),
                "vehicleId=$vehicleId",
            )
        }

        Outcome.success(
            vehicles.save(vehicle.copy(currentMileage = newMileage, mileageUpdatedAt = today))
        )
    }

    private fun validateVehicleFields(
        plate: String,
        make: String,
        model: String,
        year: Int,
        currentMileage: Int,
        today: LocalDate,
    ): ValidationError? = ValidationError.validatePlate(plate)
        ?: validateDetails(make, model, year, today)
        ?: ValidationError.validateMileage(currentMileage, config.maxMileage)

    /** Los campos que alta y edicion comparten; el ano entrante se admite. */
    private fun validateDetails(
        make: String,
        model: String,
        year: Int,
        today: LocalDate,
    ): ValidationError? = ValidationError.validateText(TextFields.MAKE, make)
        ?: ValidationError.validateText(TextFields.MODEL, model)
        ?: ValidationError.validateYear(year, config.minVehicleYear, today.year + 1)
}