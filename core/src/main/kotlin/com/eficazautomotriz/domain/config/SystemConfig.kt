package com.eficazautomotriz.domain.config

/**
 * Configurable parameters of the system. No numeric business value may be
 * written directly into the rules or the menus: everything comes through here.
 *
 * The default values are for demonstration, not policies of Eficaz Automotriz.
 */
data class SystemConfig(
    val defaultSlotCapacity: Int = 1,
    val warningThresholdKm: Int = 500,
    val errorLogPath: String = "logs/errors.log",
    val minVehicleYear: Int = 1950,
    // Odometer ceiling: above this, the reading is a typing error, not a vehicle.
    val maxMileage: Int = 2_000_000,
    val maxSlotCapacity: Int = 20,
    // Date window the console accepts when booking or rescheduling.
    val schedulingHorizonYears: Long = 1,
    // Maximum age a report is allowed to query back into the past.
    val reportHistoryYears: Long = 10,
)
