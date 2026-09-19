package com.eficazautomotriz.cli.io

/**
 * Unico punto del sistema autorizado a imprimir. El dominio y los repositorios
 * devuelven datos; la presentacion vive aqui.
 *
 * La salida entra por constructor para que las pruebas la capturen en vez de ensuciar
 * stdout. Escribe el texto exacto, con el salto de linea explicito: el prompt no lo lleva
 * porque el cursor debe quedar donde el usuario escribe.
 */
class ConsoleWriter(private val write: (String) -> Unit = ::print) {

    fun line(text: String = "") = write("$text\n")

    fun title(text: String) {
        line()
        line(text.uppercase())
        line("=".repeat(text.length.coerceAtLeast(MIN_RULE)))
    }

    fun section(text: String) {
        line()
        line(text)
        line("-".repeat(text.length.coerceAtLeast(MIN_RULE)))
    }

    fun success(text: String) = line("[OK] $text")

    fun warning(text: String) = line("[!] $text")

    fun failure(text: String) = line("[X] $text")

    fun hint(text: String) = line("    $text")

    fun prompt(text: String) = write("$text > ")

    private companion object {
        const val MIN_RULE = 8
    }
}
