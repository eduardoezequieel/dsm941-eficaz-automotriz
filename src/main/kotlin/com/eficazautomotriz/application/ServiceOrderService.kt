package com.eficazautomotriz.application

import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.data.EvidenceRepository
import com.eficazautomotriz.data.ServiceOrderRepository
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.data.VehicleRepository
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

/** Orden de servicio junto con las evidencias que se le registraron. */
data class ServiceOrderDetail(
    val order: ServiceOrder,
    val evidences: List<Evidence>,
)

/** Recepcion, avance, evidencias y cierre de las ordenes de servicio. */
class ServiceOrderService(
    private val serviceOrders: ServiceOrderRepository,
    private val evidences: EvidenceRepository,
    private val vehicles: VehicleRepository,
    private val appointments: AppointmentRepository,
    private val serviceTypes: ServiceTypeRepository,
    private val maintenanceService: MaintenanceService,
    private val vehicleService: VehicleService,
    private val transitionRule: ServiceTransitionRule,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    fun serviceTypeName(serviceTypeId: String): String = serviceTypes.nameOf(serviceTypeId)

    fun listAll(actor: User): Outcome<List<ServiceOrder>> =
        requireStaff("listAll", actor, "consultar todas las ordenes de servicio")
            .flatMap { Outcome.success(serviceOrders.findAll().sortedByDescending { it.receivedAt }) }

    /** Historial de un vehiculo: el cliente ve el suyo, el personal ve el de cualquiera. */
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
     * Recibe un vehiculo. La cita asociada NO se marca aqui: sigue ocupando su cupo
     * hasta que la orden se cierre. Ver `complete`.
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
        ValidationError.validateOptionalText(TextFields.NOTES, notes)
            ?.let { return@flatMap failInvalid("create", it, "vehicleId=$vehicleId") }

        val order = serviceOrders.save(
            ServiceOrder(
                id = serviceOrders.nextId(),
                vehicleId = vehicleId,
                appointmentId = appointmentId,
                serviceTypeId = serviceTypeId,
                mileageAtService = null,
                status = ServiceStatus.RECEIVED,
                notes = notes,
                receivedAt = now,
                closedAt = null,
            )
        )
        Outcome.success(order)
    }

    /** Avanza el estado sin cerrar. El cierre tiene su propio caso de uso. */
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
                TransitionResult.Allowed -> Outcome.success(serviceOrders.save(order.copy(status = to)))
            }
        }

    /**
     * Cierra la orden: valida la transicion y el kilometraje, actualiza el odometro del
     * vehiculo, reinicia el ciclo de los mantenimientos que se realizaron y marca como
     * atendida la cita de la que proviene.
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

    /** El fallo al marcar la cita no invalida la orden ya cerrada: solo queda en el log. */
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