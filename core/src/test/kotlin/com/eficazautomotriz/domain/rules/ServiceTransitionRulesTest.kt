package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ServiceTransitionRuleTest {

    private val rule = ServiceTransitionRule()

    @Test
    fun `from received to in progress is allowed`() {
        val result = rule.canTransition(ServiceStatus.RECEIVED, ServiceStatus.IN_PROGRESS)

        assertEquals(TransitionResult.Allowed, result)
    }

    @Test
    fun `from in progress to completed is allowed`() {
        val result = rule.canTransition(ServiceStatus.IN_PROGRESS, ServiceStatus.COMPLETED)

        assertEquals(TransitionResult.Allowed, result)
    }

    @Test
    fun `received cannot jump to completed`() {
        assertIs<TransitionResult.Denied>(
            rule.canTransition(ServiceStatus.RECEIVED, ServiceStatus.COMPLETED)
        )
    }

    @Test
    fun `in progress cannot move back to received`() {
        assertIs<TransitionResult.Denied>(
            rule.canTransition(ServiceStatus.IN_PROGRESS, ServiceStatus.RECEIVED)
        )
    }

    @Test
    fun `completed is terminal toward any state`() {
        ServiceStatus.entries.forEach { target ->
            assertIs<TransitionResult.Denied>(
                rule.canTransition(ServiceStatus.COMPLETED, target),
                "COMPLETED no debe admitir transicion hacia $target",
            )
        }
    }

    @Test
    fun `a transition into the same state is always denied`() {
        ServiceStatus.entries.forEach { status ->
            assertIs<TransitionResult.Denied>(
                rule.canTransition(status, status),
                "$status no debe poder transicionar hacia si mismo",
            )
        }
    }

    @Test
    fun `closing without a mileage is denied`() {
        assertIs<TransitionResult.Denied>(
            rule.validateClosure(mileageAtService = null, lastKnownMileage = 40_000)
        )
    }

    @Test
    fun `closing with a mileage below the last known value is denied`() {
        assertIs<TransitionResult.Denied>(
            rule.validateClosure(mileageAtService = 39_999, lastKnownMileage = 40_000)
        )
    }

    @Test
    fun `closing with the same known mileage is allowed`() {
        val result = rule.validateClosure(mileageAtService = 40_000, lastKnownMileage = 40_000)

        assertEquals(TransitionResult.Allowed, result)
    }

    @Test
    fun `closing with a higher mileage is allowed`() {
        val result = rule.validateClosure(mileageAtService = 41_500, lastKnownMileage = 40_000)

        assertEquals(TransitionResult.Allowed, result)
    }
}