package com.eficazautomotriz.app.ui.staff.reports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Pure geometry and labels behind the Canvas charts. */
class ChartMathTest {

    @Test
    fun `bar fraction is count over max and clamped`() {
        assertEquals(0.5f, ChartMath.barFraction(3, 6))
        assertEquals(1f, ChartMath.barFraction(6, 6))
        assertEquals(1f, ChartMath.barFraction(9, 6))
        assertEquals(0f, ChartMath.barFraction(3, 0))
    }

    @Test
    fun `donut sweep is percentage times three point six`() {
        assertEquals(90f, ChartMath.sweepOf(25.0), 0.001f)
        assertEquals(0f, ChartMath.sweepOf(0.0))
        assertEquals(0f, ChartMath.sweepOf(-5.0))
    }

    @Test
    fun `a single slice never sweeps a full circle`() {
        assertEquals(359.99f, ChartMath.sweepOf(100.0), 0.001f)
    }

    @Test
    fun `slices of a complete report add up to the whole donut`() {
        val percentages = listOf(100.0 / 3, 100.0 / 3, 100.0 / 3)
        assertEquals(360f, percentages.sumOf { ChartMath.sweepOf(it).toDouble() }.toFloat(), 0.01f)
    }

    @Test
    fun `share label uses one decimal and a dot separator`() {
        assertEquals("33.3 %", ChartMath.formatShare(100.0 / 3))
        assertEquals("0.0 %", ChartMath.formatShare(0.0))
    }
}
