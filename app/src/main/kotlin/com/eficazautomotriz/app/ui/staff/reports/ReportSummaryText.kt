package com.eficazautomotriz.app.ui.staff.reports

import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.viewmodel.staff.Reports
import com.eficazautomotriz.application.PeriodReport
import java.time.LocalDate

/**
 * Plain-text summary shared by "Exportar resumen" (P9). Uses the per-status rows, not the
 * grouped bars, so nothing is merged in the exported numbers.
 */
fun reportSummaryText(reports: Reports, from: LocalDate, to: LocalDate): String = buildString {
    appendLine("Eficaz Automotriz · Resumen de reportes")
    appendLine("Periodo: ${Formats.date(from)} – ${Formats.date(to)}")
    appendSection("Citas por estado", reports.appointmentsByStatus)
    appendSection("Servicios realizados por tipo", reports.completedServicesByType)
    appendLine()
    append("Vehículos registrados: ${reports.summary.totalVehicles}")
}

private fun StringBuilder.appendSection(title: String, report: PeriodReport) {
    appendLine()
    appendLine("$title (total ${report.total})")
    if (report.rows.isEmpty()) appendLine("- Sin datos en el periodo")
    report.rows.forEach { appendLine("- ${it.label}: ${it.count} (${ChartMath.formatShare(it.percentage)})") }
}
