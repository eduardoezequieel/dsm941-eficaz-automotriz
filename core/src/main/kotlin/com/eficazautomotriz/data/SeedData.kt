package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.MaintenanceRecord
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.model.enums.UserRole
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Demonstration data so the system can be walked through without typing everything by hand.
 *
 * WARNING: these are invented example data for the course. They do not represent
 * real clients, vehicles, schedules, or prices of Eficaz Automotriz.
 */
object SeedData {

    /** Loads the full catalog. `today` is passed in so the demo stays reproducible. */
    fun load(repositories: Repositories, today: LocalDate) {
        loadUsers(repositories)
        loadCatalogs(repositories.serviceTypes, repositories.maintenanceTypes, repositories.timeSlots)
        loadVehicles(repositories, today)
        loadMaintenanceRecords(repositories)
        loadAppointments(repositories, today)
        loadServiceOrders(repositories, today)
    }

    /**
     * Only the three catalogs (service types, maintenance types, and slots). It uses
     * fixed identifiers and `save` overwrites by id, so calling it again does not
     * duplicate anything: the app invokes it at startup against an empty database.
     */
    fun loadCatalogs(
        serviceTypes: ServiceTypeRepository,
        maintenanceTypes: MaintenanceTypeRepository,
        timeSlots: TimeSlotRepository,
    ) {
        loadServiceTypes(serviceTypes)
        loadMaintenanceTypes(maintenanceTypes)
        loadTimeSlots(timeSlots)
    }

    private fun loadUsers(repositories: Repositories) {
        listOf(
            User("USR-001", "Ana Beatriz Melara", "ana.melara@ejemplo.com", "7012-3456", UserRole.CLIENT),
            User("USR-002", "Carlos Ernesto Rivera", "carlos.rivera@ejemplo.com", "7845-9021", UserRole.CLIENT),
            User("USR-003", "Jose Antonio Mendoza", "jose.mendoza@ejemplo.com", "2251-8890", UserRole.STAFF),
        ).forEach(repositories.users::save)
    }

    private fun loadServiceTypes(serviceTypes: ServiceTypeRepository) {
        listOf(
            ServiceType("SVT-001", "Mantenimiento preventivo", "Revision programada segun kilometraje.", true),
            ServiceType("SVT-002", "Sistema de frenos", "Pastillas, discos, liquido y purga.", true),
            ServiceType("SVT-003", "Sistema electrico", "Bateria, alternador, luces y cableado.", true),
            ServiceType("SVT-004", "Diagnostico por escaner", "Lectura de codigos de la computadora.", true),
            ServiceType("SVT-005", "Reparacion de motor", "Diagnostico y reparacion mayor de motor.", true),
            ServiceType("SVT-006", "Aire acondicionado", "Carga de gas, filtros y compresor.", true),
        ).forEach(serviceTypes::save)
    }

    private fun loadMaintenanceTypes(maintenanceTypes: MaintenanceTypeRepository) {
        listOf(
            MaintenanceType("MNT-001", "Cambio de aceite y filtro", 5_000, true),
            MaintenanceType("MNT-002", "Rotacion de neumaticos", 10_000, true),
            MaintenanceType("MNT-003", "Revision de frenos", 15_000, true),
            MaintenanceType("MNT-004", "Cambio de filtro de aire", 20_000, true),
        ).forEach(maintenanceTypes::save)
    }

    /**
     * Declared schedule: Monday to Friday from 9:00 to 12:00 and from 13:00 to 17:00 in
     * one-hour slots; Saturday from 9:00 to 13:00 with no break. Sunday with no service.
     */
    private fun loadTimeSlots(timeSlots: TimeSlotRepository) {
        val weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        val hourlyStarts = listOf(9, 10, 11, 13, 14, 15, 16)

        val weekdaySlots = weekdays.flatMap { day ->
            hourlyStarts.map { hour -> Triple(day, LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0)) }
        }
        // Fixed ids by position (SLT-001 through SLT-035): the seed appointments reference them.
        weekdaySlots.forEachIndexed { index, (day, start, end) ->
            timeSlots.save(TimeSlot(slotId(index + 1), day, start, end, enabled = true, capacity = 1))
        }

        timeSlots.save(
            TimeSlot(
                id = slotId(weekdaySlots.size + 1),
                dayOfWeek = DayOfWeek.SATURDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                enabled = true,
                capacity = 2,
            )
        )
    }

    private fun slotId(number: Int): String = "SLT-" + number.toString().padStart(3, '0')

    /**
     * The mileages are chosen so that, with the default warning threshold,
     * the first three vehicles land in a different maintenance state.
     */
    private fun loadVehicles(repositories: Repositories, today: LocalDate) {
        listOf(
            Vehicle("VEH-001", "USR-001", "P123456", "Toyota", "Corolla", 2019, 30_000, today.minusDays(12)),
            Vehicle("VEH-002", "USR-001", "P654321", "Nissan", "Frontier", 2017, 62_700, today.minusDays(5)),
            Vehicle("VEH-003", "USR-001", "P998877", "Hyundai", "Accent", 2015, 96_500, today.minusDays(2)),
            Vehicle("VEH-004", "USR-002", "P112233", "Kia", "Rio", 2022, 18_400, today.minusDays(20)),
        ).forEach(repositories.vehicles::save)
    }

    private fun loadMaintenanceRecords(repositories: Repositories) {
        listOf(
            // VEH-001: up to date. Recommended 33 000 against a current 30 000.
            MaintenanceRecord("MRC-001", "VEH-001", "MNT-001", 28_000),
            MaintenanceRecord("MRC-002", "VEH-001", "MNT-002", 25_000),
            // VEH-002: due soon. Recommended 63 000 against a current 62 700.
            MaintenanceRecord("MRC-003", "VEH-002", "MNT-001", 58_000),
            MaintenanceRecord("MRC-004", "VEH-002", "MNT-003", 50_000),
            // VEH-003: overdue. Recommended 95 000 against a current 96 500.
            MaintenanceRecord("MRC-005", "VEH-003", "MNT-001", 90_000),
            MaintenanceRecord("MRC-006", "VEH-003", "MNT-004", 85_000),
            // VEH-004 has no records: it shows the "no previous record" state.
        ).forEach(repositories.maintenanceRecords::save)
    }

    private fun loadAppointments(repositories: Repositories, today: LocalDate) {
        val slots = repositories.timeSlots.findAll().associateBy { it.id }

        val entries = listOf(
            SeedAppointment("APT-001", "USR-001", "VEH-001", "SVT-001", "SLT-001", -14, AppointmentStatus.ATTENDED, null),
            SeedAppointment("APT-002", "USR-001", "VEH-002", "SVT-002", "SLT-009", -7, AppointmentStatus.ATTENDED, null),
            SeedAppointment("APT-003", "USR-002", "VEH-004", "SVT-004", "SLT-016", -6, AppointmentStatus.REJECTED, "El taller no contaba con el escaner del modelo ese dia."),
            SeedAppointment("APT-004", "USR-001", "VEH-003", "SVT-005", "SLT-023", -3, AppointmentStatus.CANCELLED, null),
            SeedAppointment("APT-005", "USR-001", "VEH-003", "SVT-005", "SLT-002", 3, AppointmentStatus.CONFIRMED, null),
            SeedAppointment("APT-006", "USR-002", "VEH-004", "SVT-006", "SLT-010", 4, AppointmentStatus.PENDING, null),
            SeedAppointment("APT-007", "USR-001", "VEH-001", "SVT-003", "SLT-017", 5, AppointmentStatus.PENDING, null),
            SeedAppointment("APT-008", "USR-001", "VEH-002", "SVT-002", "SLT-036", 6, AppointmentStatus.RESCHEDULED, "Se movio al sabado a peticion del cliente."),
        )

        entries.forEach { entry ->
            val slot = slots[entry.slotId] ?: return@forEach
            val date = alignToSlotDay(today.plusDays(entry.dayOffset.toLong()), slot.dayOfWeek, entry.dayOffset)
            repositories.appointments.save(
                Appointment(
                    id = entry.id,
                    clientId = entry.clientId,
                    vehicleId = entry.vehicleId,
                    serviceTypeId = entry.serviceTypeId,
                    date = date,
                    slotId = entry.slotId,
                    status = entry.status,
                    reason = entry.reason,
                    requestedAt = date.minusDays(2).atTime(9, 30),
                )
            )
        }
    }

    private fun loadServiceOrders(repositories: Repositories, today: LocalDate) {
        listOf(
            ServiceOrder(
                id = "SRV-001",
                vehicleId = "VEH-001",
                appointmentId = "APT-001",
                serviceTypeId = "SVT-001",
                mileageAtService = 28_000,
                status = ServiceStatus.COMPLETED,
                notes = "Cambio de aceite y filtro. Revision general sin hallazgos.",
                receivedAt = today.minusDays(14).atTime(9, 15),
                startedAt = today.minusDays(14).atTime(10, 0),
                closedAt = today.minusDays(14).atTime(11, 40),
            ),
            ServiceOrder(
                id = "SRV-002",
                vehicleId = "VEH-002",
                appointmentId = "APT-002",
                serviceTypeId = "SVT-002",
                mileageAtService = 58_000,
                status = ServiceStatus.COMPLETED,
                notes = "Cambio de pastillas delanteras y rectificado de discos.",
                receivedAt = today.minusDays(7).atTime(13, 5),
                startedAt = today.minusDays(7).atTime(14, 0),
                closedAt = today.minusDays(7).atTime(16, 20),
            ),
            ServiceOrder(
                id = "SRV-003",
                vehicleId = "VEH-003",
                appointmentId = null,
                serviceTypeId = "SVT-005",
                mileageAtService = null,
                status = ServiceStatus.IN_PROGRESS,
                notes = "Ingreso sin cita por ruido en el motor. En diagnostico.",
                receivedAt = today.minusDays(1).atTime(8, 50),
                startedAt = today.minusDays(1).atTime(9, 30),
                closedAt = null,
            ),
            ServiceOrder(
                id = "SRV-004",
                vehicleId = "VEH-004",
                appointmentId = null,
                serviceTypeId = "SVT-006",
                mileageAtService = null,
                status = ServiceStatus.RECEIVED,
                notes = "Aire acondicionado sin enfriar. Pendiente de revision.",
                receivedAt = today.atTime(9, 0),
                startedAt = null,
                closedAt = null,
            ),
        ).forEach(repositories.serviceOrders::save)

        listOf(
            Evidence("EVD-001", "SRV-001", EvidenceStage.RECEPTION, "https://ejemplo.local/evidencias/srv001-recepcion.jpg", today.minusDays(14).atTime(9, 20)),
            Evidence("EVD-002", "SRV-001", EvidenceStage.RESULT, "https://ejemplo.local/evidencias/srv001-resultado.jpg", today.minusDays(14).atTime(11, 35)),
            Evidence("EVD-003", "SRV-002", EvidenceStage.DIAGNOSIS, "https://ejemplo.local/evidencias/srv002-diagnostico.jpg", today.minusDays(7).atTime(14, 10)),
            Evidence("EVD-004", "SRV-003", EvidenceStage.RECEPTION, "https://ejemplo.local/evidencias/srv003-recepcion.jpg", today.minusDays(1).atTime(9, 0)),
        ).forEach(repositories.evidences::save)
    }

    /** Aligns the date to the slot's day of the week, without crossing from the past into the future. */
    private fun alignToSlotDay(candidate: LocalDate, dayOfWeek: DayOfWeek, dayOffset: Int): LocalDate {
        val difference = dayOfWeek.value - candidate.dayOfWeek.value
        return if (dayOffset < 0) candidate.minusDays(((difference + 7) % 7).toLong())
        else candidate.plusDays(((difference + 7) % 7).toLong())
    }

    private data class SeedAppointment(
        val id: String,
        val clientId: String,
        val vehicleId: String,
        val serviceTypeId: String,
        val slotId: String,
        val dayOffset: Int,
        val status: AppointmentStatus,
        val reason: String?,
    )
}
