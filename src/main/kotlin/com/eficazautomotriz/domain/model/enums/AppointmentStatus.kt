package com.eficazautomotriz.domain.model.enums

/**
 * Estados del ciclo de vida de una cita.
 *
 * @property occupiesSlot indica si la cita consume un cupo de la franja.
 * @property isFinal indica si el estado cierra el ciclo y ya no admite cambios.
 */
enum class AppointmentStatus(
    val label: String,
    val occupiesSlot: Boolean,
    val isFinal: Boolean,
) {
    PENDING("Pendiente", occupiesSlot = true, isFinal = false),
    CONFIRMED("Confirmada", occupiesSlot = true, isFinal = false),
    // Una cita reprogramada apunta a una fecha y franja nuevas que si quedan ocupadas.
    RESCHEDULED("Reprogramada", occupiesSlot = true, isFinal = false),
    REJECTED("Rechazada", occupiesSlot = false, isFinal = true),
    CANCELLED("Cancelada", occupiesSlot = false, isFinal = true),
    ATTENDED("Atendida", occupiesSlot = false, isFinal = true),
}