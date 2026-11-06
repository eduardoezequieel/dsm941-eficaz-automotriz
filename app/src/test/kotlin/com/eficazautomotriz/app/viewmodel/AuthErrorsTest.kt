package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.data.auth.AuthFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

/**
 * Every Firebase Auth code the app can receive from email sign-in, registration,
 * Google sign-in or password reset. The technical code must not reach the user.
 */
class AuthErrorsTest {

    private val codes = listOf(
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_USER_MISMATCH",
        "ERROR_INVALID_EMAIL",
        "ERROR_USER_NOT_FOUND",
        "ERROR_USER_DISABLED",
        "ERROR_EMAIL_ALREADY_IN_USE",
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
        "ERROR_CREDENTIAL_ALREADY_IN_USE",
        "ERROR_WEAK_PASSWORD",
        "ERROR_OPERATION_NOT_ALLOWED",
        "ERROR_USER_TOKEN_EXPIRED",
        "ERROR_INVALID_USER_TOKEN",
        "ERROR_REQUIRES_RECENT_LOGIN",
        "ERROR_TOO_MANY_REQUESTS",
        "ERROR_NETWORK_REQUEST_FAILED",
    )

    @Test
    fun `each auth code maps to Spanish and the code stays hidden`() {
        codes.forEach { code ->
            val message = messageForAuthCode(code)
            assertFalse(message.contains("ERROR_"), code)
            assertFalse(message.contains(code), code)
            assertFalse(message.contains("Firebase"), code)
            assertTrue(message.isNotBlank(), code)
        }
    }

    @Test
    fun `credentials, collision, weak password and expired session use distinct messages`() {
        assertEquals("Correo o contraseña incorrectos.", messageForAuthCode("ERROR_WRONG_PASSWORD"))
        assertEquals("Correo o contraseña incorrectos.", messageForAuthCode("ERROR_INVALID_CREDENTIAL"))
        assertEquals("El correo no es válido.", messageForAuthCode("ERROR_INVALID_EMAIL"))
        assertEquals("Ya existe una cuenta con ese correo.", messageForAuthCode("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(
            "Ese correo ya está registrado con otro método. Use el método con el que creó la cuenta.",
            messageForAuthCode("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"),
        )
        assertEquals(
            "La contraseña es demasiado débil. Use al menos 6 caracteres.",
            messageForAuthCode("ERROR_WEAK_PASSWORD"),
        )
        assertEquals("Su sesión expiró. Inicie sesión de nuevo.", messageForAuthCode("ERROR_USER_TOKEN_EXPIRED"))
        assertEquals("No existe una cuenta con ese correo.", messageForAuthCode("ERROR_USER_NOT_FOUND"))
        assertEquals("Esta cuenta está deshabilitada. Contacte al taller.", messageForAuthCode("ERROR_USER_DISABLED"))
    }

    @Test
    fun `an unknown code becomes a generic message without the code`() {
        val message = messageForAuthCode("ERROR_INTERNAL_ERROR")
        assertEquals("Ocurrió un error. Intente de nuevo.", message)
        assertFalse(message.contains("INTERNAL"))
    }

    @Test
    fun `a raw network failure stays in Spanish`() {
        val message = authErrorMessage(UnknownHostException("offline"))
        assertEquals("Sin conexión. Revise su red e intente de nuevo.", message)
        assertFalse(message.contains("offline"))
    }

    @Test
    fun `auth failure keeps its Spanish message`() {
        assertEquals(
            "Ya existe una cuenta con ese correo.",
            authErrorMessage(AuthFailure("Ya existe una cuenta con ese correo.")),
        )
    }

    @Test
    fun `google cancellation is silent and other statuses hide the number`() {
        assertNull(googleSignInMessage(GOOGLE_SIGN_IN_CANCELLED))
        assertEquals("Sin conexión. Revise su red e intente de nuevo.", googleSignInMessage(7))
        val failed = googleSignInMessage(12500)
        assertEquals("No se pudo iniciar sesión con Google. Intente de nuevo.", failed)
        assertFalse(failed!!.contains("12500"))
        val unconfigured = googleSignInMessage(10)
        assertFalse(unconfigured!!.contains("10"))
        assertFalse(unconfigured.contains("DEVELOPER"))
    }
}
