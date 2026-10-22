package com.eficazautomotriz.domain.error

import com.eficazautomotriz.domain.validation.TextField
import com.eficazautomotriz.domain.validation.TextFields

/**
 * Validation errors for one specific field. The console uses them to reject input while
 * typing, and the use cases use them to revalidate what they receive: the same rules, one place.
 *
 * `field` is the technical name for the log; `label` is how it is named for the user.
 */
sealed class ValidationError(val field: String, val label: String, val message: String) {

    data object BlankPlate :
        ValidationError("plate", "placa", "La placa no puede estar vacía.")

    data object PlateWithSpaces :
        ValidationError("plate", "placa", "La placa no puede contener espacios.")

    data object InvalidPlateFormat :
        ValidationError(
            "plate",
            "placa",
            "La placa debe iniciar con P y llevar 6 letras o números (ejemplos P123-456, P123-AB2, PA12-BB3).",
        )

    data class DuplicatePlate(val plate: String) :
        ValidationError("plate", "placa", "Ya existe un vehículo registrado con la placa $plate.")

    data class MileageOutOfRange(val value: Int, val max: Int) :
        ValidationError("currentMileage", "kilometraje", "El kilometraje debe estar entre 0 y $max: $value.")

    data class YearOutOfRange(val value: Int, val min: Int, val max: Int) :
        ValidationError("year", "año", "El año $value está fuera del rango permitido ($min-$max).")

    data class BlankText(val text: TextField) :
        ValidationError(text.name, text.label, "El valor no puede quedar vacío.")

    data class TextTooLong(val text: TextField, val length: Int) :
        ValidationError(
            text.name,
            text.label,
            "El texto admite ${text.maxLength} caracteres como máximo; escribió $length.",
        )

    data class InvalidCharacters(val text: TextField) :
        ValidationError(text.name, text.label, "El texto tiene caracteres no permitidos. ${text.allowedHint}")

    data object DateInPast :
        ValidationError("date", "fecha", "La fecha debe ser igual o posterior a hoy.")

    data object InvalidImageUrl :
        ValidationError("imageUrl", "dirección de la imagen", "La dirección debe iniciar con http:// o https://.")

    data class SlotCapacityOutOfRange(val value: Int, val max: Int) :
        ValidationError("capacity", "capacidad", "La capacidad de la franja debe estar entre 0 y $max: $value.")

    /** Maintenance interval: at least 1 km and never above the odometer ceiling. */
    data class IntervalOutOfRange(val value: Int, val max: Int) :
        ValidationError("intervalKm", "intervalo", "El intervalo debe estar entre 1 y $max km: $value.")

    /** Pure checks with no dependencies. They travel to stage 3 with the rest of the domain. */
    companion object {

        /** Stored form of a typed plate: uppercase, no surrounding spaces and no display hyphen. */
        fun normalizePlate(raw: String): String = raw.trim().uppercase().replace("-", "")

        fun validatePlate(plate: String): ValidationError? = when {
            plate.isBlank() -> BlankPlate
            plate.any { it.isWhitespace() } -> PlateWithSpaces
            !SALVADORAN_PLATE.matches(plate.uppercase()) -> InvalidPlateFormat
            else -> null
        }

        /** Required text field: present, within the limit, and made of allowed characters. */
        fun validateText(field: TextField, value: String): ValidationError? = when {
            value.isBlank() -> BlankText(field)
            value.length > field.maxLength -> TextTooLong(field, value.length)
            field.allowed?.matches(value) == false -> InvalidCharacters(field)
            else -> null
        }

        /** Same as `validateText`, except empty is a valid answer: the user left it out. */
        fun validateOptionalText(field: TextField, value: String): ValidationError? =
            if (value.isBlank()) null else validateText(field, value)

        fun validateMileage(mileage: Int, maxMileage: Int): ValidationError? =
            if (mileage in 0..maxMileage) null else MileageOutOfRange(mileage, maxMileage)

        fun validateYear(year: Int, minYear: Int, maxYear: Int): ValidationError? =
            if (year in minYear..maxYear) null else YearOutOfRange(year, minYear, maxYear)

        fun validateImageUrl(url: String): ValidationError? =
            validateText(TextFields.IMAGE_URL, url)
                ?: if (url.startsWith("http://") || url.startsWith("https://")) null else InvalidImageUrl

        /** `0` is valid: it means "use the system default capacity". */
        fun validateSlotCapacity(capacity: Int, maxCapacity: Int): ValidationError? =
            if (capacity in 0..maxCapacity) null else SlotCapacityOutOfRange(capacity, maxCapacity)

        /**
         * Kilometers between services. `0` and negatives are rejected here, as data,
         * so [com.eficazautomotriz.domain.rules.MaintenanceStatusRule] never sees them.
         */
        fun validateIntervalKm(intervalKm: Int, maxMileage: Int): ValidationError? =
            if (intervalKm in 1..maxMileage) null else IntervalOutOfRange(intervalKm, maxMileage)

        /** Compact plate length: the leading P plus six alphanumerics, stored without the hyphen. */
        const val PLATE_LENGTH = 7

        /**
         * Compact Salvadoran private-car plate: a leading P and six alphanumerics. It covers the
         * numeric plates (P123456, shown P123-456) and the alphanumeric ones (P123AB2, PA12BB3).
         */
        private val SALVADORAN_PLATE = Regex("^P[0-9A-Z]{6}$")
    }
}
