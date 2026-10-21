package com.eficazautomotriz.app.data.auth

import com.eficazautomotriz.data.memory.InMemoryUserRepository
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InMemoryAuthGatewayTest {

    private val users = InMemoryUserRepository().apply {
        save(User("USR-001", "Diego Escobar", "cliente@demo.test", "7012-3456", UserRole.CLIENT))
    }
    private val gateway = InMemoryAuthGateway(
        users,
        mapOf("Cliente@Demo.test" to InMemoryAuthGateway.Account("Demo1234", "USR-001")),
    )

    @Test
    fun `signs in ignoring email case and surrounding spaces`() = runBlocking {
        assertEquals("USR-001", gateway.signInWithEmail("  cliente@demo.TEST ", "Demo1234"))
        assertEquals("USR-001", gateway.currentUid())
    }

    @Test
    fun `wrong password fails with a Spanish message and keeps the session closed`() {
        val failure = assertThrows<AuthFailure> { runBlocking { gateway.signInWithEmail("cliente@demo.test", "x") } }
        assertEquals("Correo o contraseña incorrectos.", failure.message)
        assertNull(gateway.currentUid())
    }

    @Test
    fun `register creates a client profile and signs in`() = runBlocking {
        val uid = gateway.registerWithEmail("Nueva Persona", "7000-0000", "nueva@demo.test", "Secreta1")
        assertEquals(UserRole.CLIENT, users.findById(uid)?.role)
        assertEquals(uid, gateway.currentUid())
        gateway.signOut()
        assertEquals(uid, gateway.signInWithEmail("nueva@demo.test", "Secreta1"))
    }

    @Test
    fun `register rejects an email already in use`() {
        assertThrows<AuthFailure> {
            runBlocking { gateway.registerWithEmail("Otra", "7000-0000", "cliente@demo.test", "Secreta1") }
        }
    }

    @Test
    fun `password reset only accepts known accounts`() {
        runBlocking { gateway.sendPasswordReset("cliente@demo.test") }
        assertThrows<AuthFailure> { runBlocking { gateway.sendPasswordReset("nadie@demo.test") } }
    }

    @Test
    fun `starts signed in when asked and signs out`() {
        val signedIn = InMemoryAuthGateway(users, emptyMap(), signedInUid = "USR-001")
        assertEquals("USR-001", signedIn.currentUid())
        signedIn.signOut()
        assertNull(signedIn.currentUid())
    }
}
