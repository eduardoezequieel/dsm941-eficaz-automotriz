package com.eficazautomotriz.app.data.firestore

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Firestore document (de)serialization conventions:
 * - `LocalDate` → "2026-10-18", `LocalDateTime` → "2026-10-18T10:30", `LocalTime` → "09:00"
 *   (the ISO `toString()` of java.time, with no time zones and no `Timestamp`).
 * - `DayOfWeek` and enums → `name`.
 * - Nullable fields are written as an explicit `null`: they are never omitted.
 *
 * Firestore returns every integer as `Long`, so integers are read as `Number` and
 * converted. Exception messages are in Spanish because they can reach the UI.
 */
object FirestoreCodecs {

    fun encode(value: LocalDate): String = value.toString()
    fun encode(value: LocalDateTime): String = value.toString()
    fun encode(value: LocalTime): String = value.toString()
    fun encode(value: Enum<*>): String = value.name

    fun encodeOrNull(value: LocalDateTime?): String? = value?.let(::encode)
}

/** A mandatory field is missing or has an unexpected type: the document is corrupt. */
class MalformedDocumentException(field: String, detail: String) :
    IllegalStateException("Campo '$field' invalido en el documento: $detail")

private fun Map<String, Any?>.required(key: String): Any =
    this[key] ?: throw MalformedDocumentException(key, "ausente o nulo")

fun Map<String, Any?>.string(key: String): String =
    required(key) as? String ?: throw MalformedDocumentException(key, "se esperaba texto")

fun Map<String, Any?>.stringOrNull(key: String): String? =
    this[key]?.let { it as? String ?: throw MalformedDocumentException(key, "se esperaba texto") }

fun Map<String, Any?>.int(key: String): Int =
    (required(key) as? Number ?: throw MalformedDocumentException(key, "se esperaba numero")).toInt()

fun Map<String, Any?>.intOrNull(key: String): Int? =
    this[key]?.let { (it as? Number ?: throw MalformedDocumentException(key, "se esperaba numero")).toInt() }

fun Map<String, Any?>.boolean(key: String): Boolean =
    required(key) as? Boolean ?: throw MalformedDocumentException(key, "se esperaba booleano")

fun Map<String, Any?>.localDate(key: String): LocalDate = LocalDate.parse(string(key))

fun Map<String, Any?>.localDateTime(key: String): LocalDateTime = LocalDateTime.parse(string(key))

fun Map<String, Any?>.localDateTimeOrNull(key: String): LocalDateTime? = stringOrNull(key)?.let(LocalDateTime::parse)

fun Map<String, Any?>.localTime(key: String): LocalTime = LocalTime.parse(string(key))

fun Map<String, Any?>.dayOfWeek(key: String): DayOfWeek = enumValue(key)

inline fun <reified E : Enum<E>> Map<String, Any?>.enumValue(key: String): E {
    val name = string(key)
    return enumValues<E>().firstOrNull { it.name == name }
        ?: throw MalformedDocumentException(key, "valor desconocido '$name' para ${E::class.simpleName}")
}
