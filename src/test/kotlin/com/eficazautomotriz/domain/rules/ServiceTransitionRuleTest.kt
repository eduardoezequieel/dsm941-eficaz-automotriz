package com.eficazautomotriz.domain.rules

import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ServiceTransitionRuleTest {

    private val rule = ServiceTransitionRule()

    @Test
    fun `de recibido a en proceso se permite`() {
        val result = rule.canTransition(ServiceStatus.RECEIVED, ServiceStatus.IN_PROGRESS)

        assertEquals(TransitionResult.Allowed, result)
    }

    @Test
    fun `de en proceso a finalizado se permite`() {
        val result = rule.canTransition(ServiceStatus.IN_PROGRESS, ServiceStatus.COMPLETED)

        assertEquals(TransitionResult.Allowed, result)
    }

    @Test
    fun `no se puede saltar de recibido a finalizado`() {
        assertIs<TransitionResult.Denied>(
            rule.canTransition(ServiceStatus.RECEIVED, ServiceStatus.COMPLETED)
        )
    }

    @Test
    fun `no se puede retroceder de en proceso a recibido`() {
        assertIs<TransitionResult.Denied>(
            rule.canTransition(ServiceStatus.IN_PROGRESS, ServiceStatus.RECEIVED)
        )
    }

    @Test
    fun `finalizado es terminal hacia cualquier estado`() {
        ServiceStatus.entries.forEach { target ->
            assertIs<TransitionResult.Denied>(
                rule.canTransition(ServiceStatus.COMPLETED, target),
                "COMPLETED no debe admitir transicion hacia $target",
            )
        }
    }

    @Test
    fun `una transicion al mismo estado siempre se deniega`() {
        ServiceStatus.entries.forEach { status ->
            assertIs<TransitionResult.Denied>(
                rule.canTransition(status, status),
                "$status no debe poder transicionar hacia si mismo",
            )
        }
    }

    @Test
    fun `cerrar sin kilometraje se deniega`() {
        assertIs<TransitionResult.Denied>(
            rule.validateClosure(mileageAtService = null, lastKnownMileage = 40_000)
        )
    }

    @Test
    fun `cerrar con kilometraje menor al ultimo conocido se deniega`() {
        assertIs<TransitionResult.Denied>(
            rule.validateClosure(mileageAtService = 39_999, lastKnownMileage = 40_000)
        )
    }

    @Test
    fun `cerrar con el mismo kilometraje conocido se permite`() {
        val result = rule.validateClosure(mileageAtService = 40_000, lastKnownMileage = 40_000)

        assertEquals(TransitionResult.Allowed, result)
    }

    @Test
    fun `cerrar con kilometraje mayor se permite`() {
        val result = rule.validateClosure(mileageAtService = 41_500, lastKnownMileage = 40_000)

        assertEquals(TransitionResult.Allowed, result)
    }
}
