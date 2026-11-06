package com.eficazautomotriz.app

import android.app.Application
import com.eficazautomotriz.app.demo.demoBackendOrNull
import com.eficazautomotriz.app.di.AppGraph
import com.google.firebase.FirebaseApp

/**
 * Process entry point. The graph is created lazily on first access: without
 * `google-services.json` Firebase is not initialized and creating it would crash the app.
 * Access: `(context.applicationContext as EficazApp).graph`.
 *
 * Debug builds with `demo.inMemory=true` in local.properties use the offline demo backend
 * (mockup data, no Firebase); release builds never do (see `demoBackendOrNull`).
 */
class EficazApp : Application() {

    val demoMode: Boolean get() = BuildConfig.DEBUG && BuildConfig.DEMO_IN_MEMORY

    val graph: AppGraph by lazy { AppGraph(this, if (demoMode) demoBackendOrNull() else null) }

    /** false when built without app/google-services.json: the UI shows a warning. */
    val firebaseConfigured: Boolean get() = FirebaseApp.getApps(this).isNotEmpty()
}
