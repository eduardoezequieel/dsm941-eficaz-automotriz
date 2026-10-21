package com.eficazautomotriz.app.data.auth

import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * [AuthGateway] over Firebase Auth. Firebase errors (invalid credentials, email in use, no
 * network) propagate as exceptions so the ViewModel can turn them into messages.
 */
class FirebaseAuthGateway(
    private val auth: FirebaseAuth,
    private val users: UserRepository,
) : AuthGateway {

    override fun currentUid(): String? = auth.currentUser?.uid

    override suspend fun signInWithEmail(email: String, password: String): String =
        requireUid(auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid)

    override suspend fun registerWithEmail(name: String, phone: String, email: String, password: String): String {
        val firebaseUser = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
        val uid = requireUid(firebaseUser?.uid)
        try {
            saveProfile(User(uid, name.trim(), email.trim(), phone.trim(), UserRole.CLIENT))
        } catch (e: Exception) {
            // Without a profile the account would be unusable: undo it so the user can retry.
            runCatching { firebaseUser?.delete()?.await() }
            auth.signOut()
            throw e
        }
        return uid
    }

    override suspend fun signInWithGoogle(idToken: String): String {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val firebaseUser = auth.signInWithCredential(credential).await().user
        val uid = requireUid(firebaseUser?.uid)
        try {
            val exists = withContext(Dispatchers.IO) { users.findById(uid) != null }
            if (!exists) {
                val email = firebaseUser?.email.orEmpty()
                saveProfile(
                    User(
                        id = uid,
                        name = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: email,
                        email = email,
                        phone = firebaseUser?.phoneNumber.orEmpty(),
                        role = UserRole.CLIENT,
                    )
                )
            }
        } catch (e: Exception) {
            // Mirrors registerWithEmail: never leave an Auth session without a readable profile.
            // The Google account itself is kept; the next Google sign-in retries the profile.
            auth.signOut()
            throw e
        }
        return uid
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override fun signOut() = auth.signOut()

    private suspend fun saveProfile(user: User) {
        withContext(Dispatchers.IO) { users.save(user) }
    }

    private fun requireUid(uid: String?): String =
        uid ?: throw IllegalStateException("Firebase Auth no devolvio un usuario autenticado")
}
