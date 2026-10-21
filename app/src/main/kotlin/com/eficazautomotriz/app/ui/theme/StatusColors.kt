package com.eficazautomotriz.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus

/** Background and text color of a status chip (tinted pill with colored text). */
data class ChipColors(val background: Color, val content: Color)

/** The five chip tones of the mockups. */
enum class StatusTone(val colors: ChipColors) {
    /** Confirmed / attended / in progress: light blue with navy text. */
    Info(ChipColors(EficazColors.InfoBg, EficazColors.InfoText)),
    /** Pending / due soon: light orange with brown-orange text. */
    Warning(ChipColors(EficazColors.WarningBg, EficazColors.WarningText)),
    /** Up to date / completed: light green with green text. */
    Success(ChipColors(EficazColors.SuccessBg, EficazColors.SuccessText)),
    /** Overdue / rejected: light red with red text. */
    Danger(ChipColors(EficazColors.DangerBg, EficazColors.DangerText)),
    /** Rescheduled / cancelled / no data: light gray with slate text. */
    Neutral(ChipColors(EficazColors.NeutralBg, EficazColors.NeutralText)),
}

/**
 * Status → tone table shared by maintenance, appointment and order chips, cards and charts.
 * `toneFor*` is the primary API; `for*` returns the tone's strong (text) color for callers
 * that only need one color (borders, counters, timelines).
 */
object StatusColors {
    val UpToDate: Color = StatusTone.Success.colors.content
    val DueSoon: Color = StatusTone.Warning.colors.content
    val Overdue: Color = StatusTone.Danger.colors.content
    val NoPreviousRecord: Color = StatusTone.Neutral.colors.content

    fun toneForMaintenance(status: MaintenanceStatus): StatusTone = when (status) {
        MaintenanceStatus.UP_TO_DATE -> StatusTone.Success
        MaintenanceStatus.DUE_SOON -> StatusTone.Warning
        MaintenanceStatus.OVERDUE -> StatusTone.Danger
        MaintenanceStatus.NO_PREVIOUS_RECORD -> StatusTone.Neutral
    }

    /** Pending needs attention; confirmed/attended are informative; rescheduled/cancelled are neutral. */
    fun toneForAppointment(status: AppointmentStatus): StatusTone = when (status) {
        AppointmentStatus.PENDING -> StatusTone.Warning
        AppointmentStatus.CONFIRMED, AppointmentStatus.ATTENDED -> StatusTone.Info
        AppointmentStatus.RESCHEDULED, AppointmentStatus.CANCELLED -> StatusTone.Neutral
        AppointmentStatus.REJECTED -> StatusTone.Danger
    }

    fun toneForService(status: ServiceStatus): StatusTone = when (status) {
        ServiceStatus.RECEIVED -> StatusTone.Neutral
        ServiceStatus.IN_PROGRESS -> StatusTone.Info
        ServiceStatus.COMPLETED -> StatusTone.Success
    }

    fun chipForMaintenance(status: MaintenanceStatus): ChipColors = toneForMaintenance(status).colors
    fun chipForAppointment(status: AppointmentStatus): ChipColors = toneForAppointment(status).colors
    fun chipForService(status: ServiceStatus): ChipColors = toneForService(status).colors

    fun forMaintenance(status: MaintenanceStatus): Color = chipForMaintenance(status).content
    fun forAppointment(status: AppointmentStatus): Color = chipForAppointment(status).content
    fun forService(status: ServiceStatus): Color = chipForService(status).content

    /** Tone whose text color is [color], so the legacy `StatusChip(label, color)` gets the mockup pair. */
    fun toneOf(color: Color): StatusTone? = StatusTone.entries.firstOrNull { it.colors.content == color }
}