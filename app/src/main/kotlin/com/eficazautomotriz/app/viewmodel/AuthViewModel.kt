package com.eficazautomotriz.app.viewmodel

import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.eficazautomotriz.app.BuildConfig
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.validation.AuthFormValidator
import com.eficazautomotriz.app.viewmodel.validation.FieldErrors
import com.eficazautomotriz.domain.error.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginForm(
    val email: String = "",
    val password: String = "",
    val errors: FieldErrors = FieldErrors(),
)

data class RegisterForm(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val errors: FieldErrors = FieldErrors(),
)

/** "¿Olvidó su contraseña?" dialog; null in [AuthState.reset] means the dialog is closed. */
data class PasswordResetForm(
    val email: String = "",
    val error: String? = null,
    val busy: Boolean = false,
)

data class AuthState(
    val login: LoginForm = LoginForm(),
    val register: RegisterForm = RegisterForm(),
    val busy: Boolean = false,
    val message: String? = null,
    val reset: PasswordResetForm? = null,
    /** One-shot confirmation (e.g. reset email sent), shown in the snackbar and consumed. */
    val notice: String? = null,
    /** Freshly authenticated uid: the screen hands it to SessionViewModel and consumes it. */
    val authenticatedUid: String? = null,
)

/**
 * Login and sign-up (P1). It only authenticates: SessionViewModel loads the domain profile
 * with `AuthService.signIn(uid)`, in a single place for both app start and login.
 */
class AuthViewModel(graph: AppGraph) : EficazViewModel(graph) {

    private val validator = AuthFormValidator { Patterns.EMAIL_ADDRESS.matcher(it).matches() }

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * Without a web client id in local.properties (or in demo mode, which has no Firebase) the
     * Google button is still drawn, as in f1, but only explains that it is unavailable.
     */
    val googleWebClientId: String? = BuildConfig.GOOGLE_WEB_CLIENT_ID.takeIf { it.isNotBlank() && !graph.demoMode }

    fun updateLogin(transform: (LoginForm) -> LoginForm) =
        _state.update { it.copy(login = transform(it.login), message = null) }

    fun updateRegister(transform: (RegisterForm) -> RegisterForm) =
        _state.update { it.copy(register = transform(it.register), message = null) }

    fun signIn() {
        val form = _state.value.login
        val errors = validator.login(form.email, form.password)
        _state.update { it.copy(login = form.copy(errors = errors)) }
        if (!errors.isValid) return
        val email = form.email.trim()
        when (val check = graph.authService.validateSignIn(email, form.password)) {
            is Outcome.Failure -> _state.update { it.copy(message = check.error.userMessage()) }
            is Outcome.Success -> authenticate { graph.authGateway.signInWithEmail(email, form.password) }
        }
    }

    fun register() {
        val form = _state.value.register
        val errors = validator.register(form.name, form.phone, form.email, form.password, form.confirmation)
        _state.update { it.copy(register = form.copy(errors = errors)) }
        if (!errors.isValid) return
        val name = form.name.trim()
        val phone = form.phone.filter { it.isDigit() }
        val email = form.email.trim()
        when (val check = graph.authService.validateNewAccount(name, phone, email, form.password)) {
            is Outcome.Failure -> _state.update { it.copy(message = check.error.userMessage()) }
            is Outcome.Success -> authenticate {
                graph.authGateway.registerWithEmail(name, phone, email, form.password)
            }
        }
    }

    fun signInWithGoogle(idToken: String) = authenticate { graph.authGateway.signInWithGoogle(idToken) }

    fun showMessage(message: String) = _state.update { it.copy(message = message) }

    /** Opens the reset dialog prefilled with the email typed in the login form. */
    fun openPasswordReset() = _state.update { it.copy(reset = PasswordResetForm(email = it.login.email.trim())) }

    fun updateResetEmail(email: String) =
        _state.update { state -> state.copy(reset = state.reset?.copy(email = email, error = null)) }

    fun closePasswordReset() = _state.update { it.copy(reset = null) }

    fun consumeNotice() = _state.update { it.copy(notice = null) }

    fun sendPasswordReset() {
        val form = _state.value.reset ?: return
        if (form.busy) return
        val email = form.email.trim()
        val error = validator.email(email)
        if (error != null) {
            _state.update { it.copy(reset = form.copy(error = error)) }
            return
        }
        when (val check = graph.authService.validateEmailAddress(email)) {
            is Outcome.Failure -> {
                _state.update { it.copy(reset = form.copy(error = check.error.userMessage())) }
                return
            }
            is Outcome.Success -> Unit
        }
        _state.update { it.copy(reset = form.copy(busy = true, error = null)) }
        viewModelScope.launch {
            try {
                graph.authGateway.sendPasswordReset(email)
                _state.update {
                    it.copy(reset = null, notice = "Le enviamos un enlace para restablecer su contraseña a $email.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                graph.errorLogger.warn("AuthViewModel", e::class.simpleName ?: "Exception", e.message.orEmpty())
                _state.update { state -> state.copy(reset = state.reset?.copy(busy = false, error = authErrorMessage(e))) }
            }
        }
    }

    fun consumeAuthenticated() = _state.update { it.copy(authenticatedUid = null) }

    private fun authenticate(block: suspend () -> String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val uid = block()
                _state.update { it.copy(busy = false, authenticatedUid = uid) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                graph.errorLogger.warn("AuthViewModel", e::class.simpleName ?: "Exception", e.message.orEmpty())
                _state.update { it.copy(busy = false, message = authErrorMessage(e)) }
            }
        }
    }
}
