package com.eficazautomotriz.app.ui

/** State of a screen that loads data: loading, ready, or with a displayable error. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
