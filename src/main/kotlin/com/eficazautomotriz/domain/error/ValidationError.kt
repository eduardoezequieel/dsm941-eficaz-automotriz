package com.eficazautomotriz.domain.error

import com.eficazautomotriz.domain.validation.TextField
import com.eficazautomotriz.domain.validation.TextFields

/**
 * Errores de validacion sobre un campo concreto. La consola los usa para rechazar al
 * teclear y los casos de uso para revalidar lo que reciben: mismas reglas, un solo lugar.
 *
 * `field` es el nombre tecnico para el log; `label` es como se le nombra al usuario.
 */
sealed class ValidationError(val field: String, val label: String, val message: String) {

    data object BlankPlate :
        ValidationError("plate", "placa", "La placa no puede estar vacia.")

    data object PlateWithSpaces :
        ValidationError("plate", "placa", "La placa no puede contener espacios.")

    data object InvalidPlateFormat :
        ValidationError(
            "plate",
            "placa",
            "La placa debe tener formato de El Salvador: 1 a 3 letras de tipo, 3 numeros y 3 caracteres alfanumericos (ejemplo P123456).",
        )

    data class DuplicatePlate(val plate: String) :
        ValidationError("plate", "placa", "Ya existe un vehiculo registrado con la placa $plate.")

    data class MileageOutOfRange(val value: Int, val max: Int) :
        ValidationError("currentMileage", "kilometraje", "El kilometraje debe estar entre 0 y $max: $value.")

    data class YearOutOfRange(val value: Int, val min: Int, val max: Int) :
        ValidationError("year", "año", "El año $value esta fuera del rango permitido ($min-$max).")

    data class BlankText(val text: TextField) :
        ValidationError(text.name, text.label, "El valor no puede quedar vacio.")

    data class TextTooLong(val text: TextField, val length: Int) :
        ValidationError(
            text.name,
            text.label,
            "El texto admite ${text.maxLength} caracteres como maximo; escribio $length.",
        )

    data class InvalidCharacters(val text: TextField) :
        ValidationError(text.name, text.label, "El texto tiene caracteres no permitidos. ${text.allowedHint}")

    data object DateInPast :
        ValidationError("date", "fecha", "La fecha debe ser igual o posterior a hoy.")

    data object InvalidImageUrl :
        ValidationError("imageUrl", "direccion de la imagen", "La direccion debe iniciar con http:// o https://.")

    data class SlotCapacityOutOfRange(val value: Int, val max: Int) :
        ValidationError("capacity", "capacidad", "La capacidad de la franja debe estar entre 0 y $max: $value.")

    /** Comprobaciones puras y sin dependencias. Viajan a la Etapa 3 con el resto del dominio. */
    companion object {

        fun validatePlate(plate: String): ValidationError? = when {
            plate.isBlank() -> BlankPlate
            plate.any { it.isWhitespace() } -> PlateWithSpaces
            !SALVADORAN_PLATE.matches(plate.uppercase()) -> InvalidPlateFormat
            else -> null
        }

        /** Campo de texto obligatorio: presente, dentro del limite y con caracteres permitidos. */
        fun validateText(field: TextField, value: String): ValidationError? = when {
            value.isBlank() -> BlankText(field)
            value.length > field.maxLength -> TextTooLong(field, value.length)
            field.allowed?.matches(value) == false -> InvalidCharacters(field)
            else -> null
        }

        /** Igual que `validateText`, pero vacio es una respuesta valida: el usuario lo omitio. */
        fun validateOptionalText(field: TextField, value: String): ValidationError? =
            if (value.isBlank()) null else validateText(field, value)

        fun validateMileage(mileage: Int, maxMileage: Int): ValidationError? =
            if (mileage in 0..maxMileage) null else MileageOutOfRange(mileage, maxMileage)

        fun validateYear(year: Int, minYear: Int, maxYear: Int): ValidationError? =
            if (year in minYear..maxYear) null else YearOutOfRange(year, minYear, maxYear)

        fun validateImageUrl(url: String): ValidationError? =
            validateText(TextFields.IMAGE_URL, url)
                ?: if (url.startsWith("http://") || url.startsWith("https://")) null else InvalidImageUrl

        /** `0` es valido: significa «usar la capacidad por omision del sistema». */
        fun validateSlotCapacity(capacity: Int, maxCapacity: Int): ValidationError? =
            if (capacity in 0..maxCapacity) null else SlotCapacityOutOfRange(capacity, maxCapacity)

        /**
         * Placa salvadoreña compacta: letras de tipo, tres digitos y tres alfanumericos.
         * Cubre las numericas (P123456) y las alfanumericas desde 2021 (P12300A).
         */
        private val SALVADORAN_PLATE = Regex("^[A-Z]{1,3}[0-9]{3}[0-9A-Z]{3}$")
    }
}
