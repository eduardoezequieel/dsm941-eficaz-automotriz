package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eficazautomotriz.app.ui.SessionViewModelStoreOwner
import com.eficazautomotriz.domain.error.DomainError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Navigation scope and the store AppRoot clears when that scope changes. */
class SessionScopeTest {

    @Test
    fun `the navigation scope changes when the signed-in uid changes or the session ends`() {
        assertEquals(SIGNED_OUT_SCOPE, navigationScopeId(null))
        assertEquals("uid-a", navigationScopeId("uid-a"))
        assertNotEquals(navigationScopeId("uid-a"), navigationScopeId("uid-b"))
        assertNotEquals(navigationScopeId("uid-a"), navigationScopeId(null))
    }

    @Test
    fun `a system failure shows the generic Spanish message without the exception text`() {
        val error = DomainError.NotFound(
            EficazViewModel.SYSTEM_ENTITY,
            "PERMISSION_DENIED: Missing or insufficient permissions.",
        )
        val text = error.userMessage()
        assertEquals("No se pudo completar la operación. Revise su conexión e intente de nuevo.", text)
        assertFalse(text.contains("PERMISSION"))
        assertFalse(text.contains("Missing"))
        assertFalse(text.contains("("))
    }

    @Test
    fun `a business error keeps its own Spanish message`() {
        val error = DomainError.NotFound("vehiculo", "VEH-001")
        assertEquals(error.message, error.userMessage())
        assertTrue(error.userMessage().contains("VEH-001"))
    }

    @Test
    fun `a profile that failed offline does not show the exception text`() {
        val error = DomainError.NotFound(
            EficazViewModel.SYSTEM_ENTITY,
            "PERMISSION_DENIED: Missing or insufficient permissions.",
        )
        val text = profileErrorMessage(error)
        assertEquals("No se pudo cargar su perfil. Revise su conexión e intente de nuevo.", text)
        assertFalse(text.contains("PERMISSION"))
        assertFalse(text.contains("Missing"))
    }

    @Test
    fun `clearing the session store drops the previous account ViewModel`() {
        val owner = SessionViewModelStoreOwner()
        val first = ViewModelProvider(owner, ViewModelProvider.NewInstanceFactory())[MarkerViewModel::class.java]
        first.userId = "uid-a"
        owner.clear()
        assertTrue(first.cleared)
        val second = ViewModelProvider(owner, ViewModelProvider.NewInstanceFactory())[MarkerViewModel::class.java]
        assertTrue(first !== second)
        assertNull(second.userId)
    }

    class MarkerViewModel : ViewModel() {
        var userId: String? = null
        var cleared = false
            private set

        public override fun onCleared() {
            cleared = true
            super.onCleared()
        }
    }
}
