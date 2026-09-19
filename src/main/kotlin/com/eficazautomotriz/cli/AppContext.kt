package com.eficazautomotriz.cli

import com.eficazautomotriz.application.AppointmentService
import com.eficazautomotriz.application.AuthService
import com.eficazautomotriz.application.MaintenanceService
import com.eficazautomotriz.application.ReportService
import com.eficazautomotriz.application.ServiceOrderService
import com.eficazautomotriz.application.VehicleService
import com.eficazautomotriz.cli.io.ConsoleReader
import com.eficazautomotriz.cli.io.ConsoleWriter
import com.eficazautomotriz.domain.config.SystemConfig
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dependencias que comparten los menus. Se construye una sola vez en Main y se
 * pasa por constructor: la consola nunca alcanza los repositorios directamente.
 *
 * El reloj entra como funcion para que la fecha se resuelva en el momento de usarla
 * y no al arrancar el programa.
 */
class AppContext(
    val auth: AuthService,
    val vehicles: VehicleService,
    val appointments: AppointmentService,
    val serviceOrders: ServiceOrderService,
    val maintenance: MaintenanceService,
    val reports: ReportService,
    val reader: ConsoleReader,
    val writer: ConsoleWriter,
    val config: SystemConfig,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) {
    fun now(): LocalDateTime = clock()

    fun today(): LocalDate = clock().toLocalDate()
}
