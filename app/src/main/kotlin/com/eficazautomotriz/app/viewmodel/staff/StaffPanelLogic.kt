package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.StaffOrderRow
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDate

/** P8 header: appointments dated today, requests still pending, orders currently in progress. */
data class StaffPanelCounts(
    val appointmentsToday: Int,
    val pendingConfirmation: Int,
    val inProgress: Int,
)

/**
 * Counts for the three tiles above the staff tabs.
 *
 * [agendaDates] is the schedule from today onward, so "today" is a filter on that list,
 * not a separate query. Pending requests and in-progress orders are independent of it:
 * a request for today counts in both the first and the second tile.
 */
fun staffPanelCounts(
    pendingCount: Int,
    agendaDates: List<LocalDate>,
    orderStatuses: List<ServiceStatus>,
    today: LocalDate,
): StaffPanelCounts = StaffPanelCounts(
    appointmentsToday = agendaDates.count { it == today },
    pendingConfirmation = pendingCount,
    inProgress = orderStatuses.count { it == ServiceStatus.IN_PROGRESS },
)

/**
 * Staff may confirm, reject or reschedule only while the request is still open.
 * [AppointmentStatus.CONFIRMED] is not final, but the decision was already made.
 */
fun canDecideAppointment(status: AppointmentStatus): Boolean =
    status == AppointmentStatus.PENDING || status == AppointmentStatus.RESCHEDULED

/**
 * The panel's "Agenda confirmada": confirmed appointments of [agenda] (already sorted by date
 * and slot) that are not listed among the [pending] requests above it.
 */
fun confirmedAgenda(pending: List<AppointmentRow>, agenda: List<AppointmentRow>): List<AppointmentRow> {
    val pendingIds = pending.mapTo(HashSet()) { it.appointment.id }
    return agenda.filter { it.appointment.status == AppointmentStatus.CONFIRMED && it.appointment.id !in pendingIds }
}

/**
 * Orders under "Servicios en proceso" on f8. Only [ServiceStatus.IN_PROGRESS]: the card chip
 * is "En proceso" and the actions are Subir evidencia / Finalizar. Newest reception first, so
 * the job received today leads. Received orders stay on the Servicios tab, where they can be started.
 */
fun panelInProgressOrders(orders: List<StaffOrderRow>): List<StaffOrderRow> =
    orders.filter { it.order.status == ServiceStatus.IN_PROGRESS }
        .sortedByDescending { it.order.receivedAt }
