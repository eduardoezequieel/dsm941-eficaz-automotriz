package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.domain.model.Vehicle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Reception search: the hyphen in a plate is display-only. */
class ReceptionSearchTest {

    private val today = LocalDate.of(2026, 10, 11)

    private fun vehicle(plate: String, make: String = "Toyota", model: String = "Corolla") = Vehicle(
        id = plate,
        ownerId = "USR-001",
        plate = plate,
        make = make,
        model = model,
        year = 2020,
        currentMileage = 10_000,
        mileageUpdatedAt = today,
    )

    @Test
    fun `a plate matches with or without the display hyphen`() {
        val vehicles = listOf(vehicle("P123456"), vehicle("P987321", "Nissan", "Frontier"))

        assertEquals(listOf("P123456"), filterReceptionVehicles(vehicles, "P123-456").map { it.plate })
        assertEquals(listOf("P123456"), filterReceptionVehicles(vehicles, "P123456").map { it.plate })
        assertEquals(listOf("P123456"), filterReceptionVehicles(vehicles, "p123-456").map { it.plate })
    }

    @Test
    fun `search also matches make and model, and a blank query keeps every vehicle`() {
        val vehicles = listOf(
            vehicle("P123456", "Toyota", "Corolla"),
            vehicle("P987321", "Nissan", "Frontier"),
        )

        assertEquals(2, filterReceptionVehicles(vehicles, "  ").size)
        assertEquals(emptyList<String>(), filterReceptionVehicles(vehicles, "---").map { it.plate })
        assertEquals(listOf("P987321"), filterReceptionVehicles(vehicles, "frontier").map { it.plate })
        assertEquals(listOf("P987321"), filterReceptionVehicles(vehicles, "Nissan Frontier").map { it.plate })
    }
}
