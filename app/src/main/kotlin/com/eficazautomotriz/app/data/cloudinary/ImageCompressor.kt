package com.eficazautomotriz.app.data.cloudinary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.Closeable

/**
 * Shrinks a photo to JPEG before uploading it.
 *
 * Decodes with `inSampleSize` to avoid loading extra pixels and, when needed, scales the
 * longest side down to [maxSide]. Honors the EXIF orientation. Blocking: call it on IO.
 */
object ImageCompressor {
    fun compress(context: Context, uri: Uri, maxSide: Int = 1280, quality: Int = 80): ByteArray {
        require(maxSide >= 1) { "maxSide must be greater than zero." }
        require(quality in 0..100) { "quality must be between 0 and 100." }

        val orientation = readOrientation(context, uri)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        // With inJustDecodeBounds, decodeStream always returns null by design; only the bounds matter.
        withOpenedStream(context.contentResolver.openInputStream(uri)) { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("No se pudo leer la imagen.")
        }

        val decoded = decodeSampled(context, uri, bounds.outWidth, bounds.outHeight, maxSide)
        var scaled: Bitmap? = null
        var oriented: Bitmap? = null
        try {
            scaled = scaleDown(decoded, maxSide)
            oriented = applyOrientation(scaled, orientation)
            val output = ByteArrayOutputStream()
            if (!oriented.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                throw IllegalStateException("No se pudo comprimir la imagen a JPEG.")
            }
            return output.toByteArray()
        } finally {
            recycleDistinct(decoded, scaled, oriented)
        }
    }

    private fun decodeSampled(context: Context, uri: Uri, width: Int, height: Int, maxSide: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(width, height, maxSide)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        return withOpenedStream(context.contentResolver.openInputStream(uri)) { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IllegalArgumentException("No se pudo decodificar la imagen.")
    }

    /**
     * Runs [block] on [stream] and closes it. A null [stream] means the source could not be
     * opened; a null result from [block] is passed through so callers decide what it means.
     */
    internal inline fun <S : Closeable, R> withOpenedStream(stream: S?, block: (S) -> R): R =
        (stream ?: throw IllegalArgumentException("No se pudo abrir la imagen.")).use(block)

    /** Largest power of two that keeps the longest side at or below [maxSide]. */
    internal fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        val largest = maxOf(width, height)
        while (largest / sample > maxSide && sample < largest) {
            val next = sample * 2
            if (next <= sample) break
            sample = next
        }
        return sample
    }

    private fun scaleDown(source: Bitmap, maxSide: Int): Bitmap {
        val largest = maxOf(source.width, source.height)
        if (largest <= maxSide || largest <= 0) return source
        val scale = maxSide.toFloat() / largest.toFloat()
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    /** Best effort: an unopenable stream or unreadable EXIF falls back to NORMAL; compress reports the error. */
    private fun readOrientation(context: Context, uri: Uri): Int =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            // Known limitation: TRANSPOSE/TRANSVERSE (5 and 7) are rare and their op order is not verified.
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun recycleDistinct(decoded: Bitmap, scaled: Bitmap?, oriented: Bitmap?) {
        if (oriented != null && !oriented.isRecycled) oriented.recycle()
        if (scaled != null && scaled !== oriented && !scaled.isRecycled) scaled.recycle()
        if (decoded !== scaled && decoded !== oriented && !decoded.isRecycled) decoded.recycle()
    }
}