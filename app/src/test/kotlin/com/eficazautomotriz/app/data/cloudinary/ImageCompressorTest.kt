package com.eficazautomotriz.app.data.cloudinary

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** JVM-testable parts of [ImageCompressor]; bitmap decoding itself needs a device. */
class ImageCompressorTest {

    private class TrackingStream : InputStream() {
        var closed = false
        override fun read(): Int = -1
        override fun close() {
            closed = true
        }
    }

    @Test
    fun `a null result from an opened stream is passed through instead of failing`() {
        // Mirrors decoding with inJustDecodeBounds, where decodeStream returns null by design.
        val result: Any? = ImageCompressor.withOpenedStream(ByteArrayInputStream(byteArrayOf(1))) { null }
        assertNull(result)
    }

    @Test
    fun `the block result is returned and the stream is closed`() {
        val stream = TrackingStream()
        val result = ImageCompressor.withOpenedStream(stream) { 42 }
        assertEquals(42, result)
        assertTrue(stream.closed)
    }

    @Test
    fun `the stream is closed even when the block throws`() {
        val stream = TrackingStream()
        assertThrows(IllegalStateException::class.java) {
            ImageCompressor.withOpenedStream(stream) { error("boom") }
        }
        assertTrue(stream.closed)
    }

    @Test
    fun `a null stream reports that the image could not be opened`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ImageCompressor.withOpenedStream<InputStream, Int>(null) { 1 }
        }
        assertEquals("No se pudo abrir la imagen.", error.message)
    }

    @Test
    fun `small images are not subsampled`() {
        assertEquals(1, ImageCompressor.sampleSize(800, 600, 1280))
        assertEquals(1, ImageCompressor.sampleSize(1280, 1280, 1280))
    }

    @Test
    fun `sample size is the largest power of two keeping the longest side within bounds`() {
        assertEquals(2, ImageCompressor.sampleSize(2560, 1440, 1280))
        assertEquals(4, ImageCompressor.sampleSize(3000, 4000, 1280))
        assertEquals(4, ImageCompressor.sampleSize(1000, 4032, 1280))
    }

    @Test
    fun `sample size terminates for tiny max sides`() {
        assertEquals(2048, ImageCompressor.sampleSize(4000, 3000, 1))
    }
}
