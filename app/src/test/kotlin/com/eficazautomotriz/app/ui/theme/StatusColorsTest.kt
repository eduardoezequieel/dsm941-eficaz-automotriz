package com.eficazautomotriz.app.ui.theme

import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Chip tones follow the mockups: blue confirmed, orange pending, green up to date, red overdue, gray rescheduled. */
class StatusColorsTest {

    @Test
    fun `maintenance tones`() {
        assertEquals(StatusTone.Success, StatusColors.toneForMaintenance(MaintenanceStatus.UP_TO_DATE))
        assertEquals(StatusTone.Warning, StatusColors.toneForMaintenance(MaintenanceStatus.DUE_SOON))
        assertEquals(StatusTone.Danger, StatusColors.toneForMaintenance(MaintenanceStatus.OVERDUE))
        assertEquals(StatusTone.Neutral, StatusColors.toneForMaintenance(MaintenanceStatus.NO_PREVIOUS_RECORD))
    }

    @Test
    fun `appointment tones`() {
        assertEquals(StatusTone.Warning, StatusColors.toneForAppointment(AppointmentStatus.PENDING))
        assertEquals(StatusTone.Info, StatusColors.toneForAppointment(AppointmentStatus.CONFIRMED))
        assertEquals(StatusTone.Info, StatusColors.toneForAppointment(AppointmentStatus.ATTENDED))
        assertEquals(StatusTone.Neutral, StatusColors.toneForAppointment(AppointmentStatus.RESCHEDULED))
        assertEquals(StatusTone.Danger, StatusColors.toneForAppointment(AppointmentStatus.REJECTED))
        assertEquals(StatusTone.Neutral, StatusColors.toneForAppointment(AppointmentStatus.CANCELLED))
    }

    @Test
    fun `service tones`() {
        assertEquals(StatusTone.Neutral, StatusColors.toneForService(ServiceStatus.RECEIVED))
        assertEquals(StatusTone.Info, StatusColors.toneForService(ServiceStatus.IN_PROGRESS))
        assertEquals(StatusTone.Success, StatusColors.toneForService(ServiceStatus.COMPLETED))
    }

    @Test
    fun `legacy single colors map back to their tone`() {
        assertEquals(StatusTone.Danger, StatusColors.toneOf(StatusColors.forMaintenance(MaintenanceStatus.OVERDUE)))
        assertEquals(StatusTone.Info, StatusColors.toneOf(StatusColors.forService(ServiceStatus.IN_PROGRESS)))
        assertNull(StatusColors.toneOf(EficazColors.Orange))
    }
}
