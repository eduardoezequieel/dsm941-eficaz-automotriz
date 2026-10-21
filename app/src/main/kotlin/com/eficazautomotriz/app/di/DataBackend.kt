package com.eficazautomotriz.app.di

import com.eficazautomotriz.app.data.ImageUploader
import com.eficazautomotriz.app.data.auth.AuthGateway
import com.eficazautomotriz.data.Repositories
import kotlinx.coroutines.CoroutineDispatcher

/**
 * What [AppGraph] needs from the outside world: persistence, authentication and image upload.
 * Production uses [firebaseBackend]; debug builds with `demo.inMemory=true` use the in-memory
 * backend of `src/debug/.../demo` (see `demoBackendOrNull`).
 */
class DataBackend(
    val repositories: Repositories,
    val authGateway: AuthGateway,
    val imageUploader: ImageUploader,
    val imageUploaderConfigured: Boolean,
    /**
     * Dispatcher for blocking repository calls. Firestore uses `Dispatchers.IO`; the in-memory
     * repositories are not thread-safe, so the demo serializes every call on one thread.
     */
    val io: CoroutineDispatcher,
    /** Blocking Firestore housekeeping run for staff sessions (contadores/placas/ocupacion); null otherwise. */
    val staffBackfill: (() -> Unit)?,
    /** True for the offline demo: no Firebase, seeded mockup data, demo accounts. */
    val demoMode: Boolean,
)
