package com.eficazautomotriz.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.eficazautomotriz.app.ui.LocalSnackbar

/** Reloads every time the screen comes back to the front (e.g. returning from a form). */
@Composable
fun ReloadOnResume(load: () -> Unit) {
    LifecycleResumeEffect(Unit) {
        load()
        onPauseOrDispose { }
    }
}

/** Shows a ViewModel message in the shared Snackbar and marks it as consumed. */
@Composable
fun MessageSnackbar(message: String?, onShown: () -> Unit) {
    val snackbar = LocalSnackbar.current
    LaunchedEffect(message) {
        if (message != null) {
            onShown()
            snackbar.showSnackbar(message)
        }
    }
}
