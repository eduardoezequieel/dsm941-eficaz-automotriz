package com.eficazautomotriz.app.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InitialsTest {

    @Test
    fun `two words give their first letters`() {
        assertEquals("DE", initialsOf("Diego Escobar"))
        assertEquals("CC", initialsOf("  Carlos   Cruz "))
        assertEquals("NF", initialsOf("Nissan Frontier 2019"))
    }

    @Test
    fun `a single word gives its first two letters and blank gives a placeholder`() {
        assertEquals("CA", initialsOf("carlos"))
        assertEquals("?", initialsOf("   "))
    }
}
