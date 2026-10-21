package com.eficazautomotriz.app.ui.client

import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.NewAppointmentState
import com.eficazautomotriz.app.viewmodel.SlotOption
import com.eficazautomotriz.app.viewmodel.splitClientAppointments
import com.eficazautomotriz.app.viewmodel.VehicleSummary
import com.eficazautomotriz.application.MaintenanceLine
import com.eficazautomotriz.application.SlotAvailability
import com.eficazautomotriz.application.VehicleMaintenanceReport
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.rules.AvailabilityResult
import com.eficazautomotriz.domain.rules.MaintenanceEvaluation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/** Pure display logic of the client screens P3–P6. */
class ClientScreensLogicTest {

    private val day = LocalDate.of(2026, 8, 19)
    private val corolla = Vehicle("VEH-001", "USR-001", "P123456", "Toyota", "Corolla", 2016, 78_400, day)
    private val frontier = Vehicle("VEH-002", "USR-001", "P987321", "Nissan", "Frontier", 2019, 84_100, day)

    @Test
    fun `the current month starts at today's week, as f6 shows it`() {
        val weeks = calendarWeeks(YearMonth.of(2026, 8), today = day)
        assertEquals(3, weeks.size)
        assertEquals((17..23).toList(), weeks.first().map { it!!.dayOfMonth })
        assertEquals(listOf(31, null, null, null, null, null, null), weeks.last().map { it?.dayOfMonth })
    }

    @Test
    fun `a later month shows every week with Monday first`() {
        val weeks = calendarWeeks(YearMonth.of(2026, 9), today = day)
        // 1 September 2026 is a Tuesday.
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), weeks.first().map { it?.dayOfMonth })
        assertEquals(30, weeks.flatten().filterNotNull().size)
    }

    @Test
    fun `the calendar note names the closed weekdays`() {
        val mondayToSaturday = DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY
        assertEquals(
            "Los domingos aparecen deshabilitados según los días de atención configurados.",
            closedDaysNote(mondayToSaturday),
        )
        assertEquals(
            "Los sábados y domingos aparecen deshabilitados según los días de atención configurados.",
            closedDaysNote(mondayToSaturday - DayOfWeek.SATURDAY),
        )
        assertNull(closedDaysNote(emptySet()))
        assertNull(closedDaysNote(DayOfWeek.entries.toSet()))
    }

    @Test
    fun `f5 chips and the rejection reason`() {
        assertEquals("Pendiente de confirmación", appointmentCardLabel(AppointmentStatus.PENDING))
        assertEquals("Reprogramada por el taller", appointmentCardLabel(AppointmentStatus.RESCHEDULED))
        assertEquals("Confirmada", appointmentCardLabel(AppointmentStatus.CONFIRMED))
        assertEquals("Motivo indicado por el taller: horario no disponible.", rejectionText("Horario no disponible."))
        assertEquals("Motivo indicado por el taller: sin detalle.", rejectionText(null))
    }

    @Test
    fun `f5 keeps a rejection on proximas and strikes the previous slot`() {
        val wednesday = LocalDate.of(2026, 8, 19)
        val thursday = LocalDate.of(2026, 8, 20)
        val reason = "Fecha anterior: 2026-08-19 14:00. El taller movió la cita: el técnico no estará."
        val rows = listOf(
            appointmentRow("CT-1028", AppointmentStatus.REJECTED, wednesday.minusDays(2)),
            appointmentRow("CT-1042", AppointmentStatus.CONFIRMED, wednesday),
            appointmentRow("CT-1030", AppointmentStatus.ATTENDED, wednesday.minusDays(10)),
            appointmentRow("CT-1039", AppointmentStatus.RESCHEDULED, thursday, reason),
        )
        val split = splitClientAppointments(rows)
        assertEquals(listOf("CT-1042", "CT-1039", "CT-1028"), split.upcoming.map { it.appointment.id })
        assertEquals(listOf("CT-1030"), split.history.map { it.appointment.id })
        val line = appointmentDateLine(split.upcoming.first { it.appointment.id == "CT-1039" })
        assertEquals("Mié 19 ago · 2:00 p.m. → Jue 20 ago · 9:00 a.m.", line.text)
        assertEquals(
            "El taller movió la cita: el técnico no estará.",
            rescheduleExplanation(reason),
        )
    }

    private fun appointmentRow(
        id: String,
        status: AppointmentStatus,
        date: LocalDate,
        reason: String? = null,
    ) = AppointmentRow(
        appointment = Appointment(id, "USR-001", "VEH-001", "SVT-001", date, "SLT-1", status, reason, LocalDateTime.of(2026, 8, 1, 18, 20)),
        vehicle = null,
        serviceName = "Servicio",
        slot = TimeSlot("SLT-1", DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), true, 1),
    )

    private fun line(name: String, last: Int?, interval: Int, status: MaintenanceStatus, current: Int = 84_100): MaintenanceLine {
        val recommended = last?.plus(interval)
        return MaintenanceLine(
            MaintenanceType("MNT-$name", name, interval, true),
            last,
            MaintenanceEvaluation(status, recommended, recommended?.minus(current)),
        )
    }

    @Test
    fun `f4 bottom action follows the worst line and hides when all are up to date`() {
        val upToDate = line("Frenos", 80_000, 20_000, MaintenanceStatus.UP_TO_DATE)
        val dueSoon = line("Rotación", 74_500, 10_000, MaintenanceStatus.DUE_SOON)
        val overdue = line("Aceite", 79_000, 5_000, MaintenanceStatus.OVERDUE)
        fun report(vararg lines: MaintenanceLine) = VehicleMaintenanceReport(frontier, lines.toList(), MaintenanceStatus.UP_TO_DATE)

        assertEquals("Agendar mantenimiento vencido", scheduleActionLabel(report(upToDate, dueSoon, overdue)))
        assertEquals("Agendar mantenimiento", scheduleActionLabel(report(upToDate, dueSoon)))
        assertNull(scheduleActionLabel(report(upToDate)))
    }

    @Test
    fun `f4 plan lists overdue, then due soon, up to date and without record`() {
        val lines = listOf(
            line("Frenos", 80_000, 20_000, MaintenanceStatus.UP_TO_DATE),
            line("Filtro", null, 15_000, MaintenanceStatus.NO_PREVIOUS_RECORD),
            line("Rotación", 74_500, 10_000, MaintenanceStatus.DUE_SOON),
            line("Aceite", 79_000, 5_000, MaintenanceStatus.OVERDUE),
        )
        assertEquals(listOf("Aceite", "Rotación", "Frenos", "Filtro"), sortedBySeverity(lines).map { it.type.name })
    }

    @Test
    fun `f4 recommendation reads exceeded or remaining kilometers`() {
        assertEquals(
            "Recomendado a los 84,000 km · excedido por 100 km",
            recommendationText(line("Aceite", 79_000, 5_000, MaintenanceStatus.OVERDUE)).text,
        )
        assertEquals(
            "Recomendado a los 84,500 km · faltan 400 km",
            recommendationText(line("Rotación", 74_500, 10_000, MaintenanceStatus.DUE_SOON)).text,
        )
    }

    @Test
    fun `search matches the displayed plate, the make and the model`() {
        val vehicles = listOf(VehicleSummary(corolla, MaintenanceStatus.UP_TO_DATE), VehicleSummary(frontier, MaintenanceStatus.DUE_SOON))
        assertEquals(listOf(frontier), filterVehicles(vehicles, "P987-321").map { it.vehicle })
        assertEquals(listOf(frontier), filterVehicles(vehicles, "p987321").map { it.vehicle })
        assertEquals(emptyList<Vehicle>(), filterVehicles(vehicles, "---").map { it.vehicle })
        assertEquals(listOf(corolla), filterVehicles(vehicles, "corolla").map { it.vehicle })
        assertEquals(listOf(frontier), filterVehicles(vehicles, "Nissan Frontier").map { it.vehicle })
        assertEquals(2, filterVehicles(vehicles, "  ").size)
    }

    @Test
    fun `the f6 header step follows the choices made`() {
        val type = ServiceType("SVT-001", "Mantenimiento preventivo", "", true)
        val slot = SlotOption(
            SlotAvailability(TimeSlot("SLT-1", DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), true, 2), 0, 2, AvailabilityResult.Available),
            unavailableLabel = null,
        )
        val empty = NewAppointmentState(loading = false)
        assertEquals(1 to "Vehículo", empty.progress)
        assertEquals(2 to "Servicio", empty.copy(vehicle = corolla).progress)
        val dated = empty.copy(vehicle = corolla, serviceType = type, date = LocalDate.of(2026, 8, 21))
        assertEquals(3 to "Fecha y horario", dated.progress)
        assertFalse(dated.canSubmit)
        val ready = dated.copy(slot = slot)
        // f6 keeps "Paso 3 de 4 · Fecha y horario" once the slot is chosen; the summary is inline.
        assertEquals(3 to "Fecha y horario", ready.progress)
        assertTrue(ready.canSubmit)
        assertFalse(ready.copy(submitting = true).canSubmit)
    }
}
