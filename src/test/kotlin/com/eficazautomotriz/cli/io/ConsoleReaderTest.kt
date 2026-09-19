package com.eficazautomotriz.cli.io

import com.eficazautomotriz.domain.validation.TextFields
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Primer nivel de validacion: el FORMATO y los limites de lo tecleado. Cada prueba entrega
 * un guion de respuestas y comprueba cuantas consumio, con que valor salio y que se aviso.
 */
class ConsoleReaderTest {

    /** Entrega las respuestas en orden y devuelve null cuando se agotan, como un Enter final. */
    private class ScriptedInput(answers: List<String>) : () -> String? {
        private val pending = ArrayDeque(answers)
        var consumed: Int = 0
            private set

        override fun invoke(): String? {
            consumed++
            return pending.removeFirstOrNull()
        }
    }

    private class Console(answers: List<String>, maxAttempts: Int) {
        val printed = StringBuilder()
        val input = ScriptedInput(answers)
        val reader = ConsoleReader(ConsoleWriter(printed::append), input, maxAttempts)

        val output: String get() = printed.toString()
    }

    private fun consoleOver(vararg answers: String, maxAttempts: Int = 3) =
        Console(answers.toList(), maxAttempts)

    // --- Fechas ---

    @Test
    fun `una fecha bien escrita se acepta tal cual`() {
        val console = consoleOver("20/09/2026")

        assertEquals(LocalDate.of(2026, 9, 20), console.reader.readDate("Fecha"))
    }

    @Test
    fun `un 31 de febrero se rechaza en vez de ajustarse en silencio`() {
        // Con el estilo SMART por omision esto devolveria 28/02/2026: una fecha que
        // el usuario nunca escribio. El lector debe volver a preguntar.
        val console = consoleOver("31/02/2026", "20/09/2026")

        assertEquals(LocalDate.of(2026, 9, 20), console.reader.readDate("Fecha"))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `el 29 de febrero de un año no bisiesto se rechaza`() {
        val console = consoleOver("29/02/2026", "01/03/2026")

        assertEquals(LocalDate.of(2026, 3, 1), console.reader.readDate("Fecha"))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `el 29 de febrero de un año bisiesto se acepta`() {
        val console = consoleOver("29/02/2028")

        assertEquals(LocalDate.of(2028, 2, 29), console.reader.readDate("Fecha"))
    }

    @Test
    fun `un mes trece se rechaza`() {
        val console = consoleOver("01/13/2026", "01/12/2026")

        assertEquals(LocalDate.of(2026, 12, 1), console.reader.readDate("Fecha"))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `texto que no es una fecha se rechaza sin lanzar excepcion`() {
        val console = consoleOver("ayer", "2026-09-20", "20/09/2026", maxAttempts = 3)

        assertEquals(LocalDate.of(2026, 9, 20), console.reader.readDate("Fecha"))
        assertEquals(3, console.input.consumed)
    }

    @Test
    fun `una fecha fuera del rango pedido se rechaza aunque sea valida`() {
        val range = LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31)
        val console = consoleOver("01/01/2999", "15/06/2026")

        assertEquals(LocalDate.of(2026, 6, 15), console.reader.readDate("Fecha", range))
        assertEquals(2, console.input.consumed)
    }

    // --- Numeros ---

    @Test
    fun `letras donde va un numero se rechazan hasta obtener un entero`() {
        val console = consoleOver("hola", "3")

        assertEquals(3, console.reader.readInt("Opcion", min = 0, max = 5))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `un numero fuera del rango se rechaza`() {
        val console = consoleOver("99", "-3", "2")

        assertEquals(2, console.reader.readInt("Opcion", min = 0, max = 5))
        assertEquals(3, console.input.consumed)
    }

    @Test
    fun `el separador de miles bien puesto se acepta`() {
        val console = consoleOver("30,000")

        assertEquals(30_000, console.reader.readInt("Kilometraje", min = 0, max = 2_000_000))
    }

    @Test
    fun `comas mal puestas ya no se cuelan como un numero distinto`() {
        // "1,2,3" llegaba a valer 123: se quitaban las comas sin mirar donde estaban.
        val console = consoleOver("1,2,3", "123")

        assertEquals(123, console.reader.readInt("Kilometraje", min = 0, max = 2_000_000))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `un numero mayor que Int se rechaza en vez de desbordar`() {
        val console = consoleOver("99999999999", "7")

        assertEquals(7, console.reader.readInt("Kilometraje", min = 0, max = 2_000_000))
        assertEquals(2, console.input.consumed)
    }

    // --- Texto acotado ---

    @Test
    fun `un texto mas largo que el limite del campo se rechaza`() {
        val tooLong = "A".repeat(TextFields.MAKE.maxLength + 1)
        val console = consoleOver(tooLong, "Toyota")

        assertEquals("Toyota", console.reader.readText("Marca", TextFields.MAKE))
        assertEquals(2, console.input.consumed)
        assertTrue(console.output.contains("${TextFields.MAKE.maxLength} caracteres como maximo"))
    }

    @Test
    fun `un texto en el limite exacto del campo se acepta`() {
        val exact = "A".repeat(TextFields.MAKE.maxLength)
        val console = consoleOver(exact)

        assertEquals(exact, console.reader.readText("Marca", TextFields.MAKE))
    }

    @Test
    fun `un texto con caracteres no permitidos se rechaza`() {
        val console = consoleOver("!!!###", "Toyota")

        assertEquals("Toyota", console.reader.readText("Marca", TextFields.MAKE))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `un texto en blanco se rechaza y los espacios sobrantes se recortan`() {
        val console = consoleOver("   ", "  Toyota  ")

        assertEquals("Toyota", console.reader.readText("Marca", TextFields.MAKE))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `una linea desmedida se rechaza antes de validar el campo`() {
        val pasted = "a".repeat(5_000)
        val console = consoleOver(pasted, "Toyota")

        assertEquals("Toyota", console.reader.readText("Marca", TextFields.MAKE))
        assertTrue(console.output.contains("supera los"))
    }

    @Test
    fun `el texto opcional admite vacio pero valida lo que se escribe`() {
        val console = consoleOver("")

        assertEquals("", console.reader.readOptionalText("Notas", TextFields.NOTES))
    }

    @Test
    fun `el texto opcional rechaza lo que excede el limite`() {
        val console = consoleOver("N".repeat(TextFields.NOTES.maxLength + 1), "sin novedad")

        assertEquals("sin novedad", console.reader.readOptionalText("Notas", TextFields.NOTES))
        assertEquals(2, console.input.consumed)
    }

    // --- Placa y direccion de imagen ---

    @Test
    fun `una placa invalida se rechaza hasta obtener el formato de El Salvador`() {
        val console = consoleOver("P12332231213123123", "P123456")

        assertEquals("P123456", console.reader.readPlate("Placa"))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `una placa en minusculas se guarda en mayusculas`() {
        val console = consoleOver("p12300a")

        assertEquals("P12300A", console.reader.readPlate("Placa"))
    }

    @Test
    fun `una direccion sin protocolo se rechaza`() {
        val console = consoleOver("ejemplo.com/foto.jpg", "https://ejemplo.com/foto.jpg")

        assertEquals("https://ejemplo.com/foto.jpg", console.reader.readImageUrl("Direccion"))
        assertEquals(2, console.input.consumed)
    }

    @Test
    fun `una direccion mas larga que el limite se rechaza`() {
        val tooLong = "https://ejemplo.com/" + "a".repeat(TextFields.IMAGE_URL.maxLength)
        val console = consoleOver(tooLong, "http://ejemplo.com/f.jpg")

        assertEquals("http://ejemplo.com/f.jpg", console.reader.readImageUrl("Direccion"))
        assertEquals(2, console.input.consumed)
    }

    // --- Confirmacion y cancelacion ---

    @Test
    fun `confirmar acepta si y no en sus variantes`() {
        assertTrue(consoleOver("SÍ").reader.confirm("Continuar"))
        assertTrue(consoleOver("s").reader.confirm("Continuar"))
        assertFalse(consoleOver("No").reader.confirm("Continuar"))
    }

    @Test
    fun `confirmar con basura reintenta y al agotarse responde que no`() {
        val console = consoleOver("quiza", "tal vez", "puede ser", maxAttempts = 3)

        assertFalse(console.reader.confirm("Continuar"))
        assertEquals(3, console.input.consumed)
    }

    @Test
    fun `agotar los intentos cancela la operacion en vez de insistir sin fin`() {
        val console = consoleOver("a", "b", "c", "4", maxAttempts = 3)

        assertNull(console.reader.readInt("Opcion", min = 0, max = 5))
        assertEquals(3, console.input.consumed)
        assertTrue(console.output.contains("Se agotaron los 3 intentos"))
    }

    @Test
    fun `agotar la entrada equivale a cancelar y no cuelga el bucle`() {
        val console = consoleOver()

        assertNull(console.reader.readInt("Opcion", min = 0, max = 5))
        assertNull(console.reader.readDate("Fecha"))
        assertNull(console.reader.readText("Texto", TextFields.MAKE))
        assertNull(console.reader.readPlate("Placa"))
        assertNull(console.reader.readOptionalText("Notas", TextFields.NOTES))
    }

    // --- Seleccion ---

    @Test
    fun `readChoice devuelve null cuando se elige cero`() {
        val console = consoleOver("0")

        assertNull(console.reader.readChoice("Opcion", listOf("a", "b")) { it })
    }

    @Test
    fun `readChoice devuelve el elemento elegido`() {
        val console = consoleOver("2")

        assertEquals("b", console.reader.readChoice("Opcion", listOf("a", "b")) { it })
    }

    @Test
    fun `readChoice sobre una lista vacia avisa y no pide nada`() {
        val console = consoleOver("1")

        assertNull(console.reader.readChoice("Opcion", emptyList<String>()) { it })
        assertEquals(0, console.input.consumed)
    }
}
