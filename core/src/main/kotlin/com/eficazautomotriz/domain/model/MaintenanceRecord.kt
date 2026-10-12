package com.eficazautomotriz.domain.model

/**
 * Stores only the baseline mileage of the latest maintenance.
 * Recommended mileage and the classification are not stored: MaintenanceStatusRule
 * computes them on every query, so they never fall out of sync.
 */
data class MaintenanceRecord(
    override val id: String,
    val vehicleId: String,
    val maintenanceTypeId: String,
    // null means "no previous record", which is different from 0.
    val lastServiceMileage: Int?,
) : Identifiable
