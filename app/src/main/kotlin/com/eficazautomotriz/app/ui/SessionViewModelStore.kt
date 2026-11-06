package com.eficazautomotriz.app.ui

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * ViewModelStore of one signed-in session. [AppRoot] installs it as
 * [androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner] around the NavHost and
 * calls [clear] when the session scope changes, so every screen ViewModel is dropped
 * on sign-out or when another account signs in. [com.eficazautomotriz.app.viewmodel.SessionViewModel]
 * stays on the activity store, outside this owner.
 */
class SessionViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    fun clear() {
        viewModelStore.clear()
    }
}
