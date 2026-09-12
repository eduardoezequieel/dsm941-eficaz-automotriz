package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.MaintenanceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MaintenanceServiceTest {
    @Test
    fun `obtiene tipos de mantenimiento activos`() {
        val oilChange = TestEnvironment.maintenanceType(id = "oil", active = true)
        val inactiveType = TestEnvironment.maintenanceType(id = "inactive", active = false)
        val env = TestEnvironment(
            maintenanceTypes = mutableListOf(oilChange, inactiveType)
        )

        val result = env.maintenanceService.getActiveMaintenanceTypes()

        val types = successValue(result)
        assertEquals(listOf(oilChange), types)
    }

    @Test
    fun `genera reporte de mantenimiento de un vehiculo`() {
        val vehicle = TestEnvironment.vehicle(id = "vehicle-1", currentMileage = 14_600)
        val oilChange = TestEnvironment.maintenanceType(id = "oil", intervalMileage = 5_000)
        val env = TestEnvironment(
            vehicles = mutableListOf(vehicle),
            maintenanceTypes = mutableListOf(oilChange),
            maintenanceMileage = mutableMapOf(("vehicle-1" to "oil") to 10_000)
        )

        val result = env.maintenanceService.generateVehicleReport(vehicleId = "vehicle-1")

        val report = successValue(result)
        assertEquals(vehicle, report.vehicle)
        assertEquals(1, report.lines.size)
        assertEquals(MaintenanceStatus.DUE_SOON, report.lines.first().evaluation.status)
    }

    @Test
    fun `retorna error cuando el vehiculo no existe`() {
        val env = TestEnvironment()

        val result = env.maintenanceService.generateVehicleReport(vehicleId = "missing")

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is DomainError.EntityNotFound)
    }

    @Test
    fun `clasificacion general prioriza OVERDUE`() {
        val vehicle = TestEnvironment.vehicle(id = "vehicle-1", currentMileage = 16_000)
        val oilChange = TestEnvironment.maintenanceType(id = "oil", intervalMileage = 5_000)
        val brakeCheck = TestEnvironment.maintenanceType(id = "brakes", intervalMileage = 5_000)
        val env = TestEnvironment(
            vehicles = mutableListOf(vehicle),
            maintenanceTypes = mutableListOf(oilChange, brakeCheck),
            maintenanceMileage = mutableMapOf(
                ("vehicle-1" to "oil") to 10_000,
                ("vehicle-1" to "brakes") to 15_000
            )
        )

        val report = successValue(env.maintenanceService.generateVehicleReport("vehicle-1"))

        assertEquals(MaintenanceStatus.OVERDUE, report.overallStatus)
    }

    @Test
    fun `registra o actualiza kilometraje base`() {
        val vehicle = TestEnvironment.vehicle(id = "vehicle-1", currentMileage = 12_000)
        val oilChange = TestEnvironment.maintenanceType(id = "oil")
        val env = TestEnvironment(
            vehicles = mutableListOf(vehicle),
            maintenanceTypes = mutableListOf(oilChange)
        )

        val result = env.maintenanceService.saveBaseMaintenanceMileage(
            vehicleId = "vehicle-1",
            maintenanceTypeId = "oil",
            mileage = 11_500
        )

        assertTrue(result is Outcome.Success)
        assertEquals(11_500, env.maintenanceMileage["vehicle-1" to "oil"])
    }

    @Test
    fun `cuenta vehiculos por estado de mantenimiento`() {
        val oilChange = TestEnvironment.maintenanceType(id = "oil", intervalMileage = 5_000)
        val env = TestEnvironment(
            vehicles = mutableListOf(
                TestEnvironment.vehicle(id = "overdue", currentMileage = 16_000),
                TestEnvironment.vehicle(id = "up-to-date", currentMileage = 12_000),
                TestEnvironment.vehicle(id = "no-record", currentMileage = 9_000)
            ),
            maintenanceTypes = mutableListOf(oilChange),
            maintenanceMileage = mutableMapOf(
                ("overdue" to "oil") to 10_000,
                ("up-to-date" to "oil") to 10_000
            )
        )

        val counts = successValue(env.maintenanceService.countVehiclesByMaintenanceStatus())

        assertEquals(1, counts[MaintenanceStatus.OVERDUE])
        assertEquals(1, counts[MaintenanceStatus.UP_TO_DATE])
        assertEquals(1, counts[MaintenanceStatus.NO_PREVIOUS_RECORD])
    }

    private fun <T> successValue(outcome: Outcome<T>): T {
        assertTrue(outcome is Outcome.Success)
        return (outcome as Outcome.Success<T>).value
    }
}
