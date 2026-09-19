package com.eficazautomotriz.application

import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.data.TimeSlotRepository
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.flatMap
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.UserRole
import com.eficazautomotriz.domain.rules.AppointmentAvailabilityRule
import com.eficazautomotriz.domain.rules.AvailabilityResult
import com.eficazautomotriz.domain.validation.TextFields
import com.eficazautomotriz.logging.ErrorLogger
import java.time.LocalDate
import java.time.LocalDateTime

/** Una franja del dia consultado junto con su disponibilidad ya evaluada. */
data class SlotAvailability(
    val slot: TimeSlot,
    val occupied: Int,
    val capacity: Int,
    val result: AvailabilityResult,
) {
    val selectable: Boolean get() = result == AvailabilityResult.Available
}

/** Solicitud, confirmacion, reprogramacion, rechazo y cancelacion de citas. */
class AppointmentService(
    private val appointments: AppointmentRepository,
    private val vehicles: VehicleRepository,
    private val timeSlots: TimeSlotRepository,
    private val serviceTypes: ServiceTypeRepository,
    private val availabilityRule: AppointmentAvailabilityRule,
    private val config: SystemConfig,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    fun listVisibleTo(actor: User): List<Appointment> = when (actor.role) {
        UserRole.CLIENT -> appointments.findByClient(actor.id)
        UserRole.STAFF -> appointments.findAll()
    }.sortedByDescending { it.date }

    fun listPending(actor: User): Outcome<List<Appointment>> =
        requireStaff("listPending", actor, "revisar las solicitudes pendientes")
            .flatMap {
                Outcome.success(
                    appointments.findByStatus(AppointmentStatus.PENDING).sortedBy { it.date }
                )
            }

    /** Franjas del dia consultado, marcando cuales no son seleccionables y por que. */
    fun slotsFor(date: LocalDate, today: LocalDate, excludingAppointmentId: String? = null): List<SlotAvailability> {
        val existing = appointments.findAll().filter { it.id != excludingAppointmentId }
        return timeSlots.findByDay(date.dayOfWeek).map { slot ->
            SlotAvailability(
                slot = slot,
                occupied = availabilityRule.countOccupying(slot.id, date, existing),
                capacity = availabilityRule.effectiveCapacity(slot, config),
                result = availabilityRule.check(slot, date, existing, today, config),
            )
        }
    }

    /** Catalogos de solo lectura: la consola nunca alcanza los repositorios. */
    fun activeServiceTypes(): List<ServiceType> = serviceTypes.findActive().sortedBy { it.name }

    fun serviceTypeName(serviceTypeId: String): String = serviceTypes.nameOf(serviceTypeId)

    fun slotCatalog(): List<TimeSlot> =
        timeSlots.findAll().sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))

    fun slotOf(slotId: String): TimeSlot? = timeSlots.findById(slotId)

    /**
     * Habilita o deshabilita una franja. Una franja deshabilitada la rechaza la primera
     * comprobacion de la regla de disponibilidad (`SLOT_DISABLED`); las citas ya agendadas
     * en ella no se tocan, solo deja de admitir nuevas.
     */
    fun setSlotEnabled(actor: User, slotId: String, enabled: Boolean): Outcome<TimeSlot> =
        requireStaff("setSlotEnabled", actor, "configurar las franjas horarias").flatMap {
            val slot = timeSlots.findById(slotId)
                ?: return@flatMap fail("setSlotEnabled", DomainError.NotFound("franja horaria", slotId))
            Outcome.success(timeSlots.save(slot.copy(enabled = enabled)))
        }

    /**
     * Cambia la capacidad de una franja. `0` significa «usar la capacidad por omision del
     * sistema», tal como la lee `effectiveCapacity`; negativo no tiene sentido y se rechaza
     * como error de negocio, no con una excepcion.
     */
    fun setSlotCapacity(actor: User, slotId: String, capacity: Int): Outcome<TimeSlot> =
        requireStaff("setSlotCapacity", actor, "configurar la capacidad de las franjas").flatMap {
            val slot = timeSlots.findById(slotId)
                ?: return@flatMap fail("setSlotCapacity", DomainError.NotFound("franja horaria", slotId))
            ValidationError.validateSlotCapacity(capacity, config.maxSlotCapacity)
                ?.let { return@flatMap failInvalid("setSlotCapacity", it, "slotId=$slotId capacity=$capacity") }
            Outcome.success(timeSlots.save(slot.copy(capacity = capacity)))
        }

    /** Cuantas citas ocupan hoy esa franja en la fecha dada: contexto antes de reducir la capacidad. */
    fun occupancyOf(slotId: String, date: LocalDate): Int =
        availabilityRule.countOccupying(slotId, date, appointments.findAll())

    /** La capacidad que la regla usara para esa franja, ya resuelta la caida al valor del sistema. */
    fun effectiveCapacityOf(slot: TimeSlot): Int = availabilityRule.effectiveCapacity(slot, config)

    fun request(
        actor: User,
        vehicleId: String,
        serviceTypeId: String,
        date: LocalDate,
        slotId: String,
        today: LocalDate,
        now: LocalDateTime,
    ): Outcome<Appointment> {
        val vehicle = vehicles.findById(vehicleId)
            ?: return fail("request", DomainError.NotFound("vehiculo", vehicleId))

        val permission = requireOwnershipOrStaff("request", actor, vehicle.ownerId, "agendar sobre el vehiculo $vehicleId")
        if (permission is Outcome.Failure) return permission

        if (serviceTypes.findById(serviceTypeId) == null) {
            return fail("request", DomainError.NotFound("tipo de servicio", serviceTypeId))
        }
        if (date.isBefore(today)) {
            return failInvalid("request", ValidationError.DateInPast, "date=$date")
        }

        val slot = timeSlots.findById(slotId)
            ?: return fail("request", DomainError.NotFound("franja horaria", slotId))

        val availability = availabilityRule.check(slot, date, appointments.findAll(), today, config)
        if (availability is AvailabilityResult.Unavailable) {
            return fail(
                "request",
                DomainError.SlotUnavailable(availability.reason),
                "clientId=${vehicle.ownerId} slotId=$slotId date=$date",
            )
        }

        val appointment = Appointment(
            id = appointments.nextId(),
            clientId = vehicle.ownerId,
            vehicleId = vehicleId,
            serviceTypeId = serviceTypeId,
            date = date,
            slotId = slotId,
            status = AppointmentStatus.PENDING,
            reason = null,
            requestedAt = now,
        )
        return Outcome.success(appointments.save(appointment))
    }

    /**
     * Segunda invocacion de la regla de disponibilidad. La lista excluye la propia cita:
     * de lo contrario se compararia consigo misma y siempre reportaria capacidad llena.
     */
    fun confirm(actor: User, appointmentId: String, today: LocalDate): Outcome<Appointment> =
        requireStaff("confirm", actor, "confirmar citas").flatMap {
            findOpenAppointment("confirm", appointmentId).flatMap { appointment ->
                val slot = timeSlots.findById(appointment.slotId)
                    ?: return@flatMap fail("confirm", DomainError.NotFound("franja horaria", appointment.slotId))

                val others = appointments.findAll().filter { it.id != appointment.id }
                val availability = availabilityRule.check(slot, appointment.date, others, today, config)
                if (availability is AvailabilityResult.Unavailable) {
                    return@flatMap fail(
                        "confirm",
                        DomainError.SlotUnavailable(availability.reason),
                        "appointmentId=$appointmentId slotId=${slot.id} date=${appointment.date}",
                    )
                }

                Outcome.success(
                    appointments.save(appointment.copy(status = AppointmentStatus.CONFIRMED, reason = null))
                )
            }
        }

    fun reject(actor: User, appointmentId: String, reason: String): Outcome<Appointment> =
        requireStaff("reject", actor, "rechazar citas").flatMap {
            findOpenAppointment("reject", appointmentId).flatMap { appointment ->
                ValidationError.validateText(TextFields.REASON, reason)
                    ?.let { return@flatMap failInvalid("reject", it, "appointmentId=$appointmentId") }

                Outcome.success(
                    appointments.save(appointment.copy(status = AppointmentStatus.REJECTED, reason = reason))
                )
            }
        }

    fun reschedule(
        actor: User,
        appointmentId: String,
        newDate: LocalDate,
        newSlotId: String,
        reason: String,
        today: LocalDate,
    ): Outcome<Appointment> = requireStaff("reschedule", actor, "reprogramar citas").flatMap {
        findOpenAppointment("reschedule", appointmentId).flatMap { appointment ->
            ValidationError.validateText(TextFields.REASON, reason)
                ?.let { return@flatMap failInvalid("reschedule", it, "appointmentId=$appointmentId") }

            val slot = timeSlots.findById(newSlotId)
                ?: return@flatMap fail("reschedule", DomainError.NotFound("franja horaria", newSlotId))

            val others = appointments.findAll().filter { it.id != appointment.id }
            val availability = availabilityRule.check(slot, newDate, others, today, config)
            if (availability is AvailabilityResult.Unavailable) {
                return@flatMap fail(
                    "reschedule",
                    DomainError.SlotUnavailable(availability.reason),
                    "appointmentId=$appointmentId slotId=$newSlotId date=$newDate",
                )
            }

            Outcome.success(
                appointments.save(
                    appointment.copy(
                        date = newDate,
                        slotId = newSlotId,
                        status = AppointmentStatus.RESCHEDULED,
                        reason = reason,
                    )
                )
            )
        }
    }

    fun cancel(actor: User, appointmentId: String): Outcome<Appointment> {
        val appointment = appointments.findById(appointmentId)
            ?: return fail("cancel", DomainError.NotFound("cita", appointmentId))

        val permission = requireOwnershipOrStaff("cancel", actor, appointment.clientId, "cancelar la cita $appointmentId")
        if (permission is Outcome.Failure) return permission

        if (appointment.status.isFinal) {
            return fail(
                "cancel",
                DomainError.Forbidden("cancelar una cita en estado ${appointment.status.label}"),
                "appointmentId=$appointmentId",
            )
        }
        return Outcome.success(appointments.save(appointment.copy(status = AppointmentStatus.CANCELLED)))
    }

    /** Marca la cita como atendida cuando el vehiculo ingresa al taller. */
    fun markAttended(actor: User, appointmentId: String): Outcome<Appointment> =
        requireStaff("markAttended", actor, "marcar citas como atendidas").flatMap {
            findOpenAppointment("markAttended", appointmentId).flatMap { appointment ->
                Outcome.success(appointments.save(appointment.copy(status = AppointmentStatus.ATTENDED)))
            }
        }

    private fun findOpenAppointment(operation: String, appointmentId: String): Outcome<Appointment> {
        val appointment = appointments.findById(appointmentId)
            ?: return fail(operation, DomainError.NotFound("cita", appointmentId))
        if (appointment.status.isFinal) {
            return fail(
                operation,
                DomainError.Forbidden("modificar una cita en estado ${appointment.status.label}"),
                "appointmentId=$appointmentId",
            )
        }
        return Outcome.success(appointment)
    }
}