package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.StaffOrderRow
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Header counters and which appointment statuses still accept a staff decision. */
class StaffPanelLogicTest {

    private val today = LocalDate.of(2026, 10, 11)

    @Test
    fun `counters split today, pending requests and orders in progress`() {
        val counts = staffPanelCounts(
            pendingCount = 2,
            agendaDates = listOf(today, today, today.plusDays(1), today.minusDays(1)),
            orderStatuses = listOf(
                ServiceStatus.RECEIVED,
                ServiceStatus.IN_PROGRESS,
                ServiceStatus.IN_PROGRESS,
                ServiceStatus.COMPLETED,
            ),
            today = today,
        )
        assertEquals(StaffPanelCounts(appointmentsToday = 2, pendingConfirmation = 2, inProgress = 2), counts)
    }

    @Test
    fun `an empty panel reports zeros`() {
        assertEquals(
            StaffPanelCounts(appointmentsToday = 0, pendingConfirmation = 0, inProgress = 0),
            staffPanelCounts(pendingCount = 0, agendaDates = emptyList(), orderStatuses = emptyList(), today = today),
        )
    }

    @Test
    fun `only pending and rescheduled appointments can be decided`() {
        assertTrue(canDecideAppointment(AppointmentStatus.PENDING))
        assertTrue(canDecideAppointment(AppointmentStatus.RESCHEDULED))
        assertFalse(canDecideAppointment(AppointmentStatus.CONFIRMED))
        assertFalse(canDecideAppointment(AppointmentStatus.REJECTED))
        assertFalse(canDecideAppointment(AppointmentStatus.CANCELLED))
        assertFalse(canDecideAppointment(AppointmentStatus.ATTENDED))
    }

    @Test
    fun `confirmed agenda keeps confirmed rows in order and skips pending ones`() {
        fun row(id: String, status: AppointmentStatus) = AppointmentRow(
            Appointment(id, "C1", "V1", "T1", today, "S1", status, null, LocalDateTime.of(2026, 10, 1, 9, 0)),
            vehicle = null,
            serviceName = "Servicio",
            slot = null,
        )
        val pending = listOf(row("A2", AppointmentStatus.PENDING))
        val agenda = listOf(
            row("A1", AppointmentStatus.CONFIRMED),
            row("A2", AppointmentStatus.PENDING),
            row("A3", AppointmentStatus.RESCHEDULED),
            row("A4", AppointmentStatus.CONFIRMED),
        )
        assertEquals(listOf("A1", "A4"), confirmedAgenda(pending, agenda).map { it.appointment.id })
        assertTrue(confirmedAgenda(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `in progress section is newest first and skips received and completed`() {
        fun row(id: String, status: ServiceStatus, day: Int, hour: Int) = StaffOrderRow(
            ServiceOrder(
                id, "V", null, "T", null, status, "",
                LocalDateTime.of(2026, 10, day, hour, 0), null, null,
            ),
            serviceName = "Servicio",
            vehicle = null,
        )
        val shown = panelInProgressOrders(
            listOf(
                row("SV-1", ServiceStatus.RECEIVED, 11, 10),
                row("SV-2", ServiceStatus.IN_PROGRESS, 11, 8),
                row("SV-3", ServiceStatus.COMPLETED, 11, 9),
                row("SV-4", ServiceStatus.IN_PROGRESS, 10, 15),
                row("SV-5", ServiceStatus.IN_PROGRESS, 11, 7),
            ),
        )
        assertEquals(listOf("SV-2", "SV-5", "SV-4"), shown.map { it.order.id })
    }
}
