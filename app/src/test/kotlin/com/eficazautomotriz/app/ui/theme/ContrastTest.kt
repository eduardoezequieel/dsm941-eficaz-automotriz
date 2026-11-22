package com.eficazautomotriz.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** WCAG AA (4.5:1) for small text: status chips and the secondary text over the app surfaces. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun assertReadable(name: String, text: Color, background: Color) {
        val r = ratio(text, background)
        assertTrue(r >= 4.5f, "$name contrast is $r, below 4.5")
    }

    @Test
    fun `every status chip tone is readable`() {
        StatusTone.entries.forEach { assertReadable("chip ${it.name}", it.colors.content, it.colors.background) }
    }

    @Test
    fun `secondary text is readable on the page, the cards and the segmented tabs`() {
        assertReadable("secondary on background", EficazColors.TextSecondary, EficazColors.Background)
        assertReadable("secondary on surface", EficazColors.TextSecondary, EficazColors.Surface)
        assertReadable("secondary on segment", EficazColors.TextSecondary, EficazColors.SegmentBackground)
    }

    @Test
    fun `header subtitle and evidence placeholders are readable`() {
        assertReadable("header subtitle", EficazColors.HeaderSubtitle, EficazColors.Navy)
        assertReadable("placeholder label", EficazColors.PlaceholderText, EficazColors.PlaceholderTile)
    }
}