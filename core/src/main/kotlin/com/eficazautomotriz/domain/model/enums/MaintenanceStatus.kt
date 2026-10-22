package com.eficazautomotriz.domain.model.enums

/** Classification of preventive maintenance. It is a computed value, never stored. */
enum class MaintenanceStatus(val label: String) {
    UP_TO_DATE("Al día"),
    DUE_SOON("Próximo"),
    OVERDUE("Vencido"),
    NO_PREVIOUS_RECORD("Sin registro previo"),
}
