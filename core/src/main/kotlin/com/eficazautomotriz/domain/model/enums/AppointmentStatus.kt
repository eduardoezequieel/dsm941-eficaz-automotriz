package com.eficazautomotriz.domain.model.enums

/**
 * Lifecycle states of an appointment.
 *
 * @property occupiesSlot whether the appointment consumes a place in the slot.
 * @property isFinal whether the state closes the cycle and accepts no further changes.
 */
enum class AppointmentStatus(
    val label: String,
    val occupiesSlot: Boolean,
    val isFinal: Boolean,
) {
    PENDING("Pendiente", occupiesSlot = true, isFinal = false),
    CONFIRMED("Confirmada", occupiesSlot = true, isFinal = false),
    // A rescheduled appointment points at a new date and slot, and those do stay occupied.
    RESCHEDULED("Reprogramada", occupiesSlot = true, isFinal = false),
    REJECTED("Rechazada", occupiesSlot = false, isFinal = true),
    CANCELLED("Cancelada", occupiesSlot = false, isFinal = true),
    ATTENDED("Atendida", occupiesSlot = false, isFinal = true),
}
