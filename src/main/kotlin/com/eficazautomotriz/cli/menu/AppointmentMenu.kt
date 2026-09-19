package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.application.SlotAvailability
import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.cli.format.TableFormatter
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.rules.AvailabilityResult
import java.time.LocalDate

/** Citas del cliente: consulta, solicitud y cancelacion. */
class AppointmentMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Mis citas"

    private val vehicleMenu = VehicleMenu(context, actor)

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Listar mis citas") { list() },
        MenuItem("Solicitar una cita") { request() },
        MenuItem("Cancelar una cita") { cancel() },
    )

    fun list() {
        showTable(
            section = "Citas registradas",
            columns = columns(),
            rows = context.appointments.listVisibleTo(actor),
            emptyHint = "No hay citas registradas.",
        )
        context.reader.pause()
    }

    fun columns(): List<Column<Appointment>> = listOf(
        Column("ID", 8) { it.id },
        Column("FECHA", 11) { Labels.date(it.date) },
        Column("HORA", 13) { slotLabel(it.slotId) },
        Column("VEHICULO", 10) { it.vehicleId },
        Column("SERVICIO", 24) { context.appointments.serviceTypeName(it.serviceTypeId) },
        Column("ESTADO", 15) { Labels.status(it.status) },
        Column("MOTIVO", 30) { it.reason ?: "-" },
    )

    /** Horario de la franja, o su identificador si el catalogo ya no la tiene. */
    fun slotLabel(slotId: String): String =
        context.appointments.slotOf(slotId)?.let(Labels::slotRange) ?: slotId

    /** Muestra las franjas del dia elegido, marcando las ocupadas como no seleccionables. */
    fun showSlots(date: LocalDate, excludingAppointmentId: String? = null): List<SlotAvailability> {
        val slots = context.appointments.slotsFor(date, context.today(), excludingAppointmentId)
        context.writer.section("Franjas del ${Labels.dayOfWeek(date.dayOfWeek)} ${Labels.date(date)}")
        if (slots.isEmpty()) {
            context.writer.hint("Ese dia no tiene atencion programada. El taller no abre los domingos.")
            return emptyList()
        }
        val columns = listOf(
            Column<SlotAvailability>("FRANJA", 8) { it.slot.id },
            Column("HORARIO", 14) { Labels.slotRange(it.slot) },
            Column("CUPOS", 9, alignRight = true) { "${it.occupied}/${it.capacity}" },
            // 50 es el ancho del motivo mas largo: menos recortaria el texto y ocultaria el porque.
            Column("DISPONIBILIDAD", 50) { describe(it) },
        )
        TableFormatter.render(columns, slots).forEach(context.writer::line)
        return slots
    }

    fun selectAvailableSlot(date: LocalDate, excludingAppointmentId: String? = null): SlotAvailability? {
        val selectable = showSlots(date, excludingAppointmentId).filter { it.selectable }
        if (selectable.isEmpty()) {
            context.writer.warning("No hay franjas disponibles ese dia. Elija otra fecha.")
            return null
        }
        return context.reader.readChoice("Franja", selectable) {
            "${it.slot.id}  ${Labels.slotRange(it.slot)}"
        }
    }

    /** Ventana que la consola acepta al agendar: de hoy al horizonte configurado. */
    fun schedulingRange(): ClosedRange<LocalDate> = context.today()..
        context.today().plusYears(context.config.schedulingHorizonYears)

    private fun request() {
        val vehicle = vehicleMenu.selectVehicle("Elija el vehiculo") ?: return
        val serviceType = context.reader.readChoice(
            "Tipo de servicio",
            context.appointments.activeServiceTypes(),
        ) { it.name } ?: return
        val date = context.reader.readDate("Fecha deseada", schedulingRange()) ?: return
        val slot = selectAvailableSlot(date) ?: return

        report(
            context.appointments.request(
                actor = actor,
                vehicleId = vehicle.id,
                serviceTypeId = serviceType.id,
                date = date,
                slotId = slot.slot.id,
                today = context.today(),
                now = context.now(),
            )
        ) {
            "Cita ${it.id} solicitada para el ${Labels.date(it.date)} " +
                "a las ${Labels.slotRange(slot.slot)} ${Labels.status(it.status)}."
        }
        context.reader.pause()
    }

    private fun cancel() {
        val appointment = select(
            section = "Citas activas",
            prompt = "Cita",
            options = context.appointments.listVisibleTo(actor).filter { !it.status.isFinal },
            emptyMessage = "No hay citas activas que cancelar.",
        ) {
            "${it.id}  ${Labels.date(it.date)}  ${Labels.status(it.status)}  " +
                context.appointments.serviceTypeName(it.serviceTypeId)
        } ?: return

        report(context.appointments.cancel(actor, appointment.id)) {
            "Cita ${it.id} cancelada. La franja quedo libre de inmediato."
        }
        context.reader.pause()
    }

    private fun describe(availability: SlotAvailability): String =
        when (val result = availability.result) {
            AvailabilityResult.Available -> "Disponible"
            is AvailabilityResult.Unavailable -> "[X] ${result.reason.label}"
        }
}
