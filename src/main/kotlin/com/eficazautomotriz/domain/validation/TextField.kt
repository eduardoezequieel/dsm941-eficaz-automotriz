package com.eficazautomotriz.domain.validation

// Describes the reusable validation settings for a text field.
data class TextField(
    val name: String,
    val label: String,
    val maxLength: Int,
    val allowed: Regex? = null,
    val allowedHint: String = ""
)

// Centralizes text field configurations used across the domain.
object TextFields {
    private const val VEHICLE_TEXT_ALLOWED_HINT =
        "Permite letras, numeros, espacios y los caracteres . , ' & / -"

    private val VEHICLE_TEXT_ALLOWED = Regex("^[\\p{L}\\p{N} .,'&/-]+$")

    val MAKE = TextField(
        name = "make",
        label = "marca",
        maxLength = 30,
        allowed = VEHICLE_TEXT_ALLOWED,
        allowedHint = VEHICLE_TEXT_ALLOWED_HINT
    )

    val MODEL = TextField(
        name = "model",
        label = "modelo",
        maxLength = 30,
        allowed = VEHICLE_TEXT_ALLOWED,
        allowedHint = VEHICLE_TEXT_ALLOWED_HINT
    )

    val REASON = TextField(
        name = "reason",
        label = "motivo",
        maxLength = 200
    )

    val NOTES = TextField(
        name = "notes",
        label = "notas de recepcion",
        maxLength = 500
    )

    val IMAGE_URL = TextField(
        name = "imageUrl",
        label = "direccion de la imagen",
        maxLength = 300
    )
}
