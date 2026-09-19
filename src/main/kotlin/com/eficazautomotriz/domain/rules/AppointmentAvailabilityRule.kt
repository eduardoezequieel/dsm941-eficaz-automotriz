package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.TimeSlot
import java.time.LocalDate

sealed interface AvailabilityResult {
    data object Available : AvailabilityResult
    data class Unavailable(val reason: UnavailabilityReason) : AvailabilityResult
}

enum class UnavailabilityReason(val label: String) {
    SLOT_DISABLED("la franja esta deshabilitada"),
    DATE_IN_PAST("la fecha ya paso"),
    DAY_MISMATCH("la franja no corresponde a ese dia de la semana"),
    CAPACITY_REACHED("la franja alcanzo su capacidad"),
}

/**
 * Regla 1: determina si una franja admite una cita mas en una fecha dada.
 *
 * Sin estado y sin dependencias: recibe todo por parametro y devuelve un resultado.
 * No consulta repositorios, no imprime y no lanza excepciones.
 */
class AppointmentAvailabilityRule {

    /**
     * Evalua en orden y devuelve en la primera condicion que falla:
     * 1) franja deshabilitada, 2) fecha pasada, 3) dia distinto, 4) capacidad llena.
     */
    fun check(
        slot: TimeSlot,
        date: LocalDate,
        existingAppointments: List<Appointment>,
        today: LocalDate,
        config: SystemConfig,
    ): AvailabilityResult {
        if (!slot.enabled) return unavailable(UnavailabilityReason.SLOT_DISABLED)
        if (date.isBefore(today)) return unavailable(UnavailabilityReason.DATE_IN_PAST)
        if (date.dayOfWeek != slot.dayOfWeek) return unavailable(UnavailabilityReason.DAY_MISMATCH)

        val occupied = countOccupying(slot.id, date, existingAppointments)
        if (occupied >= effectiveCapacity(slot, config)) {
            return unavailable(UnavailabilityReason.CAPACITY_REACHED)
        }
        return AvailabilityResult.Available
    }

    /** Cupos usados: solo cuentan los estados cuyo occupiesSlot es verdadero. */
    fun countOccupying(
        slotId: String,
        date: LocalDate,
        existingAppointments: List<Appointment>,
    ): Int = existingAppointments.count {
        it.slotId == slotId && it.date == date && it.status.occupiesSlot
    }

    /** Si la franja no declara capacidad valida, se usa la del sistema. */
    fun effectiveCapacity(slot: TimeSlot, config: SystemConfig): Int =
        if (slot.capacity > 0) slot.capacity else config.defaultSlotCapacity

    private fun unavailable(reason: UnavailabilityReason): AvailabilityResult =
        AvailabilityResult.Unavailable(reason)
}
