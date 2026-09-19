package com.eficazautomotriz.cli.format

/** Una columna de ancho fijo con su extractor de texto. */
data class Column<T>(
    val header: String,
    val width: Int,
    val alignRight: Boolean = false,
    val value: (T) -> String,
)

/** Tablas de ancho fijo con encabezado. Nunca se vuelca el toString de una data class. */
object TableFormatter {

    fun <T> render(columns: List<Column<T>>, rows: List<T>): List<String> {
        val header = columns.joinToString(SEPARATOR) { pad(it.header, it.width, alignRight = false) }
        val rule = columns.joinToString(SEPARATOR) { "-".repeat(it.width) }
        val body = rows.map { row ->
            columns.joinToString(SEPARATOR) { pad(it.value(row), it.width, it.alignRight) }
        }
        return listOf(header, rule) + body
    }

    /** Barra de texto proporcional al valor sobre el maximo de la serie. */
    fun bar(value: Int, maxValue: Int, maxWidth: Int = DEFAULT_BAR_WIDTH): String {
        if (maxValue <= 0 || value <= 0) return ""
        val length = (value.toDouble() / maxValue * maxWidth).toInt().coerceAtLeast(1)
        return BAR_CHARACTER.repeat(length)
    }

    /** Recorta con puntos suspensivos para que la columna nunca se desborde. */
    private fun pad(text: String, width: Int, alignRight: Boolean): String {
        val clipped = if (text.length <= width) text else text.take(width - 1) + "."
        return if (alignRight) clipped.padStart(width) else clipped.padEnd(width)
    }

    private const val SEPARATOR = "  "
    private const val BAR_CHARACTER = "█"
    private const val DEFAULT_BAR_WIDTH = 20
}
