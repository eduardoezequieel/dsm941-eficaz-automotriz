package com.eficazautomotriz.app.ui.staff.reports

import com.eficazautomotriz.app.viewmodel.staff.Reports
import com.eficazautomotriz.application.PeriodReport
import com.eficazautomotriz.application.ReportRow
import com.eficazautomotriz.application.SystemSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Axis ticks, legend text, the four f9 bar groups and the exported summary. */
class ReportChartsTest {

    @Test
    fun `axis ticks use nice steps that cover the maximum`() {
        assertEquals(listOf(0, 8, 16, 24), ChartMath.axisTicks(21))
        assertEquals(listOf(0, 2, 4, 6), ChartMath.axisTicks(5))
        assertEquals(listOf(0, 10, 20, 30), ChartMath.axisTicks(30))
        assertEquals(listOf(0, 1, 2, 3), ChartMath.axisTicks(0))
        assertEquals(listOf(0, 1, 2, 3), ChartMath.axisTicks(3))
        assertEquals(listOf(0, 20, 40, 60), ChartMath.axisTicks(31))
    }

    @Test
    fun `legend shows whole percentages and shortened names`() {
        assertEquals("30%", ChartMath.wholePercent(29.6))
        assertEquals("12%", ChartMath.wholePercent(12.4))
        assertEquals("Mant. preventivo", ChartMath.legendLabel("Mantenimiento preventivo"))
        assertEquals("Diagnóstico escáner", ChartMath.legendLabel("Diagnóstico por escáner"))
        assertEquals("Sistema de frenos", ChartMath.legendLabel("Sistema de frenos"))
    }

    @Test
    fun `status rows are summed into the four groups in fixed order`() {
        val rows = listOf(
            ReportRow("Pendiente", 3, 0.0),
            ReportRow("Confirmada", 4, 0.0),
            ReportRow("Reprogramada", 1, 0.0),
            ReportRow("Rechazada", 3, 0.0),
            ReportRow("Cancelada", 1, 0.0),
            ReportRow("Atendida", 16, 0.0),
        )
        assertEquals(
            listOf(
                AppointmentBarGroup.CONFIRMED to 4,
                AppointmentBarGroup.ATTENDED to 16,
                AppointmentBarGroup.OPEN to 4,
                AppointmentBarGroup.CLOSED to 4,
            ),
            groupAppointmentRows(rows),
        )
    }

    @Test
    fun `missing statuses still produce every group with zero`() {
        val groups = groupAppointmentRows(listOf(ReportRow("Atendida", 2, 100.0)))
        assertEquals(AppointmentBarGroup.entries.toList(), groups.map { it.first })
        assertEquals(listOf(0, 2, 0, 0), groups.map { it.second })
        assertEquals(listOf(0, 0, 0, 0), groupAppointmentRows(emptyList()).map { it.second })
    }

    @Test
    fun `summary text lists the period, every row and the vehicle count`() {
        val from = LocalDate.of(2026, 10, 1)
        val to = LocalDate.of(2026, 10, 11)
        val text = reportSummaryText(
            Reports(
                appointmentsByStatus = PeriodReport("CITAS POR ESTADO", from, to, listOf(ReportRow("Atendida", 16, 80.0), ReportRow("Rechazada", 4, 20.0)), 20),
                completedServicesByType = PeriodReport("SERVICIOS", from, to, emptyList(), 0),
                summary = SystemSummary(9, emptyMap(), emptyMap(), emptyMap()),
            ),
            from,
            to,
        )
        assertTrue(text.contains("Periodo: 01/10/2026 – 11/10/2026"), text)
        assertTrue(text.contains("Citas por estado (total 20)"), text)
        assertTrue(text.contains("- Atendida: 16 (80.0 %)"), text)
        assertTrue(text.contains("Servicios realizados por tipo (total 0)\n- Sin datos en el periodo"), text)
        assertTrue(text.endsWith("Vehículos registrados: 9"), text)
    }
}
