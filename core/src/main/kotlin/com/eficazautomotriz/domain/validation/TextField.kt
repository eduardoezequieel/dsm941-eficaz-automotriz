package com.eficazautomotriz.domain.validation

/**
 * Rule for a text field: how it is named for the user, how much it accepts, and which
 * characters. The console uses it to reject input while typing and the use cases use it to
 * revalidate, so there is a single limit.
 */
data class TextField(
    val name: String,
    val label: String,
    val maxLength: Int,
    val allowed: Regex? = null,
    val allowedHint: String = "",
)

/** Catalog of the text fields the user can type into. */
object TextFields {

    /** Letters with accents, digits, and the punctuation that shows up in real makes and models. */
    private val NAME_CHARACTERS = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'&/-]*$")

    private const val NAME_HINT = "Inicie con una letra o un número; use letras, números, espacios y los signos . , ' & / -"

    val MAKE = TextField("make", "marca", maxLength = 30, allowed = NAME_CHARACTERS, allowedHint = NAME_HINT)

    val MODEL = TextField("model", "modelo", maxLength = 30, allowed = NAME_CHARACTERS, allowedHint = NAME_HINT)

    val REASON = TextField("reason", "motivo", maxLength = 200)

    val NOTES = TextField("notes", "notas de recepción", maxLength = 500)

    val IMAGE_URL = TextField("imageUrl", "dirección de la imagen", maxLength = 300)

    /** Service and maintenance catalog name. Same character set as make and model. */
    val CATALOG_NAME = TextField(
        "name",
        "nombre",
        maxLength = 60,
        allowed = NAME_CHARACTERS,
        allowedHint = NAME_HINT,
    )

    /** Optional service description. Blank is valid; the limit stops a pasted essay. */
    val CATALOG_DESCRIPTION = TextField("description", "descripción", maxLength = 200)

    /**
     * Person name on the account. Letters (with accents), spaces, and . ' -
     * At least two letters is checked separately: "a" and "..." match the character set.
     */
    val PERSON_NAME = TextField(
        "name",
        "nombre",
        maxLength = 60,
        allowed = Regex("^[\\p{L} .'-]+$"),
        allowedHint = "Use letras, espacios y los signos . ' -",
    )

    /**
     * Salvadoran phone stored as 8 digits starting with 2, 6 or 7.
     * The screen draws the hyphen (7123-4567); it is not part of the stored value.
     */
    val PHONE = TextField(
        "phone",
        "teléfono",
        maxLength = 8,
        allowed = Regex("^[267][0-9]{7}$"),
        allowedHint = "Use 8 dígitos que inicien con 2, 6 o 7 (ejemplo 7123-4567).",
    )

    /** Email address. 254 is the longest an address can be. Spaces are never part of it. */
    val EMAIL = TextField(
        "email",
        "correo",
        maxLength = 254,
        allowedHint = "Sin espacios. Ejemplo: nombre@dominio.com",
    )

    /** Password length cap. The minimum of 6 is checked beside this limit. */
    val PASSWORD = TextField(
        "password",
        "contraseña",
        maxLength = 64,
        allowedHint = "Entre 6 y 64 caracteres.",
    )

    /** Model year typed as four digits. validateYear still checks the configured range. */
    val YEAR = TextField("year", "año", maxLength = 4, allowed = Regex("^\\d{4}$"), allowedHint = "Use cuatro dígitos.")

    /** Free-text search on a list (plate, make, model). It is not stored. */
    val SEARCH = TextField("search", "búsqueda", maxLength = 40)

    /**
     * Characters the mileage box can hold for [maxMileage]: its digits plus a thousands comma
     * every three digits. `2,000,000` is 9. The numeric ceiling is checked again on submit.
     */
    fun mileageMaxLength(maxMileage: Int): Int {
        val digits = maxMileage.coerceAtLeast(0).toString().length
        return digits + (digits - 1) / 3
    }

    /**
     * Reception search. It is not stored: it only narrows the vehicle list.
     * Long enough for a make, a space and a model, and for a plate typed with the hyphen.
     */
    val VEHICLE_SEARCH = TextField(
        "vehicleSearch",
        "búsqueda de vehículo",
        maxLength = MAKE.maxLength + 1 + MODEL.maxLength,
    )
}
