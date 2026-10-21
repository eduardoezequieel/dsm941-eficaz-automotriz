package com.eficazautomotriz.app.logging

import android.util.Log
import com.eficazautomotriz.logging.ErrorLogger
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Same line as core's `FileErrorLogger` (`timestamp | level | context | message | data`),
 * written to `filesDir/logs/errors.log` and mirrored to Logcat.
 */
class AndroidErrorLogger(filesDir: File) : ErrorLogger {

    private val logFile = File(filesDir, "logs/errors.log")

    override fun warn(context: String, message: String, data: String) {
        val line = buildLine(LEVEL_WARN, context, message, data)
        Log.w(TAG, line)
        append(line)
    }

    override fun error(context: String, message: String, data: String) {
        val line = buildLine(LEVEL_ERROR, context, message, data)
        Log.e(TAG, line)
        append(line)
    }

    private fun append(line: String) {
        // A logger that crashes the app is worse than having no logger.
        try {
            logFile.parentFile?.mkdirs()
            logFile.appendText(line + System.lineSeparator())
        } catch (e: IOException) {
            Log.e(TAG, "Could not write ${logFile.path}", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission on ${logFile.path}", e)
        }
    }

    private fun buildLine(level: String, context: String, message: String, data: String): String {
        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
        val fields = listOf(timestamp, level, context, message) + if (data.isBlank()) emptyList() else listOf(data)
        return fields.joinToString(separator = FIELD_SEPARATOR)
    }

    private companion object {
        const val TAG = "Eficaz"
        const val LEVEL_WARN = "WARN"
        const val LEVEL_ERROR = "ERROR"
        const val FIELD_SEPARATOR = " | "
        val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}