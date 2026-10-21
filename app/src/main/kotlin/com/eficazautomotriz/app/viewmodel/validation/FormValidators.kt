package com.eficazautomotriz.app.viewmodel.validation

import com.eficazautomotriz.application.AccountValidation
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.validation.InputParsers
import com.eficazautomotriz.domain.validation.TextFields
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pure form validation (no Android): early feedback under each field. The core use cases
 * validate again with the same rules, so nothing is skipped. Messages are UI text (Spanish).
 */

/**
 * Account fields. The limits live in [TextFields] so the screen, this validator and
 * [AccountValidation] share one rule.
 */
object ProfileFields {
    val NAME = TextFields.PERSON_NAME
    val PHONE = TextFields.PHONE
    val EMAIL = TextFields.EMAIL
    val PASSWORD = TextFields.PASSWORD
}

const val MIN_PASSWORD_LENGTH = AccountValidation.MIN_PASSWORD_LENGTH

data class FieldErrors(val byField: Map<String, String> = emptyMap()) {
    val isValid: Boolean get() = byField.isEmpty()
    operator fun get(field: String): String? = byField[field]
}

private fun MutableMap<String, String>.check(field: String, error: String?) {
    if (error != null) put(field, error)
}

/** `isEmail` is injected: `Patterns.EMAIL_ADDRESS` on Android, a regular expression in tests. */
class AuthFormValidator(private val isEmail: (String) -> Boolean) {

    fun login(email: String, password: String): FieldErrors = FieldErrors(
        buildMap {
            check("email", emailError(email))
            check("password", passwordError(password))
        }
    )

    fun register(name: String, phone: String, email: String, password: String, confirmation: String): FieldErrors =
        FieldErrors(
            buildMap {
                check("name", nameError(name))
                check("phone", phoneError(phone))
                check("email", emailError(email))
                check("password", passwordError(password))
                check("confirmation", if (confirmation != password) "Las contraseñas no coinciden." else null)
            }
        )

    /** Only the email (password reset dialog). */
    fun email(email: String): String? = emailError(email)

    private fun emailError(email: String): String? {
        val trimmed = AccountValidation.normalizeEmail(email)
        val core = AccountValidation.validateEmail(email)
        if (core == null) {
            return if (isEmail(trimmed)) null else "Correo inválido. Ejemplo: nombre@dominio.com"
        }
        return when (core) {
            is ValidationError.BlankText -> "Escriba su correo."
            is ValidationError.TextTooLong ->
                "El correo admite ${ProfileFields.EMAIL.maxLength} caracteres como máximo."
            is ValidationError.InvalidCharacters ->
                if (trimmed.any { it.isWhitespace() }) "El correo no puede contener espacios."
                else "Correo inválido. Ejemplo: nombre@dominio.com"
            else -> core.message
        }
    }

    private fun passwordError(password: String): String? {
        if (AccountValidation.validatePassword(password) == null) return null
        return when {
            password.isEmpty() -> "Escriba su contraseña."
            password.length < MIN_PASSWORD_LENGTH ->
                "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
            else -> "La contraseña admite ${ProfileFields.PASSWORD.maxLength} caracteres como máximo."
        }
    }

    private fun nameError(name: String): String? {
        val trimmed = name.trim()
        if (AccountValidation.validateName(name) == null) return null
        return when {
            trimmed.isBlank() -> "Escriba su nombre."
            trimmed.length > ProfileFields.NAME.maxLength ->
                "El nombre admite ${ProfileFields.NAME.maxLength} caracteres como máximo."
            ProfileFields.NAME.allowed?.matches(trimmed) == false ->
                "Use solo letras, espacios y los signos . ' -."
            else -> "El nombre debe incluir al menos ${AccountValidation.MIN_NAME_LETTERS} letras."
        }
    }

    private fun phoneError(phone: String): String? {
        if (AccountValidation.validatePhone(phone) == null) return null
        return if (phone.isBlank()) "Escriba su teléfono."
        else "Ingrese un teléfono de 8 dígitos que inicie con 2, 6 o 7. Ejemplo: 7123-4567."
    }
}

/** Parsed values of a valid vehicle registration. */
data class VehicleInput(val plate: String, val make: String, val model: String, val year: Int, val mileage: Int)

object VehicleFormValidator {

    fun validate(
        plate: String, make: String, model: String, year: String, mileage: String,
        config: SystemConfig, today: LocalDate,
    ): Pair<FieldErrors, VehicleInput?> {
        val normalizedPlate = ValidationError.normalizePlate(plate)
        val trimmedYear = year.trim()
        val parsedYear = InputParsers.parseInt(trimmedYear)
        val parsedMileage = InputParsers.parseInt(mileage)
        val errors = buildMap {
            check("plate", ValidationError.validatePlate(normalizedPlate)?.message)
            check("make", ValidationError.validateText(TextFields.MAKE, make.trim())?.message)
            check("model", ValidationError.validateText(TextFields.MODEL, model.trim())?.message)
            check(
                "year",
                if (parsedYear == null || trimmedYear.length != TextFields.YEAR.maxLength) "Escriba el año con cuatro dígitos."
                else ValidationError.validateYear(parsedYear, config.minVehicleYear, today.year + 1)?.message,
            )
            check(
                "mileage",
                if (parsedMileage == null) "Escriba el kilometraje como número entero."
                else ValidationError.validateMileage(parsedMileage, config.maxMileage)?.message,
            )
        }
        val input = if (errors.isEmpty()) {
            VehicleInput(normalizedPlate, make.trim(), model.trim(), parsedYear!!, parsedMileage!!)
        } else {
            null
        }
        return FieldErrors(errors) to input
    }
}

object MileageValidator {

    /**
     * Characters needed to type an integer up to [ceiling], including a thousands comma
     * every three digits (`2,000,000` is 9). The field cap and the use case share [ceiling].
     */
    fun inputMaxLength(ceiling: Int): Int {
        val digits = ceiling.coerceAtLeast(0).toString().length
        return digits + (digits - 1) / 3
    }

    /** Early warning: the service also rejects a decreasing odometer. */
    fun validate(raw: String, lastKnown: Int, config: SystemConfig): Pair<String?, Int?> {
        val value = InputParsers.parseInt(raw) ?: return "Escriba el kilometraje como número entero." to null
        ValidationError.validateMileage(value, config.maxMileage)?.let { return it.message to null }
        if (value < lastKnown) return "El kilometraje no puede ser menor que el último registrado ($lastKnown km)." to null
        return null to value
    }
}

object NewAppointmentValidator {

    fun lastSchedulableDate(today: LocalDate, config: SystemConfig): LocalDate =
        today.plusYears(config.schedulingHorizonYears)

    fun isSchedulable(date: LocalDate, today: LocalDate, config: SystemConfig): Boolean =
        !date.isBefore(today) && !date.isAfter(lastSchedulableDate(today, config))

    fun dateError(date: LocalDate?, today: LocalDate, config: SystemConfig): String? = when {
        date == null -> "Elija una fecha."
        date.isBefore(today) -> "La fecha no puede estar en el pasado."
        date.isAfter(lastSchedulableDate(today, config)) ->
            "Solo se puede agendar hasta el ${lastSchedulableDate(today, config)}."
        else -> null
    }

    /** The request is incomplete until the client picks a free slot on the chosen day. */
    fun slotError(slotSelected: Boolean): String? =
        if (slotSelected) null else "Elija una franja horaria."

    /**
     * The Material 3 DatePicker works with UTC-midnight milliseconds; converting with the
     * device zone would shift the date by one day in El Salvador (UTC-6).
     */
    fun dateFromPickerMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun pickerMillisOf(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

object ReasonValidator {
    /** Mandatory reason for rejecting or rescheduling (same rule as the use case). */
    fun error(reason: String): String? = ValidationError.validateText(TextFields.REASON, reason.trim())?.message
}

object ReportPeriodValidator {

    /** Initial report period: from the first day of the current month to today. */
    fun defaultPeriod(today: LocalDate): Pair<LocalDate, LocalDate> = today.withDayOfMonth(1) to today

    fun earliestDate(today: LocalDate, config: SystemConfig): LocalDate = today.minusYears(config.reportHistoryYears)

    /** Future appointments count too, up to the scheduling horizon. */
    fun latestDate(today: LocalDate, config: SystemConfig): LocalDate = NewAppointmentValidator.lastSchedulableDate(today, config)

    fun error(from: LocalDate, to: LocalDate, today: LocalDate, config: SystemConfig): String? = when {
        from.isAfter(to) -> "La fecha inicial debe ser anterior o igual a la final."
        from.isBefore(earliestDate(today, config)) ->
            "Solo se conservan ${config.reportHistoryYears} años de historial: elija desde ${earliestDate(today, config)}."
        to.isAfter(latestDate(today, config)) -> "La fecha final no puede pasar del ${latestDate(today, config)}."
        else -> null
    }
}

/** Parsed service-catalog row. Description may be empty. */
data class ServiceTypeInput(val name: String, val description: String)

/** Parsed maintenance-catalog row. */
data class MaintenanceTypeInput(val name: String, val intervalKm: Int)

/**
 * Early checks for the staff catalog dialogs. The use case repeats the same rules,
 * so a bypassed form still cannot store a blank name or a non-positive interval.
 */
object CatalogValidator {

    fun serviceType(name: String, description: String): Pair<FieldErrors, ServiceTypeInput?> {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim()
        val errors = buildMap {
            check("name", ValidationError.validateText(TextFields.CATALOG_NAME, trimmedName)?.message)
            check("description", ValidationError.validateOptionalText(TextFields.CATALOG_DESCRIPTION, trimmedDescription)?.message)
        }
        val input = if (errors.isEmpty()) ServiceTypeInput(trimmedName, trimmedDescription) else null
        return FieldErrors(errors) to input
    }

    fun maintenanceType(
        name: String,
        intervalKm: String,
        config: SystemConfig,
    ): Pair<FieldErrors, MaintenanceTypeInput?> {
        val trimmedName = name.trim()
        val parsed = InputParsers.parseInt(intervalKm)
        val errors = buildMap {
            check("name", ValidationError.validateText(TextFields.CATALOG_NAME, trimmedName)?.message)
            check(
                "intervalKm",
                if (parsed == null) "Escriba el intervalo en kilómetros como número entero."
                else ValidationError.validateIntervalKm(parsed, config.maxMileage)?.message,
            )
        }
        val input = if (errors.isEmpty()) MaintenanceTypeInput(trimmedName, parsed!!) else null
        return FieldErrors(errors) to input
    }
}
