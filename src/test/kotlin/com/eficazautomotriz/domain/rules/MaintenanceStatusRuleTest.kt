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
    fun `sin registro previo devuelve NO_PREVIOUS_RECORD y no calcula nada`() {
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
    fun `kilometraje base cero no es lo mismo que ausencia de registro`() {
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
    fun `kilometraje muy por debajo del recomendado devuelve UP_TO_DATE`() {
        val result = rule.evaluate(30_000, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.UP_TO_DATE, result.status)
        assertEquals(33_000, result.recommendedMileage)
        assertEquals(3_000, result.differenceKm)
    }

    @Test
    fun `un kilometro antes del umbral de aviso sigue siendo UP_TO_DATE`() {
        // recomendado 33 000, umbral 500: 32 499 aun esta al dia.
        val result = rule.evaluate(32_499, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun `exactamente en el umbral de aviso devuelve DUE_SOON`() {
        // recomendado 33 000 menos umbral 500 = 32 500.
        val result = rule.evaluate(32_500, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.DUE_SOON, result.status)
        assertEquals(500, result.differenceKm)
    }

    @Test
    fun `dentro de la ventana de aviso devuelve DUE_SOON`() {
        val result = rule.evaluate(32_800, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.DUE_SOON, result.status)
        assertEquals(200, result.differenceKm)
    }

    @Test
    fun `exactamente en el kilometraje recomendado devuelve OVERDUE y no DUE_SOON`() {
        val result = rule.evaluate(33_000, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.OVERDUE, result.status)
        assertEquals(0, result.differenceKm)
    }

    @Test
    fun `pasado el recomendado devuelve OVERDUE con diferencia negativa`() {
        val result = rule.evaluate(34_200, 28_000, interval, threshold)

        assertEquals(MaintenanceStatus.OVERDUE, result.status)
        assertEquals(-1_200, result.differenceKm)
    }

    @Test
    fun `un intervalo de cero es un error de configuracion`() {
        assertFailsWith<IllegalArgumentException> {
            rule.evaluate(10_000, 5_000, intervalKm = 0, warningThresholdKm = threshold)
        }
    }

    @Test
    fun `un intervalo negativo es un error de configuracion`() {
        assertFailsWith<IllegalArgumentException> {
            rule.evaluate(10_000, 5_000, intervalKm = -100, warningThresholdKm = threshold)
        }
    }

    @Test
    fun `el intervalo invalido se valida incluso sin registro previo`() {
        assertFailsWith<IllegalArgumentException> {
            rule.evaluate(10_000, lastServiceMileage = null, intervalKm = 0, warningThresholdKm = threshold)
        }
    }

    @Test
    fun `un umbral de aviso de cero hace que DUE_SOON no exista`() {
        val justBefore = rule.evaluate(32_999, 28_000, interval, warningThresholdKm = 0)
        val exact = rule.evaluate(33_000, 28_000, interval, warningThresholdKm = 0)

        assertEquals(MaintenanceStatus.UP_TO_DATE, justBefore.status)
        assertEquals(MaintenanceStatus.OVERDUE, exact.status)
    }
}
