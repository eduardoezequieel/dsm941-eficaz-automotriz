package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.cli.format.TableFormatter
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.User

/** Historial de servicios de un vehiculo, con evidencias y kilometraje registrado. */
class ServiceHistoryMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Historial de servicios"

    private val vehicleMenu = VehicleMenu(context, actor)

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Ver historial de un vehiculo") { showHistory() },
    )

    private fun showHistory() {
        val vehicle = vehicleMenu.selectVehicle("Elija el vehiculo") ?: return
        handle(context.serviceOrders.historyFor(actor, vehicle.id)) { render(it) }
        context.reader.pause()
    }

    private fun render(history: List<ServiceOrderDetail>) {
        val columns = listOf(
            Column<ServiceOrderDetail>("ID", 8) { it.order.id },
            Column("SERVICIO", 24) { context.serviceOrders.serviceTypeName(it.order.serviceTypeId) },
            Column("ESTADO", 14) { Labels.status(it.order.status) },
            Column("RECIBIDO", 17) { Labels.dateTime(it.order.receivedAt) },
            Column("CERRADO", 17) { Labels.dateTimeOrDash(it.order.closedAt) },
            Column("KM SERVICIO", 13, alignRight = true) { Labels.mileageOrDash(it.order.mileageAtService) },
            Column("EVIDENCIAS", 11, alignRight = true) { it.evidences.size.toString() },
        )
        showTable("Ordenes de servicio", columns, history, "Este vehiculo aun no tiene servicios registrados.")
        if (history.isEmpty()) return

        history.filter { it.evidences.isNotEmpty() }.forEach { detail ->
            context.writer.line()
            context.writer.line("Evidencias de ${detail.order.id}:")
            val evidenceColumns = listOf(
                Column<Evidence>("ETAPA", 14) { Labels.stage(it.stage) },
                Column("FECHA", 17) { Labels.dateTime(it.uploadedAt) },
                Column("DIRECCION", 52) { it.imageUrl },
            )
            TableFormatter.render(evidenceColumns, detail.evidences.sortedBy { it.uploadedAt })
                .forEach { context.writer.line("  $it") }
        }
        context.writer.line()
        context.writer.hint(
            "Las notas de cada orden se leen en Ordenes de servicio > Ver detalle de una orden."
        )
    }
}