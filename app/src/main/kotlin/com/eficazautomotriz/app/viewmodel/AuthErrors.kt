package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.data.auth.AuthFailure
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** The user closed the Google account picker. Not an error worth showing. */
internal const val GOOGLE_SIGN_IN_CANCELLED = 12501

private const val GENERIC = "Ocurrió un error. Intente de nuevo."
private const val NETWORK = "Sin conexión. Revise su red e intente de nuevo."
private const val TOO_MANY = "Demasiados intentos. Espere un momento e intente de nuevo."
private const val WRONG_CREDENTIALS = "Correo o contraseña incorrectos."
private const val INVALID_EMAIL = "El correo no es válido."
private const val USER_NOT_FOUND = "No existe una cuenta con ese correo."
private const val USER_DISABLED = "Esta cuenta está deshabilitada. Contacte al taller."
private const val EMAIL_IN_USE = "Ya existe una cuenta con ese correo."
private const val DIFFERENT_CREDENTIAL =
    "Ese correo ya está registrado con otro método. Use el método con el que creó la cuenta."
private const val CREDENTIAL_IN_USE = "Esa credencial ya está asociada a otra cuenta."
private const val WEAK_PASSWORD = "La contraseña es demasiado débil. Use al menos 6 caracteres."
private const val NOT_ALLOWED = "Este método de acceso no está habilitado. Contacte al taller."
private const val SESSION_EXPIRED = "Su sesión expiró. Inicie sesión de nuevo."
private const val RECENT_LOGIN = "Por seguridad, vuelva a iniciar sesión e intente de nuevo."

/**
 * Spanish message for one Firebase Auth error code. The code itself is never part of the text.
 * Codes are the ones [com.google.firebase.auth.FirebaseAuth] returns for email sign-in,
 * registration, Google sign-in and password reset.
 */
internal fun messageForAuthCode(code: String): String = when (code) {
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_MISMATCH" -> WRONG_CREDENTIALS
    "ERROR_INVALID_EMAIL" -> INVALID_EMAIL
    "ERROR_USER_NOT_FOUND" -> USER_NOT_FOUND
    "ERROR_USER_DISABLED" -> USER_DISABLED
    "ERROR_EMAIL_ALREADY_IN_USE" -> EMAIL_IN_USE
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> DIFFERENT_CREDENTIAL
    "ERROR_CREDENTIAL_ALREADY_IN_USE" -> CREDENTIAL_IN_USE
    "ERROR_WEAK_PASSWORD" -> WEAK_PASSWORD
    "ERROR_OPERATION_NOT_ALLOWED" -> NOT_ALLOWED
    "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> SESSION_EXPIRED
    "ERROR_REQUIRES_RECENT_LOGIN" -> RECENT_LOGIN
    "ERROR_TOO_MANY_REQUESTS" -> TOO_MANY
    "ERROR_NETWORK_REQUEST_FAILED" -> NETWORK
    else -> GENERIC
}

/** Maps a gateway failure to a message. The UI never sees Firebase types or error codes. */
internal fun authErrorMessage(error: Throwable): String =
    generateSequence(error) { it.cause }.firstNotNullOfOrNull { messageOrNull(it) } ?: GENERIC

/**
 * Google Sign-In status, or null when the user dismissed the picker.
 * Play Services status numbers stay out of the text.
 */
internal fun googleSignInMessage(statusCode: Int): String? = when (statusCode) {
    GOOGLE_SIGN_IN_CANCELLED -> null
    7 -> NETWORK
    12502 -> "El inicio con Google ya está en curso. Espere un momento."
    10 -> "El inicio con Google no está configurado en esta compilación."
    else -> "No se pudo iniciar sesión con Google. Intente de nuevo."
}

private fun messageOrNull(error: Throwable): String? = when (error) {
    is AuthFailure -> error.message?.takeIf { it.isNotBlank() } ?: GENERIC
    is FirebaseAuthException -> messageForAuthCode(error.errorCode)
    is FirebaseNetworkException -> NETWORK
    is FirebaseTooManyRequestsException -> TOO_MANY
    is UnknownHostException, is SocketTimeoutException, is ConnectException -> NETWORK
    else -> null
}
