package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.MaintenanceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MaintenanceStatusRuleTest {

    private val rule = MaintenanceStatusRule()
    private val interval = 5_000
    private val threshold = 500

    @Test
    fun `with no previous record it returns NO_PREVIOUS_RECORD and computes nothing`() {
        val result = rule.evaluate(
            currentMileage = 40_000,
            lastServiceMileage = null,
            intervalKm = interval,
            warningThresholdKm = threshold,
        )

        assertEquals(MaintenanceStatus.NO_PREVIOUS_RECORD, result.status)
        assertNull(result.recommendedMileage)
        assertNull(result.differenceKm)
    }

    @Test
    fun `a baseline mileage of zero is not the same as a missing record`() {
        val result = rule.evaluate(
            currentMileage = 100,
            lastServiceMileage = 0,
            intervalKm = interval,
            warningThresholdKm = threshold,
        )

        assertEquals(MaintenanceStatus.UP_TO_DATE, result.status)
        assertEquals(5_000, result.recommendedMileage)
        assertEquals(4_900, result.differenceKm)
    }

    @Test
    fun `mileage far below the recommended value returns UP_TO_DATE`() {
        val result = rule.evaluate(30_000, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.UP_TO_DATE, result.status)
        assertEquals(33_000, result.recommendedMileage)
        assertEquals(3_000, result.differenceKm)
    }

    @Test
    fun `one kilometer before the warning threshold is still UP_TO_DATE`() {
        // recommended 33 000, threshold 500: 32 499 is still up to date.
        val result = rule.evaluate(32_499, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun `exactly at the warning threshold it returns DUE_SOON`() {
        // recommended 33 000 minus the threshold of 500 = 32 500.
        val result = rule.evaluate(32_500, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.DUE_SOON, result.status)
        assertEquals(500, result.differenceKm)
    }

    @Test
    fun `inside the warning window it returns DUE_SOON`() {
        val result = rule.evaluate(32_800, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.DUE_SOON, result.status)
        assertEquals(200, result.differenceKm)
    }

    @Test
    fun `exactly at the recommended mileage it returns OVERDUE and not DUE_SOON`() {
        val result = rule.evaluate(33_000, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.OVERDUE, result.status)
        assertEquals(0, result.differenceKm)
    }

    @Test
    fun `past the recommended mileage it returns OVERDUE with a negative difference`() {
        val result = rule.evaluate(34_200, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.OVERDUE, result.status)
        assertEquals(-1_200, result.differenceKm)
    }

    @Test
    fun `an interval of zero is a configuration error`() {
        assertFailsWith<IllegalArgumentException> {
            rule.evaluate(10_000, 5_000, intervalKm = 0, warningThresholdKm = threshold)
        }
    }

    @Test
    fun `a negative interval is a configuration error`() {
        assertFailsWith<IllegalArgumentException> {
            rule.evaluate(10_000, 5_000, intervalKm = -100, warningThresholdKm = threshold)
        }
    }

    @Test
    fun `an invalid interval is checked even when there is no previous record`() {
        assertFailsWith<IllegalArgumentException> {
            rule.evaluate(10_000, lastServiceMileage = null, intervalKm = 0, warningThresholdKm = threshold)
        }
    }

    @Test
    fun `a warning threshold of zero means DUE_SOON does not exist`() {
        val justBefore = rule.evaluate(32_999, 28_000, interval, warningThresholdKm = 0)
        val exact = rule.evaluate(33_000, 28_000, interval, warningThresholdKm = 0)

        assertEquals(MaintenanceStatus.UP_TO_DATE, justBefore.status)
        assertEquals(MaintenanceStatus.OVERDUE, exact.status)
    }
}