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

/** One slot on the queried day, together with its availability already evaluated. */
data class SlotAvailability(
    val slot: TimeSlot,
    val occupied: Int,
    val capacity: Int,
    val result: AvailabilityResult,
) {
    val selectable: Boolean get() = result == AvailabilityResult.Available
}

/** Requesting, confirming, rescheduling, rejecting, and cancelling appointments. */
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

    /** Slots on the queried day, marking which ones are not selectable and why. */
    fun slotsFor(date: LocalDate, today: LocalDate, excludingAppointmentId: String? = null): List<SlotAvailability> {
        val occupancy = occupancyExcluding(date, excludingAppointmentId?.let(appointments::findById))
        return timeSlots.findByDay(date.dayOfWeek).map { slot ->
            val occupied = occupancy[slot.id] ?: 0
            SlotAvailability(
                slot = slot,
                occupied = occupied,
                capacity = availabilityRule.effectiveCapacity(slot, config),
                result = availabilityRule.check(slot, date, occupied, today, config),
            )
        }
    }

    /** Read-only catalogs: the console never reaches the repositories. */
    fun activeServiceTypes(): List<ServiceType> = serviceTypes.findActive().sortedBy { it.name }

    fun serviceTypeName(serviceTypeId: String): String = serviceTypes.nameOf(serviceTypeId)

    fun slotCatalog(): List<TimeSlot> =
        timeSlots.findAll().sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))

    fun slotOf(slotId: String): TimeSlot? = timeSlots.findById(slotId)

    /**
     * Enables or disables a slot. A disabled slot is rejected by the first
     * check of the availability rule (`SLOT_DISABLED`); appointments already booked
     * on it are left untouched, and it simply stops accepting new ones.
     */
    fun setSlotEnabled(actor: User, slotId: String, enabled: Boolean): Outcome<TimeSlot> =
        requireStaff("setSlotEnabled", actor, "configurar las franjas horarias").flatMap {
            val slot = timeSlots.findById(slotId)
                ?: return@flatMap fail("setSlotEnabled", DomainError.NotFound("franja horaria", slotId))
            Outcome.success(timeSlots.save(slot.copy(enabled = enabled)))
        }

    /**
     * Changes a slot's capacity. `0` means "use the system default capacity",
     * as `effectiveCapacity` reads it; a negative value is meaningless and is rejected
     * as a business error, not with an exception.
     */
    fun setSlotCapacity(actor: User, slotId: String, capacity: Int): Outcome<TimeSlot> =
        requireStaff("setSlotCapacity", actor, "configurar la capacidad de las franjas").flatMap {
            val slot = timeSlots.findById(slotId)
                ?: return@flatMap fail("setSlotCapacity", DomainError.NotFound("franja horaria", slotId))
            ValidationError.validateSlotCapacity(capacity, config.maxSlotCapacity)
                ?.let { return@flatMap failInvalid("setSlotCapacity", it, "slotId=$slotId capacity=$capacity") }
            Outcome.success(timeSlots.save(slot.copy(capacity = capacity)))
        }

    /** How many appointments occupy that slot on the given date: context before lowering capacity. */
    fun occupancyOf(slotId: String, date: LocalDate): Int = occupiedPlaces(slotId, date)

    /**
     * Places taken in the slot on that date. `excluding` is an open appointment being confirmed
     * or moved: otherwise it would be compared with itself and would always report full capacity.
     */
    private fun occupiedPlaces(slotId: String, date: LocalDate, excluding: Appointment? = null): Int =
        occupancyExcluding(date, excluding)[slotId] ?: 0

    private fun occupancyExcluding(date: LocalDate, excluding: Appointment?): Map<String, Int> {
        val occupancy = appointments.occupancyOn(date)
        if (excluding == null || !excluding.status.occupiesSlot || excluding.date != date) return occupancy
        val remaining = ((occupancy[excluding.slotId] ?: 0) - 1).coerceAtLeast(0)
        return occupancy + (excluding.slotId to remaining)
    }

    /** The capacity the rule will use for that slot, once the fallback to the system value is resolved. */
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

        val availability = availabilityRule.check(slot, date, occupiedPlaces(slotId, date), today, config)
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
     * Second call to the availability rule. The count excludes the appointment itself:
     * otherwise it would be compared with itself and would always report full capacity.
     */
    fun confirm(actor: User, appointmentId: String, today: LocalDate): Outcome<Appointment> =
        requireStaff("confirm", actor, "confirmar citas").flatMap {
            findOpenAppointment("confirm", appointmentId).flatMap { appointment ->
                val slot = timeSlots.findById(appointment.slotId)
                    ?: return@flatMap fail("confirm", DomainError.NotFound("franja horaria", appointment.slotId))

                val occupied = occupiedPlaces(slot.id, appointment.date, excluding = appointment)
                val availability = availabilityRule.check(slot, appointment.date, occupied, today, config)
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
                val trimmed = reason.trim()
                ValidationError.validateText(TextFields.REASON, trimmed)
                    ?.let { return@flatMap failInvalid("reject", it, "appointmentId=$appointmentId") }

                Outcome.success(
                    appointments.save(appointment.copy(status = AppointmentStatus.REJECTED, reason = trimmed))
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
            val trimmed = reason.trim()
            ValidationError.validateText(TextFields.REASON, trimmed)
                ?.let { return@flatMap failInvalid("reschedule", it, "appointmentId=$appointmentId") }

            val slot = timeSlots.findById(newSlotId)
                ?: return@flatMap fail("reschedule", DomainError.NotFound("franja horaria", newSlotId))

            val occupied = occupiedPlaces(newSlotId, newDate, excluding = appointment)
            val availability = availabilityRule.check(slot, newDate, occupied, today, config)
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
                        reason = trimmed,
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

    /**
     * The client accepts the date and slot proposed by the shop. The reason is kept so the
     * client can still see why it was moved; the slot was already occupied while RESCHEDULED.
     */
    fun acceptReschedule(actor: User, appointmentId: String): Outcome<Appointment> =
        findRescheduledOf("acceptReschedule", actor, appointmentId, "aceptar la reprogramacion").flatMap { appointment ->
            Outcome.success(appointments.save(appointment.copy(status = AppointmentStatus.CONFIRMED)))
        }

    /** The client declines the new date: the appointment is cancelled and the slot freed. */
    fun declineReschedule(actor: User, appointmentId: String): Outcome<Appointment> =
        findRescheduledOf("declineReschedule", actor, appointmentId, "rechazar la reprogramacion").flatMap { appointment ->
            Outcome.success(appointments.save(appointment.copy(status = AppointmentStatus.CANCELLED)))
        }

    /** Marks the appointment as attended when the vehicle enters the shop. */
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

    /** Only the owner (or staff) may answer a reschedule, and only while it is still RESCHEDULED. */
    private fun findRescheduledOf(
        operation: String,
        actor: User,
        appointmentId: String,
        action: String,
    ): Outcome<Appointment> {
        val appointment = appointments.findById(appointmentId)
            ?: return fail(operation, DomainError.NotFound("cita", appointmentId))

        val permission = requireOwnershipOrStaff(operation, actor, appointment.clientId, "$action de la cita $appointmentId")
        if (permission is Outcome.Failure) return permission

        if (appointment.status != AppointmentStatus.RESCHEDULED) {
            return fail(
                operation,
                DomainError.Forbidden("$action de una cita en estado ${appointment.status.label}"),
                "appointmentId=$appointmentId",
            )
        }
        return Outcome.success(appointment)
    }
}
