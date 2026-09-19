package com.eficazautomotriz.domain.model

data class MaintenanceType(
    override val id: String,
    val name: String,
    val intervalKm: Int,
    val active: Boolean,
) : Identifiable