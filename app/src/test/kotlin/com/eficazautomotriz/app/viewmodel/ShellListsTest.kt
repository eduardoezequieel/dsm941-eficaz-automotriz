package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.viewmodel.staff.groupStaffServices
import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Pure list logic of the new bottom bar tabs: client "Historial" and staff "Servicios". */
class ShellListsTest {

    private val day = LocalDate.of(2026, 10, 11)

    private fun vehicle(id: String) = Vehicle(id, "USR-001", "P123456", "Toyota", "Corolla", 2016, 78_400, day)

    private fun order(id: String, vehicleId: String, status: ServiceStatus, receivedDaysAgo: Long) = ServiceOrder(
        id, vehicleId, null, "SVT-001", null, status, "", LocalDateTime.of(day, java.time.LocalTime.NOON).minusDays(receivedDaysAgo), null, null,
    )

    private fun row(order: ServiceOrder) = ServiceOrderRow(ServiceOrderDetail(order, emptyList()), "Mantenimiento preventivo")

    @Test
    fun `client history merges every vehicle newest first and keeps the vehicle`() {
        val a = vehicle("VEH-001")
        val b = vehicle("VEH-002")
        val merged = mergeClientHistory(
            listOf(a, b),
            mapOf(
                "VEH-001" to listOf(row(order("SV-1", "VEH-001", ServiceStatus.COMPLETED, 30))),
                "VEH-002" to listOf(
                    row(order("SV-2", "VEH-002", ServiceStatus.COMPLETED, 5)),
                    row(order("SV-3", "VEH-002", ServiceStatus.COMPLETED, 90)),
                ),
            ),
        )
        assertEquals(listOf("SV-2", "SV-1", "SV-3"), merged.map { it.row.detail.order.id })
        assertEquals(listOf("VEH-002", "VEH-001", "VEH-002"), merged.map { it.vehicle.id })
    }

    @Test
    fun `staff services are grouped by status keeping the incoming order`() {
        val rows = listOf(
            StaffOrderRow(order("SV-1", "VEH-001", ServiceStatus.IN_PROGRESS, 0), "A", null),
            StaffOrderRow(order("SV-2", "VEH-001", ServiceStatus.COMPLETED, 1), "B", null),
            StaffOrderRow(order("SV-3", "VEH-001", ServiceStatus.RECEIVED, 2), "C", null),
            StaffOrderRow(order("SV-4", "VEH-001", ServiceStatus.IN_PROGRESS, 3), "D", null),
        )
        val grouped = groupStaffServices(rows)
        assertEquals(listOf("SV-1", "SV-4"), grouped.inProgress.map { it.order.id })
        assertEquals(listOf("SV-3"), grouped.received.map { it.order.id })
        assertEquals(listOf("SV-2"), grouped.completed.map { it.order.id })
    }
}
