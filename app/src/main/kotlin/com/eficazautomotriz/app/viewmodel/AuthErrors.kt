package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.data.auth.AuthFailure
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Maps Firebase Auth exceptions to messages; the UI never sees Firebase types. */
internal fun authErrorMessage(error: Throwable): String = when (error) {
    is AuthFailure -> error.message ?: "No se pudo completar la operación."
    is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil."
    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo o fue deshabilitada."
    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
    is FirebaseNetworkException -> "Sin conexión. Revise su red e intente de nuevo."
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espere un momento e intente de nuevo."
    else -> "Ocurrió un error. Inténtalo de nuevo."
}
