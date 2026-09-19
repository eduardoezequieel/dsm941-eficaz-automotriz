package com.eficazautomotriz.domain.model.enums

/** Clasificacion del mantenimiento preventivo. Es un valor calculado, nunca almacenado. */
enum class MaintenanceStatus(val label: String) {
    UP_TO_DATE("Al dia"),
    DUE_SOON("Proximo"),
    OVERDUE("Vencido"),
    NO_PREVIOUS_RECORD("Sin registro previo"),
}