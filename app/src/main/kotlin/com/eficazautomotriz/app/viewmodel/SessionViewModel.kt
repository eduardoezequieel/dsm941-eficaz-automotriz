package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** NavHost identity while nobody is signed in. A change discards every screen ViewModel. */
internal const val SIGNED_OUT_SCOPE = "signed-out"

/** Scope id of the navigation graph. Screen ViewModels are cleared when it changes. */
internal fun navigationScopeId(signedInUserId: String?): String = signedInUserId ?: SIGNED_OUT_SCOPE

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
 *
 * [scopeId] is the navigation key. [AppRoot] keeps a [com.eficazautomotriz.app.ui.SessionViewModelStoreOwner]
 * per value and clears it when the value changes, so signing out or switching accounts drops
 * every screen ViewModel without each screen knowing.
 */
class SessionViewModel(graph: AppGraph) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _scopeId = MutableStateFlow(SIGNED_OUT_SCOPE)
    val scopeId: StateFlow<String> = _scopeId.asStateFlow()

    /** Uid whose profile load is the one allowed to publish. Null once signed out. */
    private var pendingUid: String? = null

    /** Bumped on every load and on every sign-out, so a finished older load cannot publish. */
    private var loadGeneration = 0
    private var loadJob: Job? = null
    private val stopObserving: () -> Unit

    init {
        val uid = graph.authGateway.currentUid()
        if (uid == null) _state.value = SessionState.SignedOut() else onAuthenticated(uid)
        stopObserving = graph.authGateway.observeUid(::onExternalUid)
    }

    /** Called at start with a previous session or when AuthViewModel authenticates. */
    fun onAuthenticated(uid: String) {
        startProfileLoad(uid)
    }

    fun retry() {
        pendingUid?.let(::startProfileLoad)
    }

    fun signOut(message: String? = null) {
        // Publish before the gateway notifies, so the listener does not replace [message].
        dropSession(message)
        graph.authGateway.signOut()
    }

    override fun onCleared() {
        loadJob?.cancel()
        stopObserving()
        super.onCleared()
    }

    /**
     * Auth uid changed outside this ViewModel (revocation, disablement, account switch).
     * A new uid while signed out is ignored: registration and the first Google sign-in publish
     * it before `usuarios/{uid}` exists, and [onAuthenticated] runs only after that write.
     */
    private fun onExternalUid(latest: String?) {
        if (latest == pendingUid) return
        if (latest == null) {
            if (_state.value is SessionState.SignedOut) return
            dropSession(SESSION_ENDED)
            graph.authGateway.signOut()
            return
        }
        if (pendingUid == null) return
        _scopeId.value = SIGNED_OUT_SCOPE
        startProfileLoad(latest)
    }

    private fun startProfileLoad(uid: String) {
        loadJob?.cancel()
        val generation = ++loadGeneration
        pendingUid = uid
        _state.value = SessionState.Checking
        loadJob = viewModelScope.launch {
            val outcome = loadProfile(uid)
            if (!isActive || !isCurrent(uid, generation)) return@launch
            when (outcome) {
                is Outcome.Success -> {
                    // If the catalog is empty, try to seed it; the rules only allow that for staff.
                    graph.ensureCatalogs()
                    ensureActive()
                    if (!isCurrent(uid, generation)) return@launch
                    _scopeId.value = navigationScopeId(outcome.value.id)
                    _state.value = SessionState.SignedIn(outcome.value)
                }
                is Outcome.Failure -> {
                    if (!isCurrent(uid, generation)) return@launch
                    val error = outcome.error
                    if (error is DomainError.NotFound && error.entity == USER_ENTITY) {
                        signOut(NO_PROFILE_MESSAGE)
                    } else {
                        _state.value = SessionState.ProfileError(profileErrorMessage(error))
                    }
                }
            }
        }
    }

    /** Same failure shape as [EficazViewModel.run]: the screen stays up, the detail goes to the log. */
    private suspend fun loadProfile(uid: String): Outcome<User> = withContext(graph.io) {
        runCatching { graph.authService.signIn(uid) }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            graph.errorLogger.error(
                "SessionViewModel",
                failure::class.simpleName ?: "Exception",
                failure.message.orEmpty(),
            )
            Outcome.failure(DomainError.NotFound(EficazViewModel.SYSTEM_ENTITY, failure.message ?: "sin detalle"))
        }
    }

    private fun dropSession(message: String?) {
        loadJob?.cancel()
        loadJob = null
        loadGeneration++
        pendingUid = null
        _scopeId.value = SIGNED_OUT_SCOPE
        _state.value = SessionState.SignedOut(message)
    }

    private fun isCurrent(uid: String, generation: Int): Boolean =
        generation == loadGeneration && pendingUid == uid

    private companion object {
        /** Entity used by `AuthService.signIn` when the usuarios/{uid} document is not found. */
        const val USER_ENTITY = "usuario"
        const val NO_PROFILE_MESSAGE = "Cuenta sin perfil; contacte al taller."
        const val SESSION_ENDED = "Su sesión ya no es válida. Inicie sesión de nuevo."
    }
}

/**
 * Text for a profile that could not be loaded. System failures (no network, rules) stay in
 * Spanish and do not append the exception text.
 */
internal fun profileErrorMessage(error: DomainError): String =
    if (error is DomainError.NotFound && error.entity == EficazViewModel.SYSTEM_ENTITY) {
        "No se pudo cargar su perfil. Revise su conexión e intente de nuevo."
    } else {
        error.userMessage()
    }
