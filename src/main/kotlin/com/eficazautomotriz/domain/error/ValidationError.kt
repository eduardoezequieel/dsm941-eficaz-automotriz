package com.eficazautomotriz.domain.error

import com.eficazautomotriz.domain.validation.TextField
import com.eficazautomotriz.domain.validation.TextFields
import java.net.URI
import java.time.Year

private val PLATE_FIELD = TextField(name = "plate", label = "placa", maxLength = 9)
private val MILEAGE_FIELD = TextField(name = "mileage", label = "kilometraje", maxLength = 0)
private val YEAR_FIELD = TextField(name = "year", label = "anio", maxLength = 0)
private val DATE_FIELD = TextField(name = "date", label = "fecha", maxLength = 10)
private val SLOT_CAPACITY_FIELD = TextField(name = "slotCapacity", label = "capacidad de franja", maxLength = 0)

private val SALVADORAN_PLATE_FORMAT = Regex("^[A-Z]{1,3}[0-9]{3}[A-Z0-9]{3}$")

sealed class ValidationError(
    val field: String,
    val label: String,
    val message: String
) {
    object EmptyPlate : ValidationError(
        field = PLATE_FIELD.name,
        label = PLATE_FIELD.label,
        message = "La placa es obligatoria."
    )

    object PlateContainsSpaces : ValidationError(
        field = PLATE_FIELD.name,
        label = PLATE_FIELD.label,
        message = "La placa no debe contener espacios."
    )

    object InvalidSalvadoranPlateFormat : ValidationError(
        field = PLATE_FIELD.name,
        label = PLATE_FIELD.label,
        message = "La placa debe tener de 1 a 3 letras, 3 numeros y 3 caracteres alfanumericos."
    )

    object DuplicatePlate : ValidationError(
        field = PLATE_FIELD.name,
        label = PLATE_FIELD.label,
        message = "La placa ya esta registrada."
    )

    class MileageOutOfRange(
        min: Int,
        max: Int
    ) : ValidationError(
        field = MILEAGE_FIELD.name,
        label = MILEAGE_FIELD.label,
        message = "El kilometraje debe estar entre $min y $max."
    )

    class YearOutOfRange(
        min: Int,
        max: Int
    ) : ValidationError(
        field = YEAR_FIELD.name,
        label = YEAR_FIELD.label,
        message = "El anio debe estar entre $min y $max."
    )

    class EmptyText(
        textField: TextField
    ) : ValidationError(
        field = textField.name,
        label = textField.label,
        message = "El campo ${textField.label} es obligatorio."
    )

    class TextTooLong(
        textField: TextField
    ) : ValidationError(
        field = textField.name,
        label = textField.label,
        message = "El campo ${textField.label} no debe superar ${textField.maxLength} caracteres."
    )

    class UnsupportedCharacters(
        textField: TextField
    ) : ValidationError(
        field = textField.name,
        label = textField.label,
        message = "El campo ${textField.label} contiene caracteres no permitidos. ${textField.allowedHint}"
    )

    class PastDate(
        textField: TextField = DATE_FIELD
    ) : ValidationError(
        field = textField.name,
        label = textField.label,
        message = "La fecha no puede estar en el pasado."
    )

    object InvalidImageUrl : ValidationError(
        field = TextFields.IMAGE_URL.name,
        label = TextFields.IMAGE_URL.label,
        message = "La direccion de la imagen debe ser una URL valida."
    )

    class SlotCapacityOutOfRange(
        min: Int,
        max: Int
    ) : ValidationError(
        field = SLOT_CAPACITY_FIELD.name,
        label = SLOT_CAPACITY_FIELD.label,
        message = "La capacidad de franja debe estar entre $min y $max."
    )

    companion object {
        fun validarPlaca(
            placa: String,
            placaDuplicada: Boolean = false
        ): ValidationError? {
            val valor = placa.trim()

            if (valor.isEmpty()) {
                return EmptyPlate
            }

            if (placa.any { it.isWhitespace() }) {
                return PlateContainsSpaces
            }

            if (!SALVADORAN_PLATE_FORMAT.matches(valor.uppercase())) {
                return InvalidSalvadoranPlateFormat
            }

            if (placaDuplicada) {
                return DuplicatePlate
            }

            return null
        }

        fun validarTextoObligatorio(
            textField: TextField,
            valor: String
        ): ValidationError? {
            if (valor.isBlank()) {
                return EmptyText(textField)
            }

            return validarTextoOpcional(textField, valor)
        }

        fun validarTextoOpcional(
            textField: TextField,
            valor: String?
        ): ValidationError? {
            if (valor.isNullOrBlank()) {
                return null
            }

            if (valor.length > textField.maxLength) {
                return TextTooLong(textField)
            }

            val allowed = textField.allowed
            if (allowed != null && !allowed.matches(valor)) {
                return UnsupportedCharacters(textField)
            }

            return null
        }

        fun validarKilometraje(
            kilometraje: Int,
            min: Int = 0,
            max: Int = 1_000_000
        ): ValidationError? {
            return if (kilometraje in min..max) null else MileageOutOfRange(min, max)
        }

        fun validarAnio(
            anio: Int,
            min: Int = 1900,
            max: Int = Year.now().value + 1
        ): ValidationError? {
            return if (anio in min..max) null else YearOutOfRange(min, max)
        }

        fun validarUrlImagen(url: String?): ValidationError? {
            val textError = validarTextoOpcional(TextFields.IMAGE_URL, url)
            if (textError != null) {
                return textError
            }

            val valor = url?.trim().orEmpty()
            if (valor.isEmpty()) {
                return null
            }

            val uri = runCatching { URI(valor) }.getOrNull()
            val scheme = uri?.scheme?.lowercase()

            if (uri == null || scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
                return InvalidImageUrl
            }

            return null
        }

        fun validarCapacidadFranja(
            capacidad: Int,
            min: Int = 1,
            max: Int = 20
        ): ValidationError? {
            return if (capacidad in min..max) null else SlotCapacityOutOfRange(min, max)
        }
    }
}
