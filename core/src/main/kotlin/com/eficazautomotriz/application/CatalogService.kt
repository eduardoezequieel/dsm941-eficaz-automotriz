package com.eficazautomotriz.application

import com.eficazautomotriz.data.MaintenanceTypeRepository
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.flatMap
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.validation.TextFields
import com.eficazautomotriz.logging.ErrorLogger

/**
 * Staff configuration of the service catalog and the preventive-maintenance intervals.
 * Reads are open (the booking and closing screens already filter to active rows).
 * Every write goes through [requireStaff] and returns a validation error instead of throwing.
 */
class CatalogService(
    private val serviceTypeRepository: ServiceTypeRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val config: SystemConfig,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    fun serviceTypes(): List<ServiceType> = serviceTypeRepository.findAll().sortedBy { it.name }

    fun maintenanceTypes(): List<MaintenanceType> = maintenanceTypeRepository.findAll().sortedBy { it.name }

    /**
     * Creates a service type when [id] is null (`serviceTypes.nextId()`), or replaces the
     * named one. [description] may be blank. The name is required, at most 60 characters.
     */
    fun saveServiceType(
        actor: User,
        id: String?,
        name: String,
        description: String,
        active: Boolean,
    ): Outcome<ServiceType> =
        requireStaff("saveServiceType", actor, "configurar el catalogo de servicios").flatMap {
            val current = if (id == null) {
                null
            } else {
                serviceTypeRepository.findById(id)
                    ?: return@flatMap fail("saveServiceType", DomainError.NotFound("tipo de servicio", id))
            }
            val trimmedName = name.trim()
            val trimmedDescription = description.trim()
            ValidationError.validateText(TextFields.CATALOG_NAME, trimmedName)
                ?.let { return@flatMap failInvalid("saveServiceType", it, "id=${id ?: "new"}") }
            ValidationError.validateOptionalText(TextFields.CATALOG_DESCRIPTION, trimmedDescription)
                ?.let { return@flatMap failInvalid("saveServiceType", it, "id=${id ?: "new"}") }
            Outcome.success(
                serviceTypeRepository.save(
                    ServiceType(
                        id = current?.id ?: serviceTypeRepository.nextId(),
                        name = trimmedName,
                        description = trimmedDescription,
                        active = active,
                    ),
                ),
            )
        }

    fun setServiceTypeActive(actor: User, id: String, active: Boolean): Outcome<ServiceType> =
        requireStaff("setServiceTypeActive", actor, "activar o desactivar tipos de servicio").flatMap {
            val current = serviceTypeRepository.findById(id)
                ?: return@flatMap fail("setServiceTypeActive", DomainError.NotFound("tipo de servicio", id))
            Outcome.success(serviceTypeRepository.save(current.copy(active = active)))
        }

    /**
     * Creates or updates a maintenance interval. [intervalKm] must be between 1 and
     * [SystemConfig.maxMileage]; anything else is [DomainError.Invalid], never an exception.
     */
    fun saveMaintenanceType(
        actor: User,
        id: String?,
        name: String,
        intervalKm: Int,
        active: Boolean,
    ): Outcome<MaintenanceType> =
        requireStaff("saveMaintenanceType", actor, "configurar los intervalos de mantenimiento").flatMap {
            val current = if (id == null) {
                null
            } else {
                maintenanceTypeRepository.findById(id)
                    ?: return@flatMap fail("saveMaintenanceType", DomainError.NotFound("tipo de mantenimiento", id))
            }
            val trimmedName = name.trim()
            ValidationError.validateText(TextFields.CATALOG_NAME, trimmedName)
                ?.let { return@flatMap failInvalid("saveMaintenanceType", it, "id=${id ?: "new"} intervalKm=$intervalKm") }
            ValidationError.validateIntervalKm(intervalKm, config.maxMileage)
                ?.let { return@flatMap failInvalid("saveMaintenanceType", it, "id=${id ?: "new"} intervalKm=$intervalKm") }
            Outcome.success(
                maintenanceTypeRepository.save(
                    MaintenanceType(
                        id = current?.id ?: maintenanceTypeRepository.nextId(),
                        name = trimmedName,
                        intervalKm = intervalKm,
                        active = active,
                    ),
                ),
            )
        }

    fun setMaintenanceTypeActive(actor: User, id: String, active: Boolean): Outcome<MaintenanceType> =
        requireStaff("setMaintenanceTypeActive", actor, "activar o desactivar tipos de mantenimiento").flatMap {
            val current = maintenanceTypeRepository.findById(id)
                ?: return@flatMap fail("setMaintenanceTypeActive", DomainError.NotFound("tipo de mantenimiento", id))
            Outcome.success(maintenanceTypeRepository.save(current.copy(active = active)))
        }
}
