package com.eficazautomotriz.domain.model.enums

/** States of a service order. Progress is linear and never moves backward. */
enum class ServiceStatus(val label: String) {
    RECEIVED("Recibido"),
    IN_PROGRESS("En proceso"),
    COMPLETED("Finalizado"),
}
