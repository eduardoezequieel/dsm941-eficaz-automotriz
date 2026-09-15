package com.eficazautomotriz.cli.format

import com.eficazautomotriz.application.PeriodReport
import com.eficazautomotriz.application.ReportRow
import com.eficazautomotriz.application.SystemSummary

/**
 * Dibuja los reportes. ReportService entrega estructuras de datos; las barras y
 * los porcentajes se arman aqui, en la unica capa que sabe de presentacion.
 */
object ReportFormatter {

    fun render(report: PeriodReport): List<String> {
        val header = "${report.title} - ${Labels.date(report.from)} a ${Labels.date(report.to)}"
        if (report.total == 0) {
            return listOf(header, "Sin registros en el periodo indicado.")
        }

        val maxCount = report.rows.maxOf { it.count }
        val labelWidth = report.rows.maxOf { it.label.length }.coerceAtLeast(MIN_LABEL_WIDTH)
        val body = report.rows.map { row -> renderRow(row, labelWidth, maxCount) }
        val total = "TOTAL".padEnd(labelWidth) + "  " + " ".repeat(BAR_WIDTH) +
            "  " + report.total.toString().padStart(COUNT_WIDTH)
        return listOf(header, "") + body + listOf(total)
    }

    fun render(summary: SystemSummary): List<String> {
        val lines = mutableListOf("RESUMEN DEL SISTEMA", "")
        lines += "Vehiculos registrados: ${summary.totalVehicles}"
        lines += ""
        lines += "Citas por estado:"
        summary.appointmentsByStatus.forEach { (status, count) ->
            lines += "  ${status.label.padEnd(MIN_LABEL_WIDTH)} ${count.toString().padStart(COUNT_WIDTH)}"
        }
        lines += ""
        lines += "Ordenes de servicio por estado:"
        summary.serviceOrdersByStatus.forEach { (status, count) ->
            lines += "  ${status.label.padEnd(MIN_LABEL_WIDTH)} ${count.toString().padStart(COUNT_WIDTH)}"
        }
        lines += ""
        lines += "Vehiculos por clasificacion de mantenimiento:"
        summary.vehiclesByMaintenanceStatus.forEach { (status, count) ->
            lines += "  ${status.label.padEnd(MIN_LABEL_WIDTH)} ${count.toString().padStart(COUNT_WIDTH)}"
        }
        return lines
    }

    private fun renderRow(row: ReportRow, labelWidth: Int, maxCount: Int): String {
        val bar = TableFormatter.bar(row.count, maxCount, BAR_WIDTH).padEnd(BAR_WIDTH)
        val count = row.count.toString().padStart(COUNT_WIDTH)
        val percentage = "(${Labels.percentage(row.percentage)})".padStart(PERCENTAGE_WIDTH)
        return "${row.label.padEnd(labelWidth)}  $bar  $count  $percentage"
    }

    private const val MIN_LABEL_WIDTH = 20
    private const val BAR_WIDTH = 20
    private const val COUNT_WIDTH = 3
    private const val PERCENTAGE_WIDTH = 9
}