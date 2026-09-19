package com.eficazautomotriz.cli.format

import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.rules.MaintenanceEvaluation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Etiquetas y formatos que ve el usuario. Todo el texto de pantalla se decide aqui. */
object Labels {

    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    private val DAY_NAMES: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "Lunes",
        DayOfWeek.TUESDAY to "Martes",
        DayOfWeek.WEDNESDAY to "Miercoles",
        DayOfWeek.THURSDAY to "Jueves",
        DayOfWeek.FRIDAY to "Viernes",
        DayOfWeek.SATURDAY to "Sabado",
        DayOfWeek.SUNDAY to "Domingo",
    )

    fun date(value: LocalDate): String = value.format(DATE)

    fun time(value: LocalTime): String = value.format(TIME)

    fun dateTime(value: LocalDateTime): String = value.format(DATE_TIME)

    fun dateTimeOrDash(value: LocalDateTime?): String = value?.let { dateTime(it) } ?: "-"

    fun dayOfWeek(value: DayOfWeek): String = DAY_NAMES[value] ?: value.name

    fun slotRange(slot: TimeSlot): String = "${time(slot.startTime)}-${time(slot.endTime)}"

    fun status(value: AppointmentStatus): String = "[${value.label}]"

    fun status(value: ServiceStatus): String = "[${value.label}]"

    fun status(value: MaintenanceStatus): String = "[${value.label}]"

    fun stage(value: EvidenceStage): String = value.label

    fun mileage(value: Int): String = "%,d km".format(value)

    fun mileageOrDash(value: Int?): String = value?.let { mileage(it) } ?: "-"

    fun percentage(value: Double): String = "%.1f %%".format(value)

    /** Explica en una linea que significa la diferencia calculada. */
    fun difference(evaluation: MaintenanceEvaluation): String = when (val difference = evaluation.differenceKm) {
        null -> "sin base de calculo"
        in 0..Int.MAX_VALUE -> "faltan ${mileage(difference)}"
        else -> "excedido por ${mileage(-difference)}"
    }
}
