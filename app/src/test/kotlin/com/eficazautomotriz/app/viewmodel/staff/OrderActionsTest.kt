package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.eficazautomotriz.domain.rules.ServiceTransitionRule
import com.eficazautomotriz.domain.rules.TransitionResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The staff order screen must only offer actions the domain will accept. */
class OrderActionsTest {

    private val rule = ServiceTransitionRule()

    @Test
    fun `a received order can be started and documented but not completed`() {
        assertEquals(setOf(OrderAction.START, OrderAction.ADD_EVIDENCE), orderActionsFor(ServiceStatus.RECEIVED))
    }

    @Test
    fun `an order in progress can be documented and completed but not started again`() {
        assertEquals(setOf(OrderAction.ADD_EVIDENCE, OrderAction.COMPLETE), orderActionsFor(ServiceStatus.IN_PROGRESS))
    }

    @Test
    fun `a completed order offers no actions`() {
        assertTrue(orderActionsFor(ServiceStatus.COMPLETED).isEmpty())
    }

    @Test
    fun `every offered transition is allowed by ServiceTransitionRule`() {
        ServiceStatus.entries.forEach { status ->
            val actions = orderActionsFor(status)
            assertEquals(
                rule.canTransition(status, ServiceStatus.IN_PROGRESS) == TransitionResult.Allowed,
                OrderAction.START in actions,
                "START for $status",
            )
            assertEquals(
                rule.canTransition(status, ServiceStatus.COMPLETED) == TransitionResult.Allowed,
                OrderAction.COMPLETE in actions,
                "COMPLETE for $status",
            )
        }
    }
}
