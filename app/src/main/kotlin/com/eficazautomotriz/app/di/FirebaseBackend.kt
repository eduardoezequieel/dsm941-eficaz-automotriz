package com.eficazautomotriz.app.di

import com.eficazautomotriz.app.BuildConfig
import com.eficazautomotriz.app.data.auth.FirebaseAuthGateway
import com.eficazautomotriz.app.data.cloudinary.CloudinaryUploader
import com.eficazautomotriz.app.data.firestore.FirestoreAppointmentRepository
import com.eficazautomotriz.app.data.firestore.FirestoreBackfill
import com.eficazautomotriz.app.data.firestore.FirestoreEvidenceRepository
import com.eficazautomotriz.app.data.firestore.FirestoreMaintenanceRecordRepository
import com.eficazautomotriz.app.data.firestore.FirestoreMaintenanceTypeRepository
import com.eficazautomotriz.app.data.firestore.FirestoreServiceOrderRepository
import com.eficazautomotriz.app.data.firestore.FirestoreServiceTypeRepository
import com.eficazautomotriz.app.data.firestore.FirestoreTimeSlotRepository
import com.eficazautomotriz.app.data.firestore.FirestoreUserRepository
import com.eficazautomotriz.app.data.firestore.FirestoreVehicleRepository
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.logging.ErrorLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.Dispatchers

/**
 * Production backend: Firestore repositories, Firebase Auth and Cloudinary. Requires an
 * initialized Firebase app (`app/google-services.json`); otherwise `getInstance()` throws.
 */
fun firebaseBackend(errorLogger: ErrorLogger): DataBackend {
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // On-disk offline cache. It must be set before any other use of the instance.
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }

    val vehicleRepository = FirestoreVehicleRepository(firestore)
    val appointmentRepository = FirestoreAppointmentRepository(firestore)
    val serviceOrderRepository = FirestoreServiceOrderRepository(firestore)
    val evidenceRepository = FirestoreEvidenceRepository(firestore)
    val maintenanceRecordRepository = FirestoreMaintenanceRecordRepository(firestore)
    val serviceTypeRepository = FirestoreServiceTypeRepository(firestore)
    val maintenanceTypeRepository = FirestoreMaintenanceTypeRepository(firestore)
    val timeSlotRepository = FirestoreTimeSlotRepository(firestore)

    val repositories = Repositories(
        users = FirestoreUserRepository(firestore),
        vehicles = vehicleRepository,
        appointments = appointmentRepository,
        serviceOrders = serviceOrderRepository,
        evidences = evidenceRepository,
        maintenanceRecords = maintenanceRecordRepository,
        serviceTypes = serviceTypeRepository,
        maintenanceTypes = maintenanceTypeRepository,
        timeSlots = timeSlotRepository,
    )

    /** Rebuilds contadores/placas/ocupacion for data stored before they existed (staff only). */
    val backfill = FirestoreBackfill(
        counted = listOf(
            vehicleRepository, appointmentRepository, serviceOrderRepository, evidenceRepository,
            maintenanceRecordRepository, serviceTypeRepository, maintenanceTypeRepository, timeSlotRepository,
        ),
        vehicles = vehicleRepository,
        appointments = appointmentRepository,
        logger = errorLogger,
    )

    return DataBackend(
        repositories = repositories,
        authGateway = FirebaseAuthGateway(FirebaseAuth.getInstance(), repositories.users),
        // Evidence uploads to Cloudinary (unsigned preset); credentials come from local.properties.
        imageUploader = CloudinaryUploader(BuildConfig.CLOUDINARY_CLOUD_NAME, BuildConfig.CLOUDINARY_UPLOAD_PRESET),
        imageUploaderConfigured =
            BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() && BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank(),
        io = Dispatchers.IO,
        staffBackfill = backfill::run,
        demoMode = false,
    )
}