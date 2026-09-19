package com.eficazautomotriz.domain.model.enums

/** Roles del sistema. La Etapa 1 solo contempla cliente y personal del taller. */
enum class UserRole(val label: String) {
    CLIENT("Cliente"),
    STAFF("Personal"),
}