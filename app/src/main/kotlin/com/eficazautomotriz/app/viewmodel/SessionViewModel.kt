package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Checking : SessionState
    data class SignedOut(val message: String? = null) : SessionState
    data class SignedIn(val user: User) : SessionState
    /** There is an Auth session but the profile could not be read (no network, rules): retryable. */
    data class ProfileError(val message: String) : SessionState
}

/**
 * Owner of the signed-in user. After Firebase authentication the domain `User` is ALWAYS
 * loaded with `AuthService.signIn(uid)`, and it is the `actor` every other ViewModel receives.
 */
class SessionViewModel(graph: AppGraph) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var pendingUid: String? = null

    init {
        val uid = graph.authGateway.currentUid()
        if (uid == null) _state.value = SessionState.SignedOut() else onAuthenticated(uid)
    }

    /** Called at start with a previous session or when AuthViewModel authenticates. */
    fun onAuthenticated(uid: String) {
        pendingUid = uid
        _state.value = SessionState.Checking
        run({ outcome -> onProfile(outcome) }) {
            graph.authService.signIn(uid)
        }
    }

    fun retry() {
        pendingUid?.let(::onAuthenticated)
    }

    fun signOut(message: String? = null) {
        graph.authGateway.signOut()
        pendingUid = null
        _state.value = SessionState.SignedOut(message)
    }

    private fun onProfile(outcome: Outcome<User>) {
        when (outcome) {
            is Outcome.Success -> viewModelScope.launch {
                // If the catalog is empty, try to seed it; the rules only allow that for staff.
                graph.ensureCatalogs()
                _state.value = SessionState.SignedIn(outcome.value)
            }
            is Outcome.Failure -> {
                val error = outcome.error
                if (error is DomainError.NotFound && error.entity == USER_ENTITY) {
                    signOut(NO_PROFILE_MESSAGE)
                } else {
                    _state.value = SessionState.ProfileError(error.userMessage())
                }
            }
        }
    }

    private companion object {
        /** Entity used by `AuthService.signIn` when the usuarios/{uid} document is not found. */
        const val USER_ENTITY = "usuario"
        const val NO_PROFILE_MESSAGE = "Cuenta sin perfil; contacte al taller."
    }
}
