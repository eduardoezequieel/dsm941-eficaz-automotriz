package com.eficazautomotriz.app.data.cloudinary

import com.eficazautomotriz.app.data.ImageUploader
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Uploads a JPEG image to Cloudinary with an unsigned upload preset.
 *
 * [upload] is blocking: call it on an IO dispatcher, never on the main thread.
 * Returns `secure_url`. Exception messages are in Spanish because they reach the UI.
 */
class CloudinaryUploader(
    private val cloudName: String,
    private val preset: String,
    private val client: OkHttpClient = OkHttpClient(),
) : ImageUploader {
    override fun upload(bytes: ByteArray, fileName: String): String {
        if (cloudName.isBlank()) {
            throw IllegalStateException("Falta el nombre de la nube de Cloudinary (cloudName).")
        }
        if (preset.isBlank()) {
            throw IllegalStateException("Falta el upload preset de Cloudinary.")
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart(
                "file",
                fileName,
                bytes.toRequestBody(JPEG.toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudinary respondió con el código ${response.code}.")
            }
            val json = try {
                JSONObject(payload)
            } catch (error: JSONException) {
                throw IllegalStateException("La respuesta de Cloudinary no es JSON válido.", error)
            }
            val secureUrl = json.optString("secure_url")
            if (secureUrl.isBlank()) {
                throw IllegalStateException("La respuesta de Cloudinary no incluye secure_url.")
            }
            return secureUrl
        }
    }

    private companion object {
        const val JPEG = "image/jpeg"
    }
}