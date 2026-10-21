package com.eficazautomotriz.application

import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.data.EvidenceRepository
import com.eficazautomotriz.data.ServiceOrderRepository
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.flatMap
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.rules.ServiceTransitionRule
import com.eficazautomotriz.domain.rules.TransitionResult
import com.eficazautomotriz.domain.validation.TextFields
import com.eficazautomotriz.logging.ErrorLogger
import java.time.LocalDate
import java.time.LocalDateTime

/** A service order together with the evidence recorded against it. */
data class ServiceOrderDetail(
    val order: ServiceOrder,
    val evidences: List<Evidence>,
)

/** Receiving, advancing, evidence, and closing of service orders. */
class ServiceOrderService(
    private val serviceOrders: ServiceOrderRepository,
    private val evidences: EvidenceRepository,
    private val vehicles: VehicleRepository,
    private val appointments: AppointmentRepository,
    private val serviceTypes: ServiceTypeRepository,
    private val maintenanceService: MaintenanceService,
    private val vehicleService: VehicleService,
    private val transitionRule: ServiceTransitionRule,
    private val config: SystemConfig,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    fun serviceTypeName(serviceTypeId: String): String = serviceTypes.nameOf(serviceTypeId)

    fun listAll(actor: User): Outcome<List<ServiceOrder>> =
        requireStaff("listAll", actor, "consultar todas las ordenes de servicio")
            .flatMap { Outcome.success(serviceOrders.findAll().sortedByDescending { it.receivedAt }) }

    /** History of a vehicle: the client sees their own, staff see anyone's. */
    fun historyFor(actor: User, vehicleId: String): Outcome<List<ServiceOrderDetail>> =
        vehicleService.findAccessible(actor, vehicleId).flatMap {
            val details = serviceOrders.findByVehicle(vehicleId)
                .sortedByDescending { it.receivedAt }
                .map { ServiceOrderDetail(it, evidences.findByServiceOrder(it.id)) }
            Outcome.success(details)
        }

    fun detailOf(actor: User, orderId: String): Outcome<ServiceOrderDetail> {
        val order = serviceOrders.findById(orderId)
            ?: return fail("detailOf", DomainError.NotFound("orden de servicio", orderId))
        return vehicleService.findAccessible(actor, order.vehicleId)
            .flatMap { Outcome.success(ServiceOrderDetail(order, evidences.findByServiceOrder(orderId))) }
    }

    /**
     * Receives a vehicle. The linked appointment is NOT marked here: it keeps its place
     * until the order is closed. See `complete`.
     */
    fun create(
        actor: User,
        vehicleId: String,
        serviceTypeId: String,
        appointmentId: String?,
        notes: String,
        now: LocalDateTime,
    ): Outcome<ServiceOrder> = requireStaff("create", actor, "crear ordenes de servicio").flatMap {
        if (vehicles.findById(vehicleId) == null) {
            return@flatMap fail("create", DomainError.NotFound("vehiculo", vehicleId))
        }
        if (serviceTypes.findById(serviceTypeId) == null) {
            return@flatMap fail("create", DomainError.NotFound("tipo de servicio", serviceTypeId))
        }
        if (appointmentId != null && appointments.findById(appointmentId) == null) {
            return@flatMap fail("create", DomainError.NotFound("cita", appointmentId))
        }
        val trimmedNotes = notes.trim()
        ValidationError.validateOptionalText(TextFields.NOTES, trimmedNotes)
            ?.let { return@flatMap failInvalid("create", it, "vehicleId=$vehicleId") }

        val order = serviceOrders.save(
            ServiceOrder(
                id = serviceOrders.nextId(),
                vehicleId = vehicleId,
                appointmentId = appointmentId,
                serviceTypeId = serviceTypeId,
                mileageAtService = null,
                status = ServiceStatus.RECEIVED,
                notes = trimmedNotes,
                receivedAt = now,
                startedAt = null,
                closedAt = null,
            )
        )
        Outcome.success(order)
    }

    /** Advances the status without closing. Closing has its own use case. */
    fun advance(actor: User, orderId: String, to: ServiceStatus, now: LocalDateTime): Outcome<ServiceOrder> =
        requireStaff("advance", actor, "avanzar el estado de una orden").flatMap {
            val order = serviceOrders.findById(orderId)
                ?: return@flatMap fail("advance", DomainError.NotFound("orden de servicio", orderId))

            if (to == ServiceStatus.COMPLETED) {
                return@flatMap fail(
                    "advance",
                    DomainError.Forbidden("finalizar una orden sin registrar el kilometraje"),
                    "orderId=$orderId",
                )
            }

            when (val transition = transitionRule.canTransition(order.status, to)) {
                is TransitionResult.Denied -> fail(
                    "advance",
                    DomainError.InvalidTransition(order.status, to),
                    "orderId=$orderId reason=${transition.reason}",
                )
                TransitionResult.Allowed -> {
                    val startedAt = if (to == ServiceStatus.IN_PROGRESS) now else order.startedAt
                    Outcome.success(serviceOrders.save(order.copy(status = to, startedAt = startedAt)))
                }
            }
        }

    /**
     * Closes the order: it validates the transition and the mileage, updates the vehicle
     * odometer, restarts the cycle of the maintenance that was performed, and marks the
     * appointment it came from as attended.
     */
    fun complete(
        actor: User,
        orderId: String,
        mileageAtService: Int,
        performedMaintenanceTypeIds: List<String>,
        today: LocalDate,
        now: LocalDateTime,
    ): Outcome<ServiceOrder> = requireStaff("complete", actor, "finalizar ordenes de servicio").flatMap {
        val order = serviceOrders.findById(orderId)
            ?: return@flatMap fail("complete", DomainError.NotFound("orden de servicio", orderId))
        val vehicle = vehicles.findById(order.vehicleId)
            ?: return@flatMap fail("complete", DomainError.NotFound("vehiculo", order.vehicleId))

        when (val transition = transitionRule.canTransition(order.status, ServiceStatus.COMPLETED)) {
            is TransitionResult.Denied -> return@flatMap fail(
                "complete",
                DomainError.InvalidTransition(order.status, ServiceStatus.COMPLETED),
                "orderId=$orderId reason=${transition.reason}",
            )
            TransitionResult.Allowed -> Unit
        }

        ValidationError.validateMileage(mileageAtService, config.maxMileage)
            ?.let { return@flatMap failInvalid("complete", it, "orderId=$orderId mileage=$mileageAtService") }

        when (transitionRule.validateClosure(mileageAtService, vehicle.currentMileage)) {
            is TransitionResult.Denied -> return@flatMap fail(
                "complete",
                DomainError.MileageDecrease(mileageAtService, vehicle.currentMileage),
                "orderId=$orderId vehicleId=${vehicle.id}",
            )
            TransitionResult.Allowed -> Unit
        }

        val closed = serviceOrders.save(
            order.copy(
                status = ServiceStatus.COMPLETED,
                mileageAtService = mileageAtService,
                closedAt = now,
            )
        )
        vehicles.save(vehicle.copy(currentMileage = mileageAtService, mileageUpdatedAt = today))
        performedMaintenanceTypeIds.forEach { typeId ->
            maintenanceService.registerBaseline(vehicle.id, typeId, mileageAtService)
        }
        order.appointmentId?.let { markAppointmentAttended("complete", actor, it) }
        Outcome.success(closed)
    }

    fun addEvidence(
        actor: User,
        orderId: String,
        stage: EvidenceStage,
        imageUrl: String,
        now: LocalDateTime,
    ): Outcome<Evidence> = requireStaff("addEvidence", actor, "registrar evidencias").flatMap {
        val order = serviceOrders.findById(orderId)
            ?: return@flatMap fail("addEvidence", DomainError.NotFound("orden de servicio", orderId))

        if (order.status == ServiceStatus.COMPLETED) {
            return@flatMap fail(
                "addEvidence",
                DomainError.Forbidden("agregar evidencias a una orden finalizada"),
                "orderId=$orderId",
            )
        }
        ValidationError.validateImageUrl(imageUrl)
            ?.let { return@flatMap failInvalid("addEvidence", it, "orderId=$orderId") }

        val evidence = evidences.save(
            Evidence(
                id = evidences.nextId(),
                serviceOrderId = orderId,
                stage = stage,
                imageUrl = imageUrl,
                uploadedAt = now,
            )
        )
        Outcome.success(evidence)
    }

    /** Failing to mark the appointment does not undo the order already closed: it only stays in the log. */
    private fun markAppointmentAttended(operation: String, actor: User, appointmentId: String) {
        val appointment = appointments.findById(appointmentId) ?: return
        if (appointment.status.isFinal) {
            logger.warn(
                "ServiceOrderService.$operation",
                "AppointmentAlreadyFinal(${appointment.status})",
                "appointmentId=$appointmentId actorId=${actor.id}",
            )
            return
        }
        appointments.save(appointment.copy(status = AppointmentStatus.ATTENDED))
    }
}
