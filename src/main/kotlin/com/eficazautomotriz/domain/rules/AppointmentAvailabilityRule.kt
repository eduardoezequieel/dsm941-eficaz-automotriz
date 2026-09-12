package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.SystemConfig
import com.eficazautomotriz.domain.model.TimeSlot
import java.time.LocalDate

sealed interface AvailabilityResult {
    object Available : AvailabilityResult

    class Unavailable(
        val reason: UnavailabilityReason,
        val message: String
    ) : AvailabilityResult
}

enum class UnavailabilityReason {
    SLOT_DISABLED,
    DATE_IN_PAST,
    DAY_MISMATCH,
    CAPACITY_REACHED
}

class AppointmentAvailabilityRule(
    private val today: LocalDate = LocalDate.now()
) {
    fun check(
        date: LocalDate,
        timeSlot: TimeSlot,
        appointments: List<Appointment>,
        systemConfig: SystemConfig
    ): AvailabilityResult {
        if (!timeSlot.enabled) {
            return AvailabilityResult.Unavailable(
                reason = UnavailabilityReason.SLOT_DISABLED,
                message = "La franja seleccionada esta deshabilitada."
            )
        }

        if (date.isBefore(today)) {
            return AvailabilityResult.Unavailable(
                reason = UnavailabilityReason.DATE_IN_PAST,
                message = "No se pueden agendar citas en fechas pasadas."
            )
        }

        if (date.dayOfWeek != timeSlot.dayOfWeek) {
            return AvailabilityResult.Unavailable(
                reason = UnavailabilityReason.DAY_MISMATCH,
                message = "La fecha no coincide con el dia configurado para la franja."
            )
        }

        val capacity = capacityFor(timeSlot, systemConfig)
        val occupiedCapacity = countAppointmentsThatOccupyCapacity(appointments)

        if (occupiedCapacity >= capacity) {
            return AvailabilityResult.Unavailable(
                reason = UnavailabilityReason.CAPACITY_REACHED,
                message = "La franja seleccionada ya alcanzo su capacidad maxima."
            )
        }

        return AvailabilityResult.Available
    }

    fun countAppointmentsThatOccupyCapacity(appointments: List<Appointment>): Int {
        return appointments.count { appointment -> appointment.occupiesCapacity }
    }

    private fun capacityFor(
        timeSlot: TimeSlot,
        systemConfig: SystemConfig
    ): Int {
        return if (timeSlot.capacity > 0) timeSlot.capacity else systemConfig.defaultSlotCapacity
    }
}
