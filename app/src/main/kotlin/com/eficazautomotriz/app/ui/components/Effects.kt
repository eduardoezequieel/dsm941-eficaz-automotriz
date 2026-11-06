package com.eficazautomotriz.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.eficazautomotriz.app.ui.LocalSnackbar
import kotlinx.coroutines.launch

/** Reloads every time the screen comes back to the front (e.g. returning from a form). */
@Composable
fun ReloadOnResume(load: () -> Unit) {
    LifecycleResumeEffect(Unit) {
        load()
        onPauseOrDispose { }
    }
}

/**
 * Shows a ViewModel message in the shared Snackbar and marks it as consumed.
 * Consuming clears the message, which changes this effect's key and cancels it,
 * so the snackbar runs in the composable's own scope and survives that change.
 */
@Composable
fun MessageSnackbar(message: String?, onShown: () -> Unit) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message != null) {
            scope.launch { snackbar.showSnackbar(message) }
            onShown()
        }
    }
}
