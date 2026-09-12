package com.eficazautomotriz.application

import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.logging.ErrorLogger

/**
 * Seleccion del usuario de la sesion. La Etapa 2 no implementa contrasenas:
 * la autenticacion real llega en la Etapa 3 con Firebase Authentication.
 */
class AuthService(
    private val users: UserRepository,
    logger: ErrorLogger,
) : ApplicationService(logger) {

    fun availableUsers(): List<User> = users.findAll().sortedBy { it.name }

    fun findById(userId: String): User? = users.findById(userId)

    fun nameOf(userId: String): String = users.findById(userId)?.name ?: userId

    fun signIn(userId: String): Outcome<User> {
        val user = users.findById(userId)
            ?: return fail("signIn", DomainError.NotFound("usuario", userId))
        return Outcome.success(user)
    }
}