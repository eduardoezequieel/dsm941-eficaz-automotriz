package com.eficazautomotriz.app.demo

import com.eficazautomotriz.app.data.auth.InMemoryAuthGateway
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.data.SeedData
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Demo data that mirrors the Etapa 1 mockups (f2–f9), so screenshots can be compared with
 * them. Invented data for the course: no real clients or vehicles.
 *
 * The mockups are dated August 2026; here every date is relative to `today` so upcoming
 * appointments stay upcoming and the report presets (week/month/quarter) have data.
 * Maintenance follows f2/f3: the Frontier oil change is the nearest due-soon line
 * (recommended at 85,000 km, 900 km left) and the Accent is overdue. f4's PNG draws that
 * oil change as overdue at 84,000 km; the home alert and the vehicle list both require
 * "Próximo", so the detail uses the same 85,000 km figure.
 */
object MockupSeedData {

    const val CLIENT_ID = "USR-001"
    const val STAFF_ID = "USR-002"
    const val DEMO_PASSWORD = "Demo1234"

    /**
     * Demo warning window. The Frontier oil change is 900 km out (85,000 − 84,100), so the
     * default 500 km window would leave it "Vigente" and the home alert would name the tire
     * rotation instead. [com.eficazautomotriz.app.di.AppGraph] applies this when a demo
     * backend is passed. Production stays at 500.
     */
    const val DEMO_WARNING_THRESHOLD_KM = 1_000
    const val CLIENT_EMAIL = "cliente@demo.test"
    const val STAFF_EMAIL = "personal@demo.test"

    const val COROLLA = "VEH-001"
    const val FRONTIER = "VEH-002"
    const val ACCENT = "VEH-003"

    /** Demo accounts for [InMemoryAuthGateway]: Diego (client) and Carlos (staff). */
    val accounts: Map<String, InMemoryAuthGateway.Account> = mapOf(
        CLIENT_EMAIL to InMemoryAuthGateway.Account(DEMO_PASSWORD, CLIENT_ID),
        STAFF_EMAIL to InMemoryAuthGateway.Account(DEMO_PASSWORD, STAFF_ID),
    )

    // Service types (accented names, as shown in the mockups).
    private const val PREVENTIVE = "SVT-001"
    private const val BRAKES = "SVT-002"
    private const val ELECTRICAL = "SVT-003"
    private const val SCANNER = "SVT-004"
    private const val ENGINE = "SVT-005"
    private const val AIR_CONDITIONING = "SVT-006"
    private const val OIL_CHANGE_SERVICE = "SVT-007"
    private const val TIRE_ROTATION_SERVICE = "SVT-008"

    // Maintenance types with the f4 intervals.
    private const val OIL = "MNT-001"
    private const val ROTATION = "MNT-002"
    private const val BRAKE_CHECK = "MNT-003"
    private const val AIR_FILTER = "MNT-004"

    fun load(repositories: Repositories, today: LocalDate) {
        // Time slots come from the core seed (Mon–Fri 9–12 and 13–17, Saturday morning);
        // service and maintenance types are then overwritten (same ids) with the mockup ones.
        SeedData.loadCatalogs(repositories.serviceTypes, repositories.maintenanceTypes, repositories.timeSlots)
        // f6 draws six pills (9:00–11:00 and 1:00–3:00). The core catalog also has 16:00–17:00.
        repositories.timeSlots.findAll()
            .filter { it.startTime.hour >= 16 }
            .forEach { repositories.timeSlots.deleteById(it.id) }
        loadServiceTypes(repositories)
        loadMaintenanceTypes(repositories)
        loadUsers(repositories)
        loadVehicles(repositories, today)
        loadMaintenanceRecords(repositories)
        val slots = repositories.timeSlots.findAll()
        loadClientStory(repositories, today, slots)
        loadWorkshopDay(repositories, today, slots)
        loadMonthActivity(repositories, today, slots)
    }

    private fun loadServiceTypes(repositories: Repositories) {
        listOf(
            ServiceType(PREVENTIVE, "Mantenimiento preventivo", "Revisión programada según kilometraje.", true),
            ServiceType(BRAKES, "Sistema de frenos", "Pastillas, discos, líquido y purga.", true),
            ServiceType(ELECTRICAL, "Sistema eléctrico", "Batería, alternador, luces y cableado.", true),
            ServiceType(SCANNER, "Diagnóstico por escáner", "Lectura de códigos de la computadora.", true),
            ServiceType(ENGINE, "Reparación de motor", "Diagnóstico y reparación mayor de motor.", true),
            ServiceType(AIR_CONDITIONING, "Aire acondicionado", "Carga de gas, filtros y compresor.", true),
            // Inactive: only name past orders of the f4 history ("Servicios recientes de este vehículo").
            ServiceType(OIL_CHANGE_SERVICE, "Cambio de aceite y filtro", "Servicio de mantenimiento.", false),
            ServiceType(TIRE_ROTATION_SERVICE, "Rotación de neumáticos", "Servicio de mantenimiento.", false),
        ).forEach(repositories.serviceTypes::save)
    }

    private fun loadMaintenanceTypes(repositories: Repositories) {
        listOf(
            MaintenanceType(OIL, "Cambio de aceite y filtro", 5_000, true),
            MaintenanceType(ROTATION, "Rotación de neumáticos", 10_000, true),
            MaintenanceType(BRAKE_CHECK, "Revisión de frenos", 20_000, true),
            MaintenanceType(AIR_FILTER, "Cambio de filtro de aire", 15_000, true),
        ).forEach(repositories.maintenanceTypes::save)
    }

    private fun loadUsers(repositories: Repositories) {
        listOf(
            User(CLIENT_ID, "Diego Escobar", CLIENT_EMAIL, "7012-3456", UserRole.CLIENT),
            User(STAFF_ID, "Carlos Cruz", STAFF_EMAIL, "2250-1188", UserRole.STAFF),
            User("USR-003", "María Fernanda López", "maria.lopez@ejemplo.com", "7788-1020", UserRole.CLIENT),
            User("USR-004", "José Antonio Pérez", "jose.perez@ejemplo.com", "7655-3302", UserRole.CLIENT),
            User("USR-005", "Ana Sofía Martínez", "ana.martinez@ejemplo.com", "7120-4410", UserRole.CLIENT),
            User("USR-006", "Luis Alberto Hernández", "luis.hernandez@ejemplo.com", "7034-9981", UserRole.CLIENT),
        ).forEach(repositories.users::save)
    }

    /** Plates are stored in the validated format (P123456); the mockups print them as P123-456. */
    private fun loadVehicles(repositories: Repositories, today: LocalDate) {
        listOf(
            Vehicle(COROLLA, CLIENT_ID, "P123456", "Toyota", "Corolla", 2016, 78_400, today.minusDays(30)),
            Vehicle(FRONTIER, CLIENT_ID, "P987321", "Nissan", "Frontier", 2019, 84_100, today.minusDays(5)),
            Vehicle(ACCENT, CLIENT_ID, "P456789", "Hyundai", "Accent", 2013, 142_900, today.minusDays(12)),
            Vehicle("VEH-004", "USR-003", "P210334", "Kia", "Rio", 2021, 35_200, today.minusDays(3)),
            Vehicle("VEH-005", "USR-004", "P778120", "Mazda", "3", 2018, 61_900, today.minusDays(8)),
            Vehicle("VEH-006", "USR-005", "P330981", "Honda", "Civic", 2020, 44_300, today.minusDays(1)),
            Vehicle("VEH-007", "USR-006", "P909455", "Chevrolet", "Aveo", 2015, 118_700, today.minusDays(15)),
            Vehicle("VEH-008", "USR-003", "P615202", "Toyota", "Hilux", 2022, 27_800, today.minusDays(2)),
        ).forEach(repositories.vehicles::save)
    }

    private fun loadMaintenanceRecords(repositories: Repositories) {
        val baselines = mapOf(
            // Corolla 78,400 km: every line up to date (f3 "Mantenimiento vigente").
            COROLLA to listOf(75_000, 70_000, 70_000, 70_000),
            // Frontier 84,100 km. Oil 80,000/5,000 is due at 85,000 (900 km, "Próximo" inside
            // the demo window) and is the nearest line, so f2 names it. Rotation 76,000/10,000
            // stays up to date (1,900 km left) so it does not outrank the oil change.
            FRONTIER to listOf(80_000, 76_000, 80_000, 79_000),
            // Accent 142,900 km: oil change overdue (f3 "Mantenimiento vencido").
            ACCENT to listOf(135_000, 135_000, 130_000, 130_000),
            "VEH-004" to listOf(33_000, 30_000, 30_000, 30_000),
            "VEH-005" to listOf(60_000, 55_000, 50_000, 55_000),
            "VEH-006" to listOf(42_000, 40_000, 40_000, 40_000),
            "VEH-007" to listOf(115_000, 110_000, 100_000, 110_000),
            "VEH-008" to listOf(25_000, 20_000, 20_000, 20_000),
        )
        var sequence = 1
        baselines.forEach { (vehicleId, mileages) ->
            listOf(OIL, ROTATION, BRAKE_CHECK, AIR_FILTER).zip(mileages).forEach { (typeId, mileage) ->
                repositories.maintenanceRecords.save(MaintenanceRecord(id("MRC", sequence++), vehicleId, typeId, mileage))
            }
        }
    }

    /** Diego's appointments (f2/f5/f8) and service history (f2/f4/f7). */
    private fun loadClientStory(repositories: Repositories, today: LocalDate, slots: List<TimeSlot>) {
        // First Friday after today: the confirmed appointment must be the next one (f2 "PRÓXIMA CITA").
        val friday = today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
        val appointments = repositories.appointments
        fun save(id: String, vehicle: String, service: String, date: LocalDate, hour: Int, status: AppointmentStatus, reason: String? = null) =
            appointments.save(
                Appointment(id, CLIENT_ID, vehicle, service, date, slotFor(slots, date, hour).id, status, reason, date.minusDays(4).atTime(18, 20)),
            )

        save("CT-1042", COROLLA, PREVENTIVE, friday, 10, AppointmentStatus.CONFIRMED)
        save("CT-1051", FRONTIER, SCANNER, friday.plusDays(3), 9, AppointmentStatus.PENDING)
        // f5 draws four cards. This fifth pending request belongs to another client so the
        // staff counter still sees it and Diego's "Próximas" matches the mockup.
        val otherClient = repositories.vehicles.findById("VEH-004")!!
        val pendingDay = friday.plusDays(4)
        appointments.save(
            Appointment(
                "CT-1055", otherClient.ownerId, otherClient.id, AIR_CONDITIONING, pendingDay,
                slotFor(slots, pendingDay, 14).id, AppointmentStatus.PENDING, null,
                pendingDay.minusDays(4).atTime(18, 20),
            ),
        )
        // Wednesday 1:00 p.m. is full, so f6 strikes that pill through; 10:00 stays free.
        val showcase = friday.plusDays(5)
        appointments.save(
            Appointment(
                "CT-1070", otherClient.ownerId, otherClient.id, PREVENTIVE, showcase,
                slotFor(slots, showcase, 13).id, AppointmentStatus.CONFIRMED, null,
                showcase.minusDays(2).atTime(9, 0),
            ),
        )
        val movedTo = friday.plusDays(6)
        val movedFrom = movedTo.minusDays(1)
        save(
            "CT-1039", ACCENT, ELECTRICAL, movedTo, 9, AppointmentStatus.RESCHEDULED,
            "Fecha anterior: $movedFrom 14:00. El taller movió la cita: el técnico de sistema eléctrico no estará disponible en la fecha original.",
        )
        save(
            "CT-1028", ACCENT, AIR_CONDITIONING, weekdayBefore(today, 18), 14, AppointmentStatus.REJECTED,
            "Horario no disponible.",
        )
        val brakesDay = weekdayBefore(today, 91)
        save("CT-1030", COROLLA, BRAKES, brakesDay, 9, AppointmentStatus.ATTENDED)

        // f7: brake service with three photos and the staff notes.
        repositories.serviceOrders.save(
            ServiceOrder(
                id = "SV-2087",
                vehicleId = COROLLA,
                appointmentId = "CT-1030",
                serviceTypeId = BRAKES,
                mileageAtService = 78_400,
                status = ServiceStatus.COMPLETED,
                notes = "Se reemplazaron pastillas delanteras y se rectificaron discos. Se recomienda revisar " +
                    "el sistema en el próximo mantenimiento programado.",
                receivedAt = brakesDay.atTime(9, 12),
                startedAt = brakesDay.atTime(10, 5),
                closedAt = brakesDay.atTime(15, 40),
            ),
        )
        listOf(
            EvidenceStage.RECEPTION to LocalTime.of(9, 15),
            EvidenceStage.DIAGNOSIS to LocalTime.of(10, 20),
            EvidenceStage.RESULT to LocalTime.of(15, 35),
        ).forEachIndexed { index, (stage, time) ->
            repositories.evidences.save(
                Evidence(
                    id("EVD", index + 1), "SV-2087", stage,
                    "https://demo.invalid/evidencias/sv2087-${stage.name.lowercase()}.jpg",
                    brakesDay.atTime(time),
                ),
            )
        }

        // f4 "Servicios recientes de este vehículo" (Nissan) and an older Accent service.
        completedOrder(repositories, "SV-1990", FRONTIER, OIL_CHANGE_SERVICE, 80_000, weekdayBefore(today, 210).atTime(8, 40))
        completedOrder(repositories, "SV-1904", FRONTIER, TIRE_ROTATION_SERVICE, 76_000, weekdayBefore(today, 340).atTime(13, 10))
        completedOrder(repositories, "SV-1850", ACCENT, PREVENTIVE, 135_000, weekdayBefore(today, 400).atTime(9, 30))

        // f8 "SERVICIOS EN PROCESO": engine repair on the Corolla, received today at 8:30.
        repositories.serviceOrders.save(
            ServiceOrder(
                "SV-2104", COROLLA, null, ENGINE, null, ServiceStatus.IN_PROGRESS,
                "Ingresó por ruido en el motor. En diagnóstico.",
                today.atTime(8, 30), today.atTime(9, 0), null,
            ),
        )
    }

    /** Today's workshop agenda (f8 counters: 4 appointments today, 3 orders in progress). */
    private fun loadWorkshopDay(repositories: Repositories, today: LocalDate, slots: List<TimeSlot>) {
        listOf(
            Triple("CT-1060", "VEH-004", PREVENTIVE) to 9,
            Triple("CT-1061", "VEH-005", BRAKES) to 11,
            Triple("CT-1062", "VEH-007", SCANNER) to 14,
            Triple("CT-1063", "VEH-006", AIR_CONDITIONING) to 15,
        ).forEach { (data, hour) ->
            val (id, vehicleId, service) = data
            val owner = repositories.vehicles.findById(vehicleId)!!.ownerId
            repositories.appointments.save(
                Appointment(
                    id, owner, vehicleId, service, today, slotFor(slots, today, hour).id,
                    AppointmentStatus.CONFIRMED, null, today.minusDays(3).atTime(10, 0),
                ),
            )
        }
        repositories.serviceOrders.save(
            ServiceOrder(
                "SV-2105", "VEH-008", null, ELECTRICAL, null, ServiceStatus.IN_PROGRESS,
                "Falla intermitente en luces delanteras.",
                today.minusDays(1).atTime(14, 10), today.minusDays(1).atTime(15, 0), null,
            ),
        )
        repositories.serviceOrders.save(
            ServiceOrder(
                "SV-2106", "VEH-007", null, SCANNER, null, ServiceStatus.IN_PROGRESS,
                "Testigo de motor encendido.",
                today.atTime(7, 50), today.atTime(8, 15), null,
            ),
        )
        repositories.serviceOrders.save(
            ServiceOrder(
                "SV-2107", "VEH-006", null, AIR_CONDITIONING, null, ServiceStatus.RECEIVED,
                "El aire acondicionado no enfría.",
                today.atTime(10, 5), null, null,
            ),
        )
    }

    /**
     * Activity of the current month for the reports (f9): attended appointments with their
     * completed orders (type mix close to 30/24/19/15/12 %), plus rejected and cancelled ones.
     * Dates fall between the first of the month and yesterday so "Mes" and "Trimestre" see them
     * and today's counters (f8) do not.
     */
    private fun loadMonthActivity(repositories: Repositories, today: LocalDate, slots: List<TimeSlot>) {
        val monthStart = today.withDayOfMonth(1)
        val daysAvailable = maxOf(1, today.dayOfMonth - 1)
        fun dateAt(index: Int): LocalDate =
            if (today.dayOfMonth == 1) today else monthStart.plusDays((index % daysAvailable).toLong())

        val fleet = listOf("VEH-004", "VEH-005", "VEH-006", "VEH-007", "VEH-008")
        val serviceMix = List(5) { PREVENTIVE } + List(4) { BRAKES } + List(3) { SCANNER } +
            List(2) { ELECTRICAL } + List(2) { AIR_CONDITIONING }
        val hours = listOf(9, 10, 11, 13, 14, 15, 16)

        serviceMix.forEachIndexed { index, service ->
            val vehicleId = fleet[index % fleet.size]
            val vehicle = repositories.vehicles.findById(vehicleId)!!
            val date = dateAt(index)
            val hour = hours[index % hours.size]
            val appointmentId = "CT-" + (2001 + index)
            repositories.appointments.save(
                Appointment(
                    appointmentId, vehicle.ownerId, vehicleId, service, date, slotFor(slots, date, hour).id,
                    AppointmentStatus.ATTENDED, null, date.minusDays(3).atTime(11, 0),
                ),
            )
            repositories.serviceOrders.save(
                ServiceOrder(
                    "SV-" + (3001 + index), vehicleId, appointmentId, service, vehicle.currentMileage - 200,
                    ServiceStatus.COMPLETED, "Servicio completado sin observaciones.",
                    date.atTime(hour, 5), date.atTime(hour, 30), date.atTime(hour + 1, 45),
                ),
            )
        }
        listOf(
            AppointmentStatus.REJECTED to "Horario no disponible.",
            AppointmentStatus.REJECTED to "Repuesto no disponible esa semana.",
            AppointmentStatus.REJECTED to "El taller no atiende ese servicio el sábado.",
            AppointmentStatus.CANCELLED to null,
        ).forEachIndexed { index, (status, reason) ->
            val vehicleId = fleet[(index + 2) % fleet.size]
            val vehicle = repositories.vehicles.findById(vehicleId)!!
            val date = dateAt(index * 3 + 1)
            repositories.appointments.save(
                Appointment(
                    "CT-" + (2101 + index), vehicle.ownerId, vehicleId, serviceMix[index * 4], date,
                    slotFor(slots, date, 13).id, status, reason, date.minusDays(2).atTime(16, 0),
                ),
            )
        }
    }

    private fun completedOrder(
        repositories: Repositories,
        id: String,
        vehicleId: String,
        serviceTypeId: String,
        mileage: Int,
        receivedAt: LocalDateTime,
    ) {
        repositories.serviceOrders.save(
            ServiceOrder(
                id, vehicleId, null, serviceTypeId, mileage, ServiceStatus.COMPLETED,
                "Servicio completado sin observaciones.",
                receivedAt, receivedAt.plusMinutes(30), receivedAt.plusHours(2),
            ),
        )
    }

    /**
     * Slot of [date]'s weekday starting at [hour]; else the first slot of that weekday; Sundays
     * (no service) borrow a Saturday slot. Only used to label seeded appointments.
     */
    private fun slotFor(slots: List<TimeSlot>, date: LocalDate, hour: Int): TimeSlot {
        val day = if (date.dayOfWeek == DayOfWeek.SUNDAY) DayOfWeek.SATURDAY else date.dayOfWeek
        val ofDay = slots.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
        return ofDay.firstOrNull { it.startTime.hour == hour } ?: ofDay.firstOrNull() ?: slots.first()
    }

    /** A Monday–Friday date about [days] days before [today]. */
    private fun weekdayBefore(today: LocalDate, days: Long): LocalDate {
        var date = today.minusDays(days)
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) date = date.minusDays(1)
        return date
    }

    private fun id(prefix: String, number: Int): String = "$prefix-" + number.toString().padStart(3, '0')
}