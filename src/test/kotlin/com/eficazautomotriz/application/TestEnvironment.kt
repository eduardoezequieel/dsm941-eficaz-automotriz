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

/** Logger de prueba: guarda los eventos en memoria en vez de escribir al disco. */
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
 * Arma el grafo completo de dependencias con los datos semilla, igual que Main,
 * para que las pruebas de casos de uso trabajen sobre un sistema real y no sobre dobles.
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

    /** Primera franja libre a partir de manana, util para agendar sin colisionar. */
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
