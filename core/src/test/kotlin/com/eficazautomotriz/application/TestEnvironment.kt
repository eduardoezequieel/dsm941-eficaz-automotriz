package com.eficazautomotriz.application

import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.data.SeedData
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.rules.AppointmentAvailabilityRule
import com.eficazautomotriz.domain.rules.MaintenanceStatusRule
import com.eficazautomotriz.domain.rules.ServiceTransitionRule
import com.eficazautomotriz.logging.ErrorLogger
import java.time.LocalDate
import java.time.LocalDateTime

/** Test logger: it keeps the events in memory instead of writing them to disk. */
class RecordingErrorLogger : ErrorLogger {

    val entries = mutableListOf<String>()

    override fun warn(context: String, message: String, data: String) {
        entries += "WARN|$context|$message|$data"
    }

    override fun error(context: String, message: String, data: String) {
        entries += "ERROR|$context|$message|$data"
    }
}

/**
 * Assembles the full dependency graph with the seed data, the same way Main does,
 * so the use-case tests run against a real system and not against test doubles.
 */
class TestEnvironment(val today: LocalDate = LocalDate.of(2026, 9, 16)) {

    val now: LocalDateTime = today.atTime(10, 0)
    val config = SystemConfig()
    val logger = RecordingErrorLogger()
    val repositories = Repositories()

    private val availabilityRule = AppointmentAvailabilityRule()
    private val maintenanceStatusRule = MaintenanceStatusRule()
    private val transitionRule = ServiceTransitionRule()

    val vehicleService = VehicleService(
        repositories.vehicles,
        repositories.maintenanceRecords,
        config,
        logger,
    )

    val maintenanceService = MaintenanceService(
        repositories.vehicles,
        repositories.maintenanceRecords,
        repositories.maintenanceTypes,
        maintenanceStatusRule,
        config,
        logger,
    )

    val appointmentService = AppointmentService(
        repositories.appointments,
        repositories.vehicles,
        repositories.timeSlots,
        repositories.serviceTypes,
        availabilityRule,
        config,
        logger,
    )

    val serviceOrderService = ServiceOrderService(
        repositories.serviceOrders,
        repositories.evidences,
        repositories.vehicles,
        repositories.appointments,
        repositories.serviceTypes,
        maintenanceService,
        vehicleService,
        transitionRule,
        config,
        logger,
    )

    val reportService = ReportService(
        repositories.appointments,
        repositories.serviceOrders,
        repositories.serviceTypes,
        repositories.vehicles,
        maintenanceService,
    )

    init {
        SeedData.load(repositories, today)
    }

    val firstClient: User get() = requireNotNull(repositories.users.findById("USR-001"))
    val secondClient: User get() = requireNotNull(repositories.users.findById("USR-002"))
    val staff: User get() = requireNotNull(repositories.users.findById("USR-003"))

    /** First free slot from tomorrow onward, useful for booking without a collision. */
    fun freeSlotOnOrAfterTomorrow(): Pair<LocalDate, String> {
        var date = today.plusDays(1)
        repeat(DAYS_TO_SCAN) {
            val free = appointmentService.slotsFor(date, today).firstOrNull { it.selectable }
            if (free != null) return date to free.slot.id
            date = date.plusDays(1)
        }
        error("Los datos semilla no dejaron ninguna franja libre en $DAYS_TO_SCAN dias")
    }

    private companion object {
        const val DAYS_TO_SCAN = 14
    }
}
