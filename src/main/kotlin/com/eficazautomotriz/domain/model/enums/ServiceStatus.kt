package com.eficazautomotriz.domain.model.enums

/** Estados de una orden de servicio. El avance es lineal y sin retroceso. */
enum class ServiceStatus(val label: String) {
    RECEIVED("Recibido"),
    IN_PROGRESS("En proceso"),
    COMPLETED("Finalizado"),
}