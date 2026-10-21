package com.eficazautomotriz.app.ui.staff.reports

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Pure chart calculations, tested on the JVM without a Canvas. */
object ChartMath {

    /** Fraction of the track a bar fills: `count / max`, clamped to 0..1. */
    fun barFraction(count: Int, maxCount: Int): Float =
        if (maxCount <= 0) 0f else (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

    /**
     * Y-axis ticks from 0 with [intervals] equal "nice" steps whose top covers [max]:
     * 21 → 0, 8, 16, 24 (as in f9); 5 → 0, 2, 4, 6; 0 → 0, 1, 2, 3.
     * The step is the first of 1, 2, 4, 5, 8, 10 (times a power of ten) that is at least `max / intervals`.
     */
    fun axisTicks(max: Int, intervals: Int = 3): List<Int> {
        val raw = ceil(max.coerceAtLeast(1).toDouble() / intervals).coerceAtLeast(1.0)
        val magnitude = 10.0.pow(floor(log10(raw)))
        val step = listOf(1.0, 2.0, 4.0, 5.0, 8.0, 10.0).map { it * magnitude }.first { it >= raw }.roundToInt()
        return (0..intervals).map { it * step }
    }

    /**
     * `percentage` goes from 0 to 100 and a full circle is 360 degrees. An arc of exactly 360
     * is drawn empty on some Android versions, so it is clipped to 359.99.
     */
    fun sweepOf(percentage: Double): Float {
        val sweep = percentage.toFloat() * 3.6f
        if (sweep <= 0f) return 0f
        return if (sweep >= 360f) FULL_CIRCLE else sweep
    }

    fun formatShare(percentage: Double): String = String.format(Locale.US, "%.1f %%", percentage)

    /** Legend percentage of f9: whole number, no space ("30%"). */
    fun wholePercent(percentage: Double): String = "${percentage.roundToInt()}%"

    /** Display-only shortening of long legend names, as f9 does ("Mant. preventivo", "Diagnóstico escáner"). */
    fun legendLabel(name: String): String = when {
        name.startsWith("Mantenimiento ") -> "Mant. " + name.removePrefix("Mantenimiento ")
        name.startsWith("Diagnóstico por ") -> "Diagnóstico " + name.removePrefix("Diagnóstico por ")
        else -> name
    }

    private const val FULL_CIRCLE = 359.99f
}
