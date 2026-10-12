package com.eficazautomotriz.domain.model.enums

/** System roles. Stage 1 covers only the client and the shop staff. */
enum class UserRole(val label: String) {
    CLIENT("Cliente"),
    STAFF("Personal"),
}
