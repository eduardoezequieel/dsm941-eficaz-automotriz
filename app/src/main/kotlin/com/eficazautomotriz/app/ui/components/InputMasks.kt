package com.eficazautomotriz.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.validation.TextFields

/** Rewrites what the user typed before it reaches the state (drop or convert characters). */
fun interface InputFilter {
    fun filter(typed: String): String
}

/**
 * Typing-time filters: they only keep characters the field can ever accept and cut at the
 * limit, so the user cannot type past it. Validators still check the final value.
 */
object InputFilters {

    val None = InputFilter { it }

    /** Digits only: year, capacity, intervals. */
    val Digits = InputFilter { typed -> typed.filter { it.isDigit() } }

    /** Digits and the thousands comma that `InputParsers.parseInt` accepts (30,000). */
    val DigitsWithThousands = InputFilter { typed -> typed.filter { it.isDigit() || it == ',' } }

    /** Single-line text: newlines and tabs pasted from elsewhere become spaces. */
    val SingleLine = InputFilter { typed -> typed.replace(Regex("[\\t\\r\\n]+"), " ") }

    /**
     * Letters with accents, digits and the punctuation makes, models and catalog names share.
     * A pasted newline becomes a space, then anything else is dropped. The value must start
     * with a letter or digit, so leading spaces and signs never reach the state.
     */
    val NameCharacters = InputFilter { typed ->
        SingleLine.filter(typed).filter { it.isLetterOrDigit() || it in " .,'&/-" }.trimStart { !it.isLetterOrDigit() }
    }

    /**
     * Slot capacity: digits only, no more of them than [maxCapacity] needs, and never above it.
     * "25" with a ceiling of 20 becomes "2". An empty field stays empty so the user can retype.
     */
    fun capacity(maxCapacity: Int): InputFilter = InputFilter { typed ->
        val ceiling = maxCapacity.coerceAtLeast(0)
        val limit = ceiling.toString().length
        var digits = typed.filter { it.isDigit() }.take(limit)
        while (digits.isNotEmpty()) {
            val value = digits.toIntOrNull()
            if (value != null && value <= ceiling) {
                return@InputFilter digits.trimStart('0').ifEmpty { "0" }
            }
            digits = digits.dropLast(1)
        }
        ""
    }

    /**
     * Salvadoran plate: uppercase letters and digits only (the hyphen is drawn by
     * [PlateVisualTransformation]) and always starting with P. Typing "123" yields "P123".
     */
    val Plate = InputFilter { typed ->
        val compact = typed.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
        when {
            compact.isEmpty() -> ""
            compact.first() == 'P' -> compact
            else -> "P$compact"
        }
    }

    /** Email is one token: spaces and line breaks never reach the field. */
    val Email = InputFilter { typed -> typed.filter { !it.isWhitespace() } }

    /** Person name: letters (with accents), spaces, and . ' - . Same set as [TextFields.PERSON_NAME]. */
    val PersonName = InputFilter { typed ->
        buildString {
            for (ch in typed) {
                when {
                    ch.isLetter() || ch == '.' || ch == '\'' || ch == '-' -> append(ch)
                    ch.isWhitespace() -> append(' ')
                }
            }
        }
    }

    /**
     * Salvadoran phone: digits only, capped at [TextFields.PHONE] (8).
     * The hyphen is drawn by [PhoneVisualTransformation], so it is not stored.
     */
    val Phone = InputFilter { typed -> typed.filter { it.isDigit() }.take(TextFields.PHONE.maxLength) }

    fun apply(typed: String, filter: InputFilter, maxLength: Int?): String {
        val filtered = filter.filter(typed)
        return if (maxLength != null && filtered.length > maxLength) filtered.take(maxLength) else filtered
    }

    /**
     * Where the cursor goes after [apply]: right after whatever the filter keeps of the text
     * before it. The plate filter turns "1|" into "P1|", not "P|1", so the next digit lands
     * after the 1 instead of before it.
     */
    fun cursorAfter(typed: String, cursor: Int, filter: InputFilter, maxLength: Int?): Int {
        val result = apply(typed, filter, maxLength)
        return apply(typed.take(cursor.coerceIn(0, typed.length)), filter, maxLength).length.coerceAtMost(result.length)
    }
}

/**
 * Shows a compact plate (P123AB2) as P123-AB2 while the state keeps it without the hyphen.
 * The hyphen sits after the fourth character and only appears once a fifth one is typed.
 */
object PlateVisualTransformation : VisualTransformation {

    private const val HYPHEN_AT = 4

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val shown = if (raw.length > HYPHEN_AT) "${raw.take(HYPHEN_AT)}-${raw.drop(HYPHEN_AT)}" else raw
        return TransformedText(AnnotatedString(shown), PlateOffsetMapping(raw.length))
    }
}

/**
 * Cursor mapping for [PlateVisualTransformation]; public so its edge cases are unit-tested.
 * The hyphen is not stored: a cursor right after it maps to the end of the fourth character,
 * so a backspace there deletes that character (Compose edits in original coordinates).
 */
class PlateOffsetMapping(private val rawLength: Int) : OffsetMapping {

    private val hasHyphen = rawLength > 4
    private val shownLength = if (hasHyphen) rawLength + 1 else rawLength

    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, rawLength)
        return if (hasHyphen && clamped > 4) clamped + 1 else clamped
    }

    override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, shownLength)
        return if (hasHyphen && clamped > 4) clamped - 1 else clamped
    }
}

/** Plate limit in the field: the compact length the domain stores. */
const val PLATE_MAX_LENGTH = ValidationError.PLATE_LENGTH

/**
 * Shows a stored phone (71234567) as 7123-4567. The hyphen appears once a fifth digit is typed.
 * The state keeps the digits only, the same way a plate keeps its compact form.
 */
object PhoneVisualTransformation : VisualTransformation {

    private const val HYPHEN_AT = 4

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val shown = if (raw.length > HYPHEN_AT) "${raw.take(HYPHEN_AT)}-${raw.drop(HYPHEN_AT)}" else raw
        return TransformedText(AnnotatedString(shown), PhoneOffsetMapping(raw.length))
    }
}

/** Cursor mapping for [PhoneVisualTransformation]; public so its edge cases are unit-tested. */
class PhoneOffsetMapping(private val rawLength: Int) : OffsetMapping {

    private val hasHyphen = rawLength > 4
    private val shownLength = if (hasHyphen) rawLength + 1 else rawLength

    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, rawLength)
        return if (hasHyphen && clamped > 4) clamped + 1 else clamped
    }

    override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, shownLength)
        return if (hasHyphen && clamped > 4) clamped - 1 else clamped
    }
}

/**
 * Profile card form of a phone. New accounts store 8 digits; older rows store `####-####`.
 * Both are shown as `7123-4567`. Anything else is left as it was stored.
 */
fun displayPhone(stored: String): String {
    val digits = stored.filter { it.isDigit() }
    val onlyPhoneChars = stored.all { it.isDigit() || it == '-' || it.isWhitespace() }
    return if (digits.length == 8 && onlyPhoneChars) "${digits.take(4)}-${digits.drop(4)}" else stored.trim()
}
