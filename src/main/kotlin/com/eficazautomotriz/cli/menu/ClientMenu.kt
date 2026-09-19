package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.domain.model.User

/** Menu raiz del rol cliente. */
class ClientMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Menu de cliente - ${actor.name}"

    override val exitLabel: String = "Cerrar sesion"

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Mis vehiculos") { VehicleMenu(context, actor).run() },
        MenuItem("Estado de mantenimiento") { MaintenanceMenu(context, actor).run() },
        MenuItem("Mis citas") { AppointmentMenu(context, actor).run() },
        MenuItem("Historial de servicios") { ServiceHistoryMenu(context, actor).run() },
    )
}
