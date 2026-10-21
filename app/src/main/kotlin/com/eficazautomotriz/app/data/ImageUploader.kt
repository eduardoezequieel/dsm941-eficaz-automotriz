package com.eficazautomotriz.app.data

/**
 * Uploads an image and returns its public HTTPS URL. Blocking: call it on an IO dispatcher.
 * Production: `CloudinaryUploader`; the debug demo mode returns a fake URL.
 */
fun interface ImageUploader {
    fun upload(bytes: ByteArray, fileName: String): String
}
