package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.domain.model.User

/** Menu raiz del rol personal del taller. */
class StaffMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Menu de personal - ${actor.name}"

    override val exitLabel: String = "Cerrar sesion"

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Solicitudes pendientes") { RequestMenu(context, actor).run() },
        MenuItem("Ordenes de servicio") { ServiceOrderMenu(context, actor).run() },
        MenuItem("Vehiculos y kilometraje") { VehicleAndMileageMenu(context, actor).run() },
        MenuItem("Catalogos") { CatalogMenu(context, actor).run() },
        MenuItem("Reportes") { ReportMenu(context, actor).run() },
    )
}

/** Consulta de cualquier vehiculo y su estado de mantenimiento, con edicion del odometro. */
class VehicleAndMileageMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Vehiculos y kilometraje"

    private val vehicleMenu = VehicleMenu(context, actor)
    private val maintenanceMenu = MaintenanceMenu(context, actor)

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Consultar vehiculos") { vehicleMenu.list() },
        MenuItem("Gestionar vehiculos (alta, edicion, baja, kilometraje)") { vehicleMenu.run() },
        MenuItem("Estado de mantenimiento") { maintenanceMenu.run() },
        MenuItem("Historial de servicios") { ServiceHistoryMenu(context, actor).run() },
    )
}