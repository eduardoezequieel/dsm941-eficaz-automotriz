package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.ReportFormatter
import com.eficazautomotriz.domain.model.User
import java.time.LocalDate

/** Los dos reportes comprometidos en la Etapa 1, mas el resumen del sistema. */
class ReportMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Reportes"

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Citas por estado en un periodo") { appointmentsByStatus() },
        MenuItem("Servicios finalizados por tipo") { completedServices() },
        MenuItem("Resumen del sistema") { summary() },
    )

    private fun appointmentsByStatus() {
        val period = readPeriod() ?: return
        context.writer.line()
        ReportFormatter.render(context.reports.appointmentsByStatus(period.first, period.second))
            .forEach(context.writer::line)
        context.reader.pause()
    }

    private fun completedServices() {
        val period = readPeriod() ?: return
        context.writer.line()
        ReportFormatter.render(context.reports.completedServicesByType(period.first, period.second))
            .forEach(context.writer::line)
        context.reader.pause()
    }

    private fun summary() {
        context.writer.line()
        ReportFormatter.render(context.reports.systemSummary()).forEach(context.writer::line)
        context.reader.pause()
    }

    /** Si la fecha final es anterior a la inicial se invierten, en vez de fallar. */
    private fun readPeriod(): Pair<LocalDate, LocalDate>? {
        context.writer.section("Periodo del reporte")
        val range = context.today().minusYears(context.config.reportHistoryYears)..context.today()
        val from = context.reader.readDate("Fecha inicial", range) ?: return null
        val to = context.reader.readDate("Fecha final", range) ?: return null
        return if (from.isAfter(to)) to to from else from to to
    }
}
