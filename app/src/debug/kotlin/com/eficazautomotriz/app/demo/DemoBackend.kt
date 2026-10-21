package com.eficazautomotriz.app.demo

import com.eficazautomotriz.app.BuildConfig
import com.eficazautomotriz.app.data.ImageUploader
import com.eficazautomotriz.app.data.auth.InMemoryAuthGateway
import com.eficazautomotriz.app.di.DataBackend
import com.eficazautomotriz.data.Repositories
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

/**
 * Offline demo backend (debug only, `demo.inMemory=true` in local.properties): core in-memory
 * repositories loaded with [MockupSeedData], the two demo accounts, and a fake uploader.
 * It starts signed in as Diego so the app boots to the client home of mockup f2.
 */
fun demoBackendOrNull(): DataBackend? {
    if (!BuildConfig.DEMO_IN_MEMORY) return null
    return demoBackend(LocalDate.now())
}

/** Separate from [demoBackendOrNull] so tests can build it for a fixed date. */
fun demoBackend(today: LocalDate, signedInUid: String? = MockupSeedData.CLIENT_ID): DataBackend {
    val repositories = Repositories()
    MockupSeedData.load(repositories, today)
    // The in-memory repositories are plain maps: every call is serialized on one thread.
    val io = Dispatchers.IO.limitedParallelism(1)
    return DataBackend(
        repositories = repositories,
        authGateway = InMemoryAuthGateway(repositories.users, MockupSeedData.accounts, signedInUid, io),
        imageUploader = ImageUploader { _, fileName -> "https://demo.invalid/evidencias/$fileName" },
        imageUploaderConfigured = true,
        io = io,
        staffBackfill = null,
        demoMode = true,
    )
}
