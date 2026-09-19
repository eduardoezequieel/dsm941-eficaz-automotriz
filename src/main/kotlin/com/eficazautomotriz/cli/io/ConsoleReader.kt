package com.eficazautomotriz.cli.io

import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.validation.TextField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Desenlace de un intento de lectura: valor aceptado o motivo del rechazo. */
private sealed interface Parsed<out T>

private data class Accepted<out T>(val value: T) : Parsed<T>

private data class Rejected(val reason: String) : Parsed<Nothing>

/**
 * Lectura validada del teclado: formato, longitud y rango de lo que se teclea. El
 * significado de negocio lo validan dominio y casos de uso, con las mismas reglas.
 *
 * Cada campo admite un numero acotado de intentos; agotarlos cancela la operacion en
 * lugar de dejar al usuario atrapado en el mismo prompt.
 */
class ConsoleReader(
    private val writer: ConsoleWriter,
    private val input: () -> String? = ::readlnOrNull,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
) {

    fun readInt(prompt: String, min: Int, max: Int): Int? =
        readValidated("$prompt [${group(min)}-${group(max)}]") { raw ->
            val value = if (INTEGER.matches(raw)) raw.replace(",", "").toIntOrNull() else null
            when {
                value == null -> Rejected("Escriba un numero entero, sin letras ni simbolos.")
                value < min || value > max -> Rejected("El valor debe estar entre ${group(min)} y ${group(max)}.")
                else -> Accepted(value)
            }
        }

    /** Texto obligatorio, acotado por la regla del campo. */
    fun readText(prompt: String, field: TextField): String? =
        readValidated("$prompt [max ${field.maxLength}]") { raw ->
            check(raw) { ValidationError.validateText(field, it) }
        }

    /** Texto opcional: Enter lo deja vacio, pero lo que se escriba se valida igual. */
    fun readOptionalText(prompt: String, field: TextField): String? =
        readValidated("$prompt (Enter para omitir, max ${field.maxLength})") { raw ->
            check(raw) { ValidationError.validateOptionalText(field, it) }
        }

    fun readPlate(prompt: String): String? = readValidated("$prompt [P123456]") { raw ->
        check(raw.uppercase()) { ValidationError.validatePlate(it) }
    }

    fun readImageUrl(prompt: String): String? = readValidated("$prompt [http:// o https://]") { raw ->
        check(raw) { ValidationError.validateImageUrl(it) }
    }

    /** El rango acota el ano a lo que el caso de uso puede aceptar, no a los limites de LocalDate. */
    fun readDate(prompt: String, range: ClosedRange<LocalDate>? = null): LocalDate? =
        readValidated("$prompt [dd/mm/aaaa]") { raw ->
            val date = try {
                LocalDate.parse(raw, DATE_FORMAT)
            } catch (e: DateTimeParseException) {
                null
            }
            when {
                date == null -> Rejected("Fecha invalida. Use el formato dd/mm/aaaa, por ejemplo 20/09/2026.")
                range != null && date !in range ->
                    Rejected("La fecha debe estar entre ${format(range.start)} y ${format(range.endInclusive)}.")
                else -> Accepted(date)
            }
        }

    /** Menu de seleccion sobre una lista. Devuelve null si el usuario cancela con 0. */
    fun <T> readChoice(prompt: String, options: List<T>, label: (T) -> String): T? {
        if (options.isEmpty()) {
            writer.warning("No hay opciones disponibles.")
            return null
        }
        options.forEachIndexed { index, option -> writer.line("  ${index + 1}) ${label(option)}") }
        writer.line("  0) Cancelar")
        val choice = readInt(prompt, min = 0, max = options.size) ?: return null
        return if (choice == 0) null else options[choice - 1]
    }

    fun readMenuOption(prompt: String, max: Int): Int? = readInt(prompt, min = 0, max = max)

    fun confirm(prompt: String): Boolean = readValidated("$prompt [s/n]") { raw ->
        when (raw.lowercase()) {
            "s", "si", "sí" -> Accepted(true)
            "n", "no" -> Accepted(false)
            else -> Rejected("Responda con s o n.")
        }
    } ?: false

    fun pause() {
        writer.line("")
        writer.prompt("Presione Enter para continuar")
        input()
    }

    /** Traduce el validador del dominio al desenlace que entiende el bucle de lectura. */
    private fun check(value: String, validate: (String) -> ValidationError?): Parsed<String> =
        when (val error = validate(value)) {
            null -> Accepted(value)
            else -> Rejected(error.message)
        }

    /**
     * Bucle unico de lectura: recorta, descarta lo que ni siquiera merece validarse
     * (linea desmedida o caracteres de control) y reintenta hasta agotar los intentos.
     */
    private fun <T> readValidated(prompt: String, parse: (String) -> Parsed<T>): T? {
        repeat(maxAttempts) {
            val raw = ask(prompt)?.trim() ?: return null
            val rejection = when {
                raw.length > MAX_LINE_LENGTH -> "La entrada supera los $MAX_LINE_LENGTH caracteres permitidos."
                raw.any { it.isISOControl() } -> "La entrada contiene caracteres no imprimibles."
                else -> when (val parsed = parse(raw)) {
                    is Accepted -> return parsed.value
                    is Rejected -> parsed.reason
                }
            }
            writer.warning(rejection)
        }
        writer.failure("Se agotaron los $maxAttempts intentos. Operacion cancelada.")
        return null
    }

    /** Devuelve null cuando la entrada se agota: equivale a cancelar. */
    private fun ask(prompt: String): String? {
        writer.prompt(prompt)
        return input()
    }

    private fun format(date: LocalDate): String = DATE_FORMAT.format(date)

    /** Los limites se muestran agrupados porque asi tambien se aceptan al teclear. */
    private fun group(value: Int): String = "%,d".format(value)

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3

        /**
         * Tope duro de una linea, holgado sobre el campo mas largo: ataja el pegado
         * accidental sin quitarle el rechazo por longitud a la regla del campo.
         */
        const val MAX_LINE_LENGTH = 1_000

        /** Digitos con separador de miles bien puesto; "1,2,3" y "12a" no son numeros. */
        val INTEGER = Regex("^-?\\d{1,3}(,\\d{3})+$|^-?\\d{1,10}$")

        /**
         * `ResolverStyle.STRICT` es deliberado: con el estilo por omision, el 31/02/2026 se
         * ajusta en silencio al 28/02 y el usuario agenda en un dia que no escribio. El estilo
         * estricto exige el ano independiente de era (`uuuu`, no `yyyy`).
         */
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
