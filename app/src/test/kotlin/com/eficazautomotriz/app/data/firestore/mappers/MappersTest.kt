package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.MalformedDocumentException
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Document ↔ model round trips without Firestore: `fromMap(toMap(e)) == e` for the 9
 * entities, plus the details where Firestore's map differs from the JVM one
 * (integers as Long, explicit nulls).
 */
class MappersTest {

    private val requestedAt = LocalDateTime.of(2026, 10, 18, 10, 30, 0)

    // --- Round trip per entity ---

    @Test
    fun `user round trip keeps the role`() {
        val user = User("uid-abc123", "Ana Melara", "ana@ejemplo.com", "7012-3456", UserRole.STAFF)
        assertEquals(user, UserMapper.fromMap(UserMapper.toMap(user)))
        assertEquals("STAFF", UserMapper.toMap(user)["role"])
    }

    @Test
    fun `vehicle round trip with an ISO date`() {
        val vehicle = Vehicle("VEH-001", "uid-abc123", "P123456", "Toyota", "Corolla", 2019, 30_000, LocalDate.of(2026, 10, 18))
        val map = VehicleMapper.toMap(vehicle)
        assertEquals("2026-10-18", map["mileageUpdatedAt"])
        assertEquals(vehicle, VehicleMapper.fromMap(map))
    }

    @Test
    fun `appointment round trip with a null reason`() {
        val appointment = Appointment(
            "APT-001", "uid-abc123", "VEH-001", "SVT-002", LocalDate.of(2026, 10, 20), "SLT-009",
            AppointmentStatus.PENDING, null, requestedAt,
        )
        val map = AppointmentMapper.toMap(appointment)
        assertTrue(map.containsKey("reason"))
        assertNull(map["reason"])
        assertEquals("PENDING", map["status"])
        assertEquals("2026-10-20", map["date"])
        assertEquals(appointment, AppointmentMapper.fromMap(map))
    }

    @Test
    fun `appointment round trip with a reason and a final status`() {
        val appointment = Appointment(
            "APT-002", "uid-abc123", "VEH-001", "SVT-002", LocalDate.of(2026, 10, 20), "SLT-009",
            AppointmentStatus.REJECTED, "Sin repuestos", requestedAt.withNano(123_000_000),
        )
        assertEquals(appointment, AppointmentMapper.fromMap(AppointmentMapper.toMap(appointment)))
    }

    @Test
    fun `open order stores appointment, mileage and closing as explicit nulls`() {
        val order = ServiceOrder(
            "SRV-003", "VEH-003", null, "SVT-005", null, ServiceStatus.IN_PROGRESS,
            "Ruido en el motor", requestedAt, requestedAt.plusMinutes(40), null,
        )
        val map = ServiceOrderMapper.toMap(order)
        listOf("appointmentId", "mileageAtService", "closedAt").forEach { field ->
            assertTrue(map.containsKey(field), "falta $field")
            assertNull(map[field])
        }
        assertEquals(requestedAt.plusMinutes(40).toString(), map["startedAt"])
        assertEquals(order, ServiceOrderMapper.fromMap(map))
        assertNull(ServiceOrderMapper.fromMap(map.toMutableMap().apply { remove("startedAt") }).startedAt)
    }

    @Test
    fun `closed order round trip with every field`() {
        val order = ServiceOrder(
            "SRV-001", "VEH-001", "APT-001", "SVT-001", 28_000, ServiceStatus.COMPLETED,
            "Cambio de aceite", requestedAt, requestedAt.plusHours(1), requestedAt.plusHours(2),
        )
        val map = ServiceOrderMapper.toMap(order)
        assertEquals(requestedAt.plusHours(1).toString(), map["startedAt"])
        assertEquals(order, ServiceOrderMapper.fromMap(map))
    }

    @Test
    fun `evidence round trip keeps the stage`() {
        val evidence = Evidence("EVD-001", "SRV-001", EvidenceStage.DIAGNOSIS, "https://ejemplo.local/f.jpg", requestedAt)
        assertEquals("DIAGNOSIS", EvidenceMapper.toMap(evidence)["stage"])
        assertEquals(evidence, EvidenceMapper.fromMap(EvidenceMapper.toMap(evidence)))
    }

    @Test
    fun `maintenance record without previous mileage tells null from zero`() {
        val none = MaintenanceRecord("MRC-001", "VEH-001", "MNT-001", null)
        val zero = MaintenanceRecord("MRC-002", "VEH-001", "MNT-002", 0)
        assertTrue(MaintenanceRecordMapper.toMap(none).containsKey("lastServiceMileage"))
        assertEquals(none, MaintenanceRecordMapper.fromMap(MaintenanceRecordMapper.toMap(none)))
        assertEquals(zero, MaintenanceRecordMapper.fromMap(MaintenanceRecordMapper.toMap(zero)))
    }

    @Test
    fun `service type round trip`() {
        val type = ServiceType("SVT-001", "Mantenimiento preventivo", "Revision programada.", false)
        assertEquals(type, ServiceTypeMapper.fromMap(ServiceTypeMapper.toMap(type)))
    }

    @Test
    fun `maintenance type round trip`() {
        val type = MaintenanceType("MNT-001", "Cambio de aceite y filtro", 5_000, true)
        assertEquals(type, MaintenanceTypeMapper.fromMap(MaintenanceTypeMapper.toMap(type)))
    }

    @Test
    fun `time slot round trip with readable day and hours`() {
        val slot = TimeSlot("SLT-036", DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), true, 2)
        val map = TimeSlotMapper.toMap(slot)
        assertEquals("SATURDAY", map["dayOfWeek"])
        assertEquals("09:00", map["startTime"])
        assertEquals(slot, TimeSlotMapper.fromMap(map))
    }

    // --- What Firestore actually returns ---

    @Test
    fun `integers returned by Firestore as Long are read as Int`() {
        val vehicle = VehicleMapper.fromMap(
            mapOf(
                "id" to "VEH-009", "ownerId" to "uid-1", "plate" to "P111222", "make" to "Kia",
                "model" to "Rio", "year" to 2022L, "currentMileage" to 18_400L, "mileageUpdatedAt" to "2026-10-01",
            )
        )
        assertEquals(2022, vehicle.year)
        assertEquals(18_400, vehicle.currentMileage)

        val slot = TimeSlotMapper.fromMap(
            mapOf(
                "id" to "SLT-001", "dayOfWeek" to "MONDAY", "startTime" to "09:00", "endTime" to "10:00",
                "enabled" to true, "capacity" to 1L,
            )
        )
        assertEquals(1, slot.capacity)
        assertEquals(5_000, MaintenanceTypeMapper.fromMap(
            mapOf("id" to "MNT-001", "name" to "Aceite", "intervalKm" to 5_000L, "active" to true)
        ).intervalKm)
        assertEquals(28_000, MaintenanceRecordMapper.fromMap(
            mapOf("id" to "MRC-001", "vehicleId" to "VEH-001", "maintenanceTypeId" to "MNT-001", "lastServiceMileage" to 28_000L)
        ).lastServiceMileage)
    }

    @Test
    fun `a document missing a mandatory field or with an unknown enum fails with a clear message`() {
        val missing = assertThrows(MalformedDocumentException::class.java) {
            UserMapper.fromMap(mapOf("id" to "uid-1", "name" to "Ana", "email" to "a@e.com", "role" to "CLIENT"))
        }
        assertTrue(missing.message!!.contains("phone"))

        val unknownRole = assertThrows(MalformedDocumentException::class.java) {
            UserMapper.fromMap(mapOf("id" to "uid-1", "name" to "Ana", "email" to "a@e.com", "phone" to "7", "role" to "ADMIN"))
        }
        assertTrue(unknownRole.message!!.contains("ADMIN"))
    }
}