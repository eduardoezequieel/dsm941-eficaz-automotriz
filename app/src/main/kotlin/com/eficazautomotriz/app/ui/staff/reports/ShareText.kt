package com.eficazautomotriz.app.ui.staff.reports

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Opens the Android share sheet with [text]; false when no app can receive it. */
internal fun Context.shareText(subject: String, text: String): Boolean {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, text)
    return try {
        startActivity(Intent.createChooser(send, subject))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
