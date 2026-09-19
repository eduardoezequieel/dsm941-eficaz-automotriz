package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.validation.TextFields

/** Ciclo completo de la orden de servicio: crear, avanzar, evidenciar y finalizar. */
class ServiceOrderMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Ordenes de servicio"

    private val vehicleMenu = VehicleMenu(context, actor)

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Listar ordenes") { list() },
        MenuItem("Ver detalle de una orden") { showDetail() },
        MenuItem("Crear orden (recibir vehiculo)") { create() },
        MenuItem("Avanzar estado") { advance() },
        MenuItem("Registrar evidencia") { addEvidence() },
        MenuItem("Finalizar orden") { complete() },
    )

    private fun list() {
        val columns = listOf(
            Column<ServiceOrder>("ID", 8) { it.id },
            Column("VEHICULO", 10) { it.vehicleId },
            Column("SERVICIO", 24) { context.serviceOrders.serviceTypeName(it.serviceTypeId) },
            Column("ESTADO", 14) { Labels.status(it.status) },
            Column("CITA", 9) { it.appointmentId ?: "sin cita" },
            Column("RECIBIDO", 17) { Labels.dateTime(it.receivedAt) },
            Column("CERRADO", 17) { Labels.dateTimeOrDash(it.closedAt) },
            Column("KM SERVICIO", 13, alignRight = true) { Labels.mileageOrDash(it.mileageAtService) },
        )
        showTable("Ordenes registradas", columns, allOrders(), "No hay ordenes de servicio.")
        context.reader.pause()
    }

    /** Unico lugar donde se leen las notas de recepcion y las evidencias de una orden. */
    private fun showDetail() {
        val selected = selectOrder("Elija la orden a consultar") { true } ?: return
        handle(context.serviceOrders.detailOf(actor, selected.id)) { renderDetail(it) }
        context.reader.pause()
    }

    private fun renderDetail(detail: ServiceOrderDetail) {
        val order = detail.order
        context.writer.section("Orden ${order.id}")
        context.writer.line("  Vehiculo        ${order.vehicleId}")
        context.writer.line("  Servicio        ${context.serviceOrders.serviceTypeName(order.serviceTypeId)}")
        context.writer.line("  Estado          ${Labels.status(order.status)}")
        context.writer.line("  Cita            ${order.appointmentId ?: "sin cita"}")
        context.writer.line("  Recibido        ${Labels.dateTime(order.receivedAt)}")
        context.writer.line("  Cerrado         ${Labels.dateTimeOrDash(order.closedAt)}")
        context.writer.line("  Km al servicio  ${Labels.mileageOrDash(order.mileageAtService)}")

        context.writer.section("Notas de recepcion")
        if (order.notes.isBlank()) {
            context.writer.hint("La orden se registro sin notas.")
        } else {
            order.notes.lines().forEach { context.writer.line("  $it") }
        }

        context.writer.section("Evidencias (${detail.evidences.size})")
        if (detail.evidences.isEmpty()) {
            context.writer.hint("Esta orden no tiene evidencias registradas.")
            return
        }
        // Sin tabla de ancho fijo a proposito: esta es la unica vista donde se lee la
        // direccion completa, y recortarla la volveria inservible para abrirla.
        detail.evidences.sortedBy { it.uploadedAt }.forEachIndexed { index, evidence ->
            context.writer.line("  ${index + 1}) ${Labels.stage(evidence.stage)}  ${Labels.dateTime(evidence.uploadedAt)}")
            context.writer.line("     ${evidence.imageUrl}")
        }
    }

    private fun create() {
        val vehicle = vehicleMenu.selectVehicle("Elija el vehiculo que ingresa") ?: return
        val serviceType = context.reader.readChoice(
            "Tipo de servicio",
            context.appointments.activeServiceTypes(),
        ) { it.name } ?: return

        val appointment = readAppointmentChoice(vehicle.id) ?: return

        val notes = context.reader.readOptionalText("Notas de recepcion", TextFields.NOTES) ?: return

        report(
            context.serviceOrders.create(
                actor, vehicle.id, serviceType.id, appointment.appointmentId, notes, context.now(),
            )
        ) { "Orden ${it.id} creada ${Labels.status(it.status)} para ${vehicle.plate}." }
        context.reader.pause()
    }

    /**
     * «Sin cita» es una opcion de la lista, no la ausencia de eleccion: asi cancelar
     * (que devuelve null) deja de crear la orden por error.
     */
    private fun readAppointmentChoice(vehicleId: String): AppointmentChoice? {
        val open = context.appointments.listVisibleTo(actor)
            .filter { it.vehicleId == vehicleId && !it.status.isFinal }
        if (open.isEmpty()) {
            context.writer.hint("El vehiculo no tiene citas activas: la orden se registra sin cita.")
            return AppointmentChoice.WithoutAppointment
        }
        context.writer.section("Citas activas del vehiculo")
        val options = listOf(AppointmentChoice.WithoutAppointment) + open.map(AppointmentChoice::Scheduled)
        return context.reader.readChoice("Cita asociada", options) { it.label() }
    }

    private fun AppointmentChoice.label(): String = when (this) {
        AppointmentChoice.WithoutAppointment -> "Ingreso sin cita"
        is AppointmentChoice.Scheduled ->
            "${appointment.id}  ${Labels.date(appointment.date)}  ${Labels.status(appointment.status)}"
    }

    private fun advance() {
        val order = selectOrder("Elija la orden a avanzar") { it.status == ServiceStatus.RECEIVED } ?: return

        report(
            context.serviceOrders.advance(actor, order.id, ServiceStatus.IN_PROGRESS, context.now())
        ) { "Orden ${it.id} avanzo a ${Labels.status(it.status)}." }
        context.reader.pause()
    }

    private fun addEvidence() {
        val order = selectOrder("Elija la orden") { it.status != ServiceStatus.COMPLETED } ?: return
        val stage = context.reader.readChoice("Etapa", EvidenceStage.entries.toList()) { it.label } ?: return
        context.writer.hint("En consola no hay camara: se registra la direccion de la imagen.")
        val url = context.reader.readImageUrl("Direccion de la imagen") ?: return

        report(context.serviceOrders.addEvidence(actor, order.id, stage, url, context.now())) {
            "Evidencia ${it.id} registrada en la etapa ${Labels.stage(it.stage)} de ${order.id}."
        }
        context.reader.pause()
    }

    private fun complete() {
        val order = selectOrder("Elija la orden a finalizar") { it.status == ServiceStatus.IN_PROGRESS } ?: return
        val vehicle = context.vehicles.listVisibleTo(actor).firstOrNull { it.id == order.vehicleId }
        vehicle?.let { context.writer.hint("Ultimo kilometraje conocido: ${Labels.mileage(it.currentMileage)}") }

        val mileage = context.reader
            .readInt("Kilometraje al momento del servicio", 0, context.config.maxMileage) ?: return
        val performed = selectPerformedMaintenances()

        report(
            context.serviceOrders.complete(actor, order.id, mileage, performed, context.today(), context.now())
        ) {
            "Orden ${it.id} finalizada. Odometro actualizado a ${Labels.mileage(mileage)} " +
                "y ${performed.size} tipo(s) de mantenimiento reiniciados."
        }
        context.reader.pause()
    }

    /** Los mantenimientos marcados reinician su ciclo con el kilometraje de cierre. */
    private fun selectPerformedMaintenances(): List<String> {
        context.writer.section("Mantenimientos preventivos realizados")
        return context.maintenance.activeTypes()
            .filter { context.reader.confirm("Se realizo ${it.name}") }
            .map { it.id }
    }

    private fun allOrders(): List<ServiceOrder> = contentsOf(context.serviceOrders.listAll(actor))

    /** La cita asociada a una orden que ingresa: una existente o ninguna. */
    private sealed interface AppointmentChoice {

        val appointmentId: String?

        data object WithoutAppointment : AppointmentChoice {
            override val appointmentId: String? = null
        }

        data class Scheduled(val appointment: Appointment) : AppointmentChoice {
            override val appointmentId: String = appointment.id
        }
    }

    private fun selectOrder(section: String, filter: (ServiceOrder) -> Boolean): ServiceOrder? = select(
        section = section,
        prompt = "Orden",
        options = allOrders().filter(filter),
        emptyMessage = "No hay ordenes en el estado requerido para esta operacion.",
    ) {
        "${it.id}  ${it.vehicleId}  ${context.serviceOrders.serviceTypeName(it.serviceTypeId)}  " +
            Labels.status(it.status)
    }
}
