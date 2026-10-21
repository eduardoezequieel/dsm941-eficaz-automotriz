package com.eficazautomotriz.app.data.auth

import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [AuthGateway] kept in memory, for the debug demo mode (no Firebase, no network).
 * [accounts] maps a lowercase email to its password and uid; the uid must exist in [users].
 * [signedInUid] lets the demo start already signed in. Repository writes run on [io], the same
 * serialized dispatcher the ViewModels use, because the in-memory repositories are not thread-safe.
 */
class InMemoryAuthGateway(
    private val users: UserRepository,
    accounts: Map<String, Account>,
    signedInUid: String? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : AuthGateway {

    data class Account(val password: String, val uid: String)

    private val lock = Any()
    private val accounts = accounts.mapKeys { it.key.lowercase() }.toMutableMap()
    private var current: String? = signedInUid

    override fun currentUid(): String? = synchronized(lock) { current }

    override suspend fun signInWithEmail(email: String, password: String): String = synchronized(lock) {
        val account = accounts[email.trim().lowercase()]
        if (account == null || account.password != password) throw AuthFailure(INVALID_CREDENTIALS)
        current = account.uid
        account.uid
    }

    override suspend fun registerWithEmail(name: String, phone: String, email: String, password: String): String =
        withContext(io) { register(name, phone, email, password) }

    private fun register(name: String, phone: String, email: String, password: String): String =
        synchronized(lock) {
            val key = email.trim().lowercase()
            if (key in accounts) throw AuthFailure("Ya existe una cuenta con ese correo.")
            val uid = users.nextId()
            users.save(User(uid, name.trim(), email.trim(), phone.trim(), UserRole.CLIENT))
            accounts[key] = Account(password, uid)
            current = uid
            uid
        }

    override suspend fun signInWithGoogle(idToken: String): String =
        throw AuthFailure("El inicio con Google no está disponible en el modo demostración.")

    override suspend fun sendPasswordReset(email: String) {
        // Nothing is sent: the demo only checks that the account exists.
        synchronized(lock) {
            if (email.trim().lowercase() !in accounts) throw AuthFailure("No existe una cuenta con ese correo.")
        }
    }

    override fun signOut() = synchronized(lock) { current = null }

    private companion object {
        const val INVALID_CREDENTIALS = "Correo o contraseña incorrectos."
    }
}
