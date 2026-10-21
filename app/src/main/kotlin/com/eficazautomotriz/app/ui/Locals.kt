package com.eficazautomotriz.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider
import com.eficazautomotriz.domain.model.User

/** ViewModel factory of the current session; provided by AppRoot. */
val LocalVmFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("LocalVmFactory not provided: screens are shown inside AppRoot")
}

/** Snackbar shared by the main Scaffold for confirmation and error messages. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Session actions for the header of the current destination, provided by AppRoot per route.
 * `onBack` is null on bottom-bar destinations (no back arrow there).
 */
data class ShellActions(
    val user: User?,
    val onBack: (() -> Unit)?,
    val onProfile: () -> Unit,
    val onSignOut: () -> Unit,
)

val LocalShell = staticCompositionLocalOf<ShellActions?> { null }
