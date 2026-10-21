package com.eficazautomotriz.app.ui.staff.evidence

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Target file for `TakePicture`: `cacheDir/images/evd_<millis>.jpg`, shared with the camera
 * app through the FileProvider declared in the manifest (`res/xml/file_paths.xml`).
 */
internal fun newCameraUri(context: Context): Uri {
    val directory = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(directory, "evd_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
