package com.eficazautomotriz.app.data.auth

/**
 * Authentication backend. It only authenticates and creates the `usuarios/{uid}` profile;
 * the domain `User` is loaded afterwards with `AuthService.signIn(uid)`.
 *
 * Implementations: [FirebaseAuthGateway] (production) and [InMemoryAuthGateway] (debug demo
 * mode). Failures propagate as exceptions; the ViewModel turns them into messages.
 */
interface AuthGateway {

    fun currentUid(): String?

    /** Returns the authenticated uid. */
    suspend fun signInWithEmail(email: String, password: String): String

    /** Creates the account and its profile, always with the CLIENT role. Returns the uid. */
    suspend fun registerWithEmail(name: String, phone: String, email: String, password: String): String

    /** On the first sign-in with that Google account, creates its profile as CLIENT. */
    suspend fun signInWithGoogle(idToken: String): String

    /** Sends the provider's password reset email to [email]. */
    suspend fun sendPasswordReset(email: String)

    fun signOut()
}

/** Gateway failure whose message is already user-facing Spanish (used by non-Firebase gateways). */
class AuthFailure(message: String) : Exception(message)