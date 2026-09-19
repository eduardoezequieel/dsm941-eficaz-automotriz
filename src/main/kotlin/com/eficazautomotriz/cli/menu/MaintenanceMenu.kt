package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.application.MaintenanceLine
import com.eficazautomotriz.application.VehicleMaintenanceReport
import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User

/** Visualizacion del modulo de procesamiento: el mantenimiento preventivo calculado. */
class MaintenanceMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Estado de mantenimiento"

    private val vehicleMenu = VehicleMenu(context, actor)

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Consultar un vehiculo") { showOne() },
        MenuItem("Resumen de todos los vehiculos visibles") { showAll() },
    )

    private fun showOne() {
        val vehicle = vehicleMenu.selectVehicle("Elija el vehiculo") ?: return
        handle(context.maintenance.reportFor(actor, vehicle.id)) { renderReport(it) }
        context.reader.pause()
    }

    private fun showAll() {
        // Un vehiculo sin reporte accesible se omite: el resumen no es el lugar para el error.
        val reports = context.vehicles.listVisibleTo(actor).mapNotNull { vehicle ->
            (context.maintenance.reportFor(actor, vehicle.id) as? Outcome.Success)?.value
        }
        val columns = listOf(
            Column<VehicleMaintenanceReport>("PLACA", 10) { it.vehicle.plate },
            Column("VEHICULO", 22) { "${it.vehicle.make} ${it.vehicle.model}" },
            Column("KILOMETRAJE", 13, alignRight = true) { Labels.mileage(it.vehicle.currentMileage) },
            Column("CLASIFICACION", 22) { Labels.status(it.overallStatus) },
        )
        showTable("Clasificacion por vehiculo", columns, reports, "No hay vehiculos que mostrar.")
        context.writer.hint("La clasificacion del vehiculo es la mas severa de sus tipos de mantenimiento.")
        context.reader.pause()
    }

    fun renderReport(report: VehicleMaintenanceReport) {
        val vehicle = report.vehicle
        context.writer.section("${vehicle.plate} - ${vehicle.make} ${vehicle.model} ${vehicle.year}")
        context.writer.line(
            "Kilometraje actual: ${Labels.mileage(vehicle.currentMileage)}" +
                "   Actualizado: ${Labels.date(vehicle.mileageUpdatedAt)}" +
                "   Clasificacion general: ${Labels.status(report.overallStatus)}"
        )
        context.writer.line()

        val lineColumns = listOf(
            Column<MaintenanceLine>("TIPO DE MANTENIMIENTO", 28) { it.type.name },
            Column("ULTIMO KM", 11, alignRight = true) { Labels.mileageOrDash(it.lastServiceMileage) },
            Column("INTERVALO", 11, alignRight = true) { Labels.mileage(it.type.intervalKm) },
            Column("RECOMENDADO", 13, alignRight = true) { Labels.mileageOrDash(it.evaluation.recommendedMileage) },
            Column("CLASIFICACION", 22) { Labels.status(it.evaluation.status) },
            Column("DIFERENCIA", 24) { Labels.difference(it.evaluation) },
        )
        showTable("Mantenimientos", lineColumns, report.lines, "Este vehiculo no tiene tipos de mantenimiento activos.")
    }
}