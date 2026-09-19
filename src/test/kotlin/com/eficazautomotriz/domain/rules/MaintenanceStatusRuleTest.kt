package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.MaintenanceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MaintenanceStatusRuleTest {
    private val rule = MaintenanceStatusRule()

    @Test
    fun `sin mantenimiento previo`() {
        val result = rule.evaluate(
            currentMileage = 10_000,
            lastMaintenanceMileage = null,
            maintenanceInterval = 5_000
        )

        assertEquals(MaintenanceStatus.NO_PREVIOUS_RECORD, result.status)
        assertNull(result.recommendedMileage)
        assertNull(result.mileageDifference)
    }

    @Test
    fun `mantenimiento vencido`() {
        val result = rule.evaluate(
            currentMileage = 15_000,
            lastMaintenanceMileage = 10_000,
            maintenanceInterval = 5_000
        )

        assertEquals(MaintenanceStatus.OVERDUE, result.status)
        assertEquals(15_000, result.recommendedMileage)
        assertEquals(0, result.mileageDifference)
    }

    @Test
    fun `mantenimiento proximo`() {
        val result = rule.evaluate(
            currentMileage = 14_600,
            lastMaintenanceMileage = 10_000,
            maintenanceInterval = 5_000,
            warningThreshold = 500
        )

        assertEquals(MaintenanceStatus.DUE_SOON, result.status)
        assertEquals(15_000, result.recommendedMileage)
        assertEquals(400, result.mileageDifference)
    }

    @Test
    fun `mantenimiento al dia`() {
        val result = rule.evaluate(
            currentMileage = 12_000,
            lastMaintenanceMileage = 10_000,
            maintenanceInterval = 5_000,
            warningThreshold = 500
        )

        assertEquals(MaintenanceStatus.UP_TO_DATE, result.status)
        assertEquals(15_000, result.recommendedMileage)
        assertEquals(3_000, result.mileageDifference)
    }

    @Test
    fun `intervalo invalido`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.evaluate(
                currentMileage = 12_000,
                lastMaintenanceMileage = 10_000,
                maintenanceInterval = 0
            )
        }
    }
}
