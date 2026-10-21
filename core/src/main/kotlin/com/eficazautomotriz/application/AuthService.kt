package com.eficazautomotriz.application

import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.validation.TextFields
import com.eficazautomotriz.logging.ErrorLogger

/**
 * Selection of the session user, plus the account-field checks the sign-up form
 * already ran. Firebase Authentication still creates the credential; this service
 * revalidates the same limits before that call.
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

    /** Email and password of the login form. */
    fun validateSignIn(email: String, password: String): Outcome<Unit> = accept(
        "validateSignIn",
        AccountValidation.validateEmail(email) ?: AccountValidation.validatePassword(password),
        email,
    )

    /** Email of the password-reset dialog. Same rule as the login email. */
    fun validateEmailAddress(email: String): Outcome<Unit> =
        accept("validateEmailAddress", AccountValidation.validateEmail(email), email)

    /** Name, phone, email and password of sign-up. The confirmation is a form-only check. */
    fun validateNewAccount(name: String, phone: String, email: String, password: String): Outcome<Unit> =
        accept(
            "validateNewAccount",
            AccountValidation.validateName(name)
                ?: AccountValidation.validatePhone(phone)
                ?: AccountValidation.validateEmail(email)
                ?: AccountValidation.validatePassword(password),
            email,
        )

    /** Name and phone, for a profile edit that does not touch the credential. */
    fun validateProfile(name: String, phone: String): Outcome<Unit> =
        accept(
            "validateProfile",
            AccountValidation.validateName(name) ?: AccountValidation.validatePhone(phone),
            "",
        )

    private fun accept(operation: String, error: ValidationError?, email: String): Outcome<Unit> {
        if (error == null) return Outcome.success(Unit)
        val data = if (email.isBlank()) "" else "email=${email.trim()}"
        return failInvalid(operation, error, data)
    }
}

/**
 * Rules shared by the form and by [AuthService]. Phone is stored as 8 digits;
 * rows saved earlier may still carry the hyphen (`7012-3456`) and are not rewritten here.
 */
object AccountValidation {

    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_NAME_LETTERS = 2

    private val EMAIL_SHAPE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(raw: String): String = raw.trim()

    fun validateEmail(raw: String): ValidationError? {
        val email = normalizeEmail(raw)
        return when {
            email.isBlank() -> ValidationError.BlankText(TextFields.EMAIL)
            email.length > TextFields.EMAIL.maxLength -> ValidationError.TextTooLong(TextFields.EMAIL, email.length)
            email.any { it.isWhitespace() } -> ValidationError.InvalidCharacters(TextFields.EMAIL)
            !EMAIL_SHAPE.matches(email) -> ValidationError.InvalidCharacters(TextFields.EMAIL)
            else -> null
        }
    }

    /** Not trimmed: a space can be part of a password. */
    fun validatePassword(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.BlankText(TextFields.PASSWORD)
        password.length < MIN_PASSWORD_LENGTH -> ValidationError.InvalidCharacters(TextFields.PASSWORD)
        password.length > TextFields.PASSWORD.maxLength ->
            ValidationError.TextTooLong(TextFields.PASSWORD, password.length)
        else -> null
    }

    fun validateName(raw: String): ValidationError? {
        val name = raw.trim()
        ValidationError.validateText(TextFields.PERSON_NAME, name)?.let { return it }
        val letters = name.count { it.isLetter() }
        return if (letters < MIN_NAME_LETTERS) ValidationError.InvalidCharacters(TextFields.PERSON_NAME) else null
    }

    /** Stored form: exactly 8 digits starting with 2, 6 or 7. The hyphen is display-only. */
    fun validatePhone(raw: String): ValidationError? {
        val phone = raw.trim()
        if (phone.isEmpty()) return ValidationError.BlankText(TextFields.PHONE)
        return ValidationError.validateText(TextFields.PHONE, phone)
    }
}
