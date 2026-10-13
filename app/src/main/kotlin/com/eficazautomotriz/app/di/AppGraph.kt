package com.eficazautomotriz.app.di

import android.content.Context
import android.net.Uri
import com.eficazautomotriz.app.data.ImageUploader
import com.eficazautomotriz.app.data.auth.AuthGateway
import com.eficazautomotriz.app.data.cloudinary.ImageCompressor
import com.eficazautomotriz.app.logging.AndroidErrorLogger
import com.eficazautomotriz.application.AppointmentService
import com.eficazautomotriz.application.AuthService
import com.eficazautomotriz.application.CatalogService
import com.eficazautomotriz.application.MaintenanceService
import com.eficazautomotriz.application.ReportService
import com.eficazautomotriz.application.ServiceOrderService
import com.eficazautomotriz.application.VehicleService
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.data.SeedData
import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.model.enums.UserRole
import com.eficazautomotriz.domain.rules.AppointmentAvailabilityRule
import com.eficazautomotriz.domain.rules.MaintenanceStatusRule
import com.eficazautomotriz.domain.rules.ServiceTransitionRule
import com.eficazautomotriz.logging.ErrorLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hand-wired dependency graph (no Hilt or Koin), equivalent to Stage 2's
 * `Main.buildConsoleApp` but with the repositories of a [DataBackend] and the Android logger.
 *
 * Created once per process through `EficazApp.graph`. With the default backend
 * ([firebaseBackend]) it requires an initialized Firebase app (`app/google-services.json`).
 * Debug builds with `demo.inMemory=true` pass the in-memory demo backend instead.
 */
class AppGraph(context: Context, demoBackend: DataBackend? = null) {

    private val appContext = context.applicationContext

    val config = SystemConfig(
        errorLogPath = File(appContext.filesDir, "logs/errors.log").path,
        // Demo only. 900 km still reads as "Próximo", so f2 can name the Frontier oil
        // change. Production keeps the 500 km default. Same value as
        // MockupSeedData.DEMO_WARNING_THRESHOLD_KM (debug source; not visible here).
        warningThresholdKm = if (demoBackend != null) 1_000 else 500,
    )
    val errorLogger: ErrorLogger = AndroidErrorLogger(appContext.filesDir)

    private val backend: DataBackend = demoBackend ?: firebaseBackend(errorLogger)

    /** True in the offline demo (seeded mockup data, demo accounts, no Firebase). */
    val demoMode: Boolean = backend.demoMode

    /** Dispatcher for blocking repository/use-case calls (see [DataBackend.io]). */
    val io: CoroutineDispatcher = backend.io

    val repositories: Repositories = backend.repositories

    val authGateway: AuthGateway = backend.authGateway

    val authService = AuthService(repositories.users, errorLogger)

    val maintenanceService = MaintenanceService(
        repositories.vehicles,
        repositories.maintenanceRecords,
        repositories.maintenanceTypes,
        MaintenanceStatusRule(),
        config,
        errorLogger,
    )

    val vehicleService = VehicleService(
        repositories.vehicles,
        repositories.maintenanceRecords,
        config,
        errorLogger,
    )

    val appointmentService = AppointmentService(
        repositories.appointments,
        repositories.vehicles,
        repositories.timeSlots,
        repositories.serviceTypes,
        AppointmentAvailabilityRule(),
        config,
        errorLogger,
    )

    val serviceOrderService = ServiceOrderService(
        repositories.serviceOrders,
        repositories.evidences,
        repositories.vehicles,
        repositories.appointments,
        repositories.serviceTypes,
        maintenanceService,
        vehicleService,
        ServiceTransitionRule(),
        config,
        errorLogger,
    )

    val reportService = ReportService(
        repositories.appointments,
        repositories.serviceOrders,
        repositories.serviceTypes,
        repositories.vehicles,
        maintenanceService,
    )

    /** Service catalog and maintenance intervals. Staff writes; other screens keep using the filtered reads. */
    val catalog = CatalogService(
        repositories.serviceTypes,
        repositories.maintenanceTypes,
        config,
        errorLogger,
    )

    /** Evidence uploads (Cloudinary in production, a fake URL in the demo). */
    val cloudinaryUploader: ImageUploader = backend.imageUploader

    val cloudinaryConfigured: Boolean = backend.imageUploaderConfigured

    /** Process-wide scope for fire-and-forget housekeeping that must outlive a ViewModel. */
    private val housekeeping = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Deletes a camera capture from `cacheDir/images` (FileProvider Uri). Gallery Uris belong to
     * other apps and are never touched. Runs on IO; failures are ignored.
     */
    fun discardTempImage(uri: Uri) {
        if (uri.authority != "${appContext.packageName}.fileprovider") return
        housekeeping.launch { runCatching { appContext.contentResolver.delete(uri, null, null) } }
    }

    /** Shrinks the photo to a JPEG of at most 1280 px. Blocking: call it on IO. */
    fun compressImage(uri: Uri): ByteArray = ImageCompressor.compress(appContext, uri)

    /**
     * Seeds the 3 catalogs when `tiposServicio` is empty. Returns false on failure (for
     * example, the rules only let staff write catalogs); the error is logged.
     *
     * For a STAFF session it also starts the idempotent backfill of contadores/placas/ocupacion
     * in the background, so the first staff login after deploying the rules rebuilds them
     * without delaying the sign-in.
     */
    suspend fun ensureCatalogs(): Boolean = withContext(io) {
        try {
            if (repositories.serviceTypes.findAll().isEmpty()) {
                SeedData.loadCatalogs(repositories.serviceTypes, repositories.maintenanceTypes, repositories.timeSlots)
            }
            true
        } catch (e: Exception) {
            errorLogger.error("AppGraph.ensureCatalogs", e::class.simpleName ?: "Exception", e.message ?: "no detail")
            false
        } finally {
            startBackfillIfStaff()
        }
    }

    private fun startBackfillIfStaff() {
        val backfill = backend.staffBackfill ?: return
        housekeeping.launch {
            try {
                val uid = authGateway.currentUid() ?: return@launch
                if (repositories.users.findById(uid)?.role == UserRole.STAFF) backfill()
            } catch (e: Exception) {
                errorLogger.error("AppGraph.backfill", e::class.simpleName ?: "Exception", e.message ?: "no detail")
            }
        }
    }
}