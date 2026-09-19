package com.eficazautomotriz.logging

import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Escribe una linea por evento en modo append.
 * Formato: timestamp | nivel | contexto | mensaje | datos
 */
class FileErrorLogger(private val logPath: String) : ErrorLogger {

    override fun warn(context: String, message: String, data: String) =
        append(LEVEL_WARN, context, message, data)

    override fun error(context: String, message: String, data: String) =
        append(LEVEL_ERROR, context, message, data)

    private fun append(level: String, context: String, message: String, data: String) {
        // Un logger que tumba la aplicacion es peor que no tener logger.
        try {
            val file = File(logPath)
            file.parentFile?.mkdirs()
            file.appendText(buildLine(level, context, message, data))
        } catch (e: IOException) {
            // Sin salida por consola: el dominio y la infraestructura no imprimen.
        } catch (e: SecurityException) {
            // Permisos denegados sobre el directorio de logs: se ignora deliberadamente.
        }
    }

    private fun buildLine(level: String, context: String, message: String, data: String): String {
        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
        val fields = listOf(timestamp, level, context, message) + if (data.isBlank()) emptyList() else listOf(data)
        return fields.joinToString(separator = FIELD_SEPARATOR, postfix = System.lineSeparator())
    }

    private companion object {
        const val LEVEL_WARN = "WARN"
        const val LEVEL_ERROR = "ERROR"
        const val FIELD_SEPARATOR = " | "
        val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}
