package com.eficazautomotriz

import com.eficazautomotriz.application.AppointmentService
import com.eficazautomotriz.application.AuthService
import com.eficazautomotriz.application.MaintenanceService
import com.eficazautomotriz.application.ReportService
import com.eficazautomotriz.application.ServiceOrderService
import com.eficazautomotriz.application.VehicleService
import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.ConsoleApp
import com.eficazautomotriz.cli.io.ConsoleReader
import com.eficazautomotriz.cli.io.ConsoleWriter
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.data.SeedData
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.rules.AppointmentAvailabilityRule
import com.eficazautomotriz.domain.rules.MaintenanceStatusRule
import com.eficazautomotriz.domain.rules.ServiceTransitionRule
import com.eficazautomotriz.logging.ErrorLogger
import com.eficazautomotriz.logging.FileErrorLogger
import java.time.LocalDate
import kotlin.system.exitProcess

/**
 * Punto de entrada: arma el grafo de dependencias a mano, carga los datos de
 * demostracion y arranca la consola. Aqui vive la unica captura de Exception
 * generica de todo el sistema.
 */
fun main() {
    val config = SystemConfig()
    val logger: ErrorLogger = FileErrorLogger(config.errorLogPath)

    try {
        buildConsoleApp(config, logger).start()
    } catch (e: Exception) {
        logger.error("Main", e::class.simpleName ?: "Exception", e.message ?: "sin detalle")
        println()
        println("[X] Ocurrio un error inesperado. El detalle quedo en ${config.errorLogPath}.")
        exitProcess(1)
    }
}

private fun buildConsoleApp(config: SystemConfig, logger: ErrorLogger): ConsoleApp {
    val repositories = Repositories()
    SeedData.load(repositories, LocalDate.now())

    val maintenanceService = MaintenanceService(
        repositories.vehicles,
        repositories.maintenanceRecords,
        repositories.maintenanceTypes,
        MaintenanceStatusRule(),
        config,
        logger,
    )
    val vehicleService = VehicleService(
        repositories.vehicles,
        repositories.maintenanceRecords,
        config,
        logger,
    )
    val appointmentService = AppointmentService(
        repositories.appointments,
        repositories.vehicles,
        repositories.timeSlots,
        repositories.serviceTypes,
        AppointmentAvailabilityRule(),
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
        ServiceTransitionRule(),
        logger,
    )
    val reportService = ReportService(
        repositories.appointments,
        repositories.serviceOrders,
        repositories.serviceTypes,
        repositories.vehicles,
        maintenanceService,
    )

    val writer = ConsoleWriter()
    val context = AppContext(
        auth = AuthService(repositories.users, logger),
        vehicles = vehicleService,
        appointments = appointmentService,
        serviceOrders = serviceOrderService,
        maintenance = maintenanceService,
        reports = reportService,
        reader = ConsoleReader(writer),
        writer = writer,
        config = config,
    )
    return ConsoleApp(context)
}
