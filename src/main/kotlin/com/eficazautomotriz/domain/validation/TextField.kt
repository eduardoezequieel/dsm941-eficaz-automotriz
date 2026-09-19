package com.eficazautomotriz.domain.validation

/**
 * Regla de un campo de texto: como se le nombra al usuario, cuanto admite y que
 * caracteres. La consola la usa para rechazar al teclear y los casos de uso para
 * revalidar, de modo que el limite sea uno solo.
 */
data class TextField(
    val name: String,
    val label: String,
    val maxLength: Int,
    val allowed: Regex? = null,
    val allowedHint: String = "",
)

/** Catalogo de los campos de texto que el usuario puede teclear. */
object TextFields {

    /** Letras con tilde, digitos y la puntuacion que aparece en marcas y modelos reales. */
    private val NAME_CHARACTERS = Regex("^[\\p{L}\\p{N} .,'&/-]+$")

    private const val NAME_HINT = "Use letras, numeros, espacios y los signos . , ' & / -"

    val MAKE = TextField("make", "marca", maxLength = 30, allowed = NAME_CHARACTERS, allowedHint = NAME_HINT)

    val MODEL = TextField("model", "modelo", maxLength = 30, allowed = NAME_CHARACTERS, allowedHint = NAME_HINT)

    val REASON = TextField("reason", "motivo", maxLength = 200)

    val NOTES = TextField("notes", "notas de recepcion", maxLength = 500)

    val IMAGE_URL = TextField("imageUrl", "direccion de la imagen", maxLength = 300)
}
