package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.ServiceStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServiceTransitionRuleTest {
    private val rule = ServiceTransitionRule()

    @Test
    fun `permite RECEIVED a IN_PROGRESS`() {
        val result = rule.canTransition(
            currentStatus = ServiceStatus.RECEIVED,
            nextStatus = ServiceStatus.IN_PROGRESS
        )

        assertAllowed(result)
    }

    @Test
    fun `permite IN_PROGRESS a COMPLETED`() {
        val result = rule.canTransition(
            currentStatus = ServiceStatus.IN_PROGRESS,
            nextStatus = ServiceStatus.COMPLETED
        )

        assertAllowed(result)
    }

    @Test
    fun `salto directo no permitido`() {
        val result = rule.canTransition(
            currentStatus = ServiceStatus.RECEIVED,
            nextStatus = ServiceStatus.COMPLETED
        )

        assertDenied(result)
    }

    @Test
    fun `retroceso no permitido`() {
        val result = rule.canTransition(
            currentStatus = ServiceStatus.IN_PROGRESS,
            nextStatus = ServiceStatus.RECEIVED
        )

        assertDenied(result)
    }

    @Test
    fun `mismo estado no permitido`() {
        val result = rule.canTransition(
            currentStatus = ServiceStatus.RECEIVED,
            nextStatus = ServiceStatus.RECEIVED
        )

        assertDenied(result)
    }

    @Test
    fun `cambios despues de COMPLETED`() {
        val result = rule.canTransition(
            currentStatus = ServiceStatus.COMPLETED,
            nextStatus = ServiceStatus.IN_PROGRESS
        )

        assertDenied(result)
    }

    @Test
    fun `cierre sin kilometraje`() {
        val result = rule.validateClosure(
            closingMileage = null,
            previousKnownMileage = 12_000
        )

        assertDenied(result)
    }

    @Test
    fun `kilometraje menor al anterior`() {
        val result = rule.validateClosure(
            closingMileage = 11_500,
            previousKnownMileage = 12_000
        )

        assertDenied(result)
    }

    private fun assertAllowed(result: TransitionResult) {
        assertTrue(result is TransitionResult.Allowed)
    }

    private fun assertDenied(result: TransitionResult) {
        assertTrue(result is TransitionResult.Denied)
    }
}
