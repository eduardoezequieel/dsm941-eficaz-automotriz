package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.errorOrNull
import com.eficazautomotriz.domain.validation.TextFields
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Account fields revalidated in core, with the same limits the form uses. */
class AuthServiceTest {

    private fun auth(): AuthService {
        val env = TestEnvironment()
        return AuthService(env.repositories.users, env.logger)
    }

    @Test
    fun `a new account accepts a salvadoran phone and a name with accents`() {
        val result = auth().validateNewAccount("José Peña-López", "71234567", "  ana@ejemplo.com  ", "secreto")
        assertInstanceOf(Outcome.Success::class.java, result)
        assertNull(AccountValidation.validatePhone("22501188"))
        assertNull(AccountValidation.validatePhone("61234567"))
        assertNull(AccountValidation.validateName("Ña"))
        assertNull(AccountValidation.validateName("  O'Brien  "))
    }

    @Test
    fun `name needs two letters and stays inside the shared limit`() {
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validateName("a"))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validateName("..."))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validateName("A."))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validateName("Ana2"))
        assertInstanceOf(ValidationError.BlankText::class.java, AccountValidation.validateName("   "))

        val tooLong = "A".repeat(TextFields.PERSON_NAME.maxLength + 1)
        val length = assertInstanceOf(ValidationError.TextTooLong::class.java, AccountValidation.validateName(tooLong))
        assertEquals(TextFields.PERSON_NAME.maxLength, length.text.maxLength)
        assertNull(AccountValidation.validateName("A".repeat(TextFields.PERSON_NAME.maxLength)))
    }

    @Test
    fun `phone is eight digits starting with 2, 6 or 7`() {
        assertInstanceOf(ValidationError.BlankText::class.java, AccountValidation.validatePhone("  "))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validatePhone("81234567"))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validatePhone("7123456"))
        assertInstanceOf(ValidationError.TextTooLong::class.java, AccountValidation.validatePhone("712345678"))
        assertInstanceOf(ValidationError.TextTooLong::class.java, AccountValidation.validatePhone("7012-3456"))
        val invalid = assertInstanceOf(
            DomainError.Invalid::class.java,
            auth().validateProfile("Ana Melara", "81234567").errorOrNull(),
        )
        assertEquals("phone", invalid.validation.field)
        assertInstanceOf(Outcome.Success::class.java, auth().validateProfile("Ana Melara", "22501188"))
    }

    @Test
    fun `email is trimmed, has no spaces and stops at 254 characters`() {
        assertNull(AccountValidation.validateEmail("  ana@ejemplo.com  "))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validateEmail("ana @ejemplo.com"))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validateEmail("ana-sin-arroba"))
        assertInstanceOf(ValidationError.BlankText::class.java, AccountValidation.validateEmail("   "))

        val atLimit = "a".repeat(TextFields.EMAIL.maxLength - "@b.co".length) + "@b.co"
        assertEquals(TextFields.EMAIL.maxLength, atLimit.length)
        assertNull(AccountValidation.validateEmail(atLimit))
        val tooLong = assertInstanceOf(ValidationError.TextTooLong::class.java, AccountValidation.validateEmail(atLimit + "x"))
        assertEquals(TextFields.EMAIL.maxLength, tooLong.text.maxLength)

        val rejected = assertInstanceOf(
            DomainError.Invalid::class.java,
            auth().validateEmailAddress("ana @ejemplo.com").errorOrNull(),
        )
        assertEquals("email", rejected.validation.field)
    }

    @Test
    fun `password allows 6 to 64 characters`() {
        assertInstanceOf(ValidationError.BlankText::class.java, AccountValidation.validatePassword(""))
        assertInstanceOf(ValidationError.InvalidCharacters::class.java, AccountValidation.validatePassword("12345"))
        assertNull(AccountValidation.validatePassword("123456"))
        assertNull(AccountValidation.validatePassword("a".repeat(TextFields.PASSWORD.maxLength)))
        val tooLong = assertInstanceOf(
            ValidationError.TextTooLong::class.java,
            AccountValidation.validatePassword("a".repeat(TextFields.PASSWORD.maxLength + 1)),
        )
        assertEquals(TextFields.PASSWORD.maxLength, tooLong.text.maxLength)
        assertInstanceOf(Outcome.Success::class.java, auth().validateSignIn("ana@ejemplo.com", "secreto"))
        assertInstanceOf(
            DomainError.Invalid::class.java,
            auth().validateSignIn("ana@ejemplo.com", "12345").errorOrNull(),
        )
    }
}
