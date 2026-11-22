package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.TimeSlot
import java.time.LocalDate

/** Outcome of checking whether a time slot can accept another appointment on a date. */
sealed interface AvailabilityResult {
    data object Available : AvailabilityResult
    data class Unavailable(val reason: UnavailabilityReason) : AvailabilityResult
}

/** Business reason why a time slot cannot accept an appointment, ordered by evaluation priority. */
enum class UnavailabilityReason(val label: String) {
    SLOT_DISABLED("la franja está deshabilitada"),
    DATE_IN_PAST("la fecha ya paso"),
    DAY_MISMATCH("la franja no corresponde a ese día de la semana"),
    CAPACITY_REACHED("la franja alcanzo su capacidad"),
}

/**
 * Rule 1 (`docs/etapa2/REGLAS-Y-DECISIONES.md` §3.1 — "¿cabe una cita más en esta franja?"):
 * "devuelve en la primera condición que falla" and "nunca llama a `LocalDate.now()`" so callers
 * pass `today` explicitly, keeping availability checks stateless and deterministic.
 */
class AppointmentAvailabilityRule {

    /**
     * Evaluates in order and returns on the first condition that fails:
     * 1) disabled slot, 2) past date, 3) different day, 4) full capacity.
     */
    fun check(
        slot: TimeSlot,
        date: LocalDate,
        existingAppointments: List<Appointment>,
        today: LocalDate,
        config: SystemConfig,
    ): AvailabilityResult = check(slot, date, countOccupying(slot.id, date, existingAppointments), today, config)

    /**
     * Same evaluation when only the number of places already taken is known (the
     * persistence layer may keep per-slot counters instead of exposing every appointment).
     */
    fun check(
        slot: TimeSlot,
        date: LocalDate,
        occupied: Int,
        today: LocalDate,
        config: SystemConfig,
    ): AvailabilityResult {
        if (!slot.enabled) return unavailable(UnavailabilityReason.SLOT_DISABLED)
        if (date.isBefore(today)) return unavailable(UnavailabilityReason.DATE_IN_PAST)
        if (date.dayOfWeek != slot.dayOfWeek) return unavailable(UnavailabilityReason.DAY_MISMATCH)

        if (occupied >= effectiveCapacity(slot, config)) {
            return unavailable(UnavailabilityReason.CAPACITY_REACHED)
        }
        return AvailabilityResult.Available
    }

    /** Places taken: only states whose occupiesSlot is true are counted. */
    fun countOccupying(
        slotId: String,
        date: LocalDate,
        existingAppointments: List<Appointment>,
    ): Int = existingAppointments.count {
        it.slotId == slotId && it.date == date && it.status.occupiesSlot
    }

    /** When the slot declares no valid capacity, the system capacity is used. */
    fun effectiveCapacity(slot: TimeSlot, config: SystemConfig): Int =
        if (slot.capacity > 0) slot.capacity else config.defaultSlotCapacity

    private fun unavailable(reason: UnavailabilityReason): AvailabilityResult =
        AvailabilityResult.Unavailable(reason)
}