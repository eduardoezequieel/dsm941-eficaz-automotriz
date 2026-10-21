package com.eficazautomotriz.app.ui.staff.reports

import com.eficazautomotriz.application.ReportRow
import com.eficazautomotriz.domain.model.enums.AppointmentStatus

/**
 * The four bars of "CITAS POR ESTADO" (f9). The mockup's "Finalizada" bar has no appointment
 * status behind it, so the honest split of the six statuses is: Confirmada, Atendida,
 * Pendiente/reprogramada (still open) and Rechazada/cancelada (closed without service).
 * [label] breaks before the slash, as the mockup's two-line x labels do.
 */
enum class AppointmentBarGroup(val label: String, val statuses: Set<AppointmentStatus>) {
    CONFIRMED("Confirmada", setOf(AppointmentStatus.CONFIRMED)),
    ATTENDED("Atendida", setOf(AppointmentStatus.ATTENDED)),
    OPEN("Pendiente\n/reprogramada", setOf(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED)),
    CLOSED("Rechazada\n/cancelada", setOf(AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED)),
}

/**
 * Sums the `appointmentsByStatus` rows (labelled with [AppointmentStatus.label]) into the
 * four groups. Always returns every group in enum order, with 0 for groups without rows,
 * so the chart keeps the same four columns in every period.
 */
fun groupAppointmentRows(rows: List<ReportRow>): List<Pair<AppointmentBarGroup, Int>> {
    val byStatus = rows.mapNotNull { row ->
        AppointmentStatus.entries.firstOrNull { it.label.equals(row.label.trim(), ignoreCase = true) }?.let { it to row.count }
    }
    return AppointmentBarGroup.entries.map { group ->
        group to byStatus.filter { (status, _) -> status in group.statuses }.sumOf { (_, count) -> count }
    }
}
