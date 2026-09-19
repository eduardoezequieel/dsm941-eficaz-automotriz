package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.validation.TextFields

/** Bandeja del personal: confirmar, reprogramar o rechazar solicitudes de cita. */
class RequestMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Solicitudes pendientes"

    private val appointmentMenu = AppointmentMenu(context, actor)

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Ver solicitudes pendientes") { listPending() },
        MenuItem("Confirmar una solicitud") { confirm() },
        MenuItem("Reprogramar una solicitud") { reschedule() },
        MenuItem("Rechazar una solicitud") { reject() },
        MenuItem("Ver todas las citas") { appointmentMenu.list() },
    )

    private fun listPending() {
        showTable(
            section = "Solicitudes pendientes",
            columns = appointmentMenu.columns(),
            rows = pendingAppointments(),
            emptyHint = "No hay solicitudes por atender.",
        )
        context.reader.pause()
    }

    private fun confirm() {
        val appointment = selectPending("Elija la solicitud a confirmar") ?: return
        context.writer.hint("Se revalida la disponibilidad de la franja antes de confirmar.")

        report(context.appointments.confirm(actor, appointment.id, context.today())) {
            "Cita ${it.id} confirmada para el ${Labels.date(it.date)} ${Labels.status(it.status)}."
        }
        context.reader.pause()
    }

    private fun reschedule() {
        val appointment = selectOpen("Elija la cita a reprogramar") ?: return
        val date = context.reader.readDate("Nueva fecha", appointmentMenu.schedulingRange()) ?: return
        val slot = appointmentMenu.selectAvailableSlot(date, excludingAppointmentId = appointment.id) ?: return
        val reason = context.reader.readText("Motivo de la reprogramacion", TextFields.REASON) ?: return

        report(
            context.appointments.reschedule(actor, appointment.id, date, slot.slot.id, reason, context.today())
        ) {
            "Cita ${it.id} reprogramada al ${Labels.date(it.date)} " +
                "a las ${Labels.slotRange(slot.slot)} ${Labels.status(it.status)}."
        }
        context.reader.pause()
    }

    private fun reject() {
        val appointment = selectOpen("Elija la cita a rechazar") ?: return
        val reason = context.reader.readText("Motivo del rechazo", TextFields.REASON) ?: return

        report(context.appointments.reject(actor, appointment.id, reason)) {
            "Cita ${it.id} rechazada. La franja quedo libre."
        }
        context.reader.pause()
    }

    private fun pendingAppointments(): List<Appointment> =
        contentsOf(context.appointments.listPending(actor))

    private fun selectPending(section: String): Appointment? = select(
        section = section,
        prompt = "Solicitud",
        options = pendingAppointments(),
        emptyMessage = "No hay solicitudes pendientes.",
        label = ::describe,
    )

    private fun selectOpen(section: String): Appointment? = select(
        section = section,
        prompt = "Cita",
        options = context.appointments.listVisibleTo(actor).filter { !it.status.isFinal },
        emptyMessage = "No hay citas activas.",
        label = ::describe,
    )

    private fun describe(appointment: Appointment): String =
        "${appointment.id}  ${Labels.date(appointment.date)}  " +
            "${appointmentMenu.slotLabel(appointment.slotId)}  " +
            "${context.auth.nameOf(appointment.clientId)}  " +
            "${context.appointments.serviceTypeName(appointment.serviceTypeId)}  " +
            Labels.status(appointment.status)
}