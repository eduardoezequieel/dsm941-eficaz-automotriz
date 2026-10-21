package com.eficazautomotriz.app.debug

import android.util.Log
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.domain.error.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Phase 3 smoke test against a real Firebase project. It walks through:
 * sign-up → `usuarios/{uid}` → `vehicleService.register` → maintenance baseline,
 * and logs the created ids to Logcat (`adb logcat -s EficazSmoke`).
 *
 * How to enable it:
 * 1. Copy the real `google-services.json` into `app/` (without it Firebase is not initialized).
 * 2. Enable Email/Password in Firebase Auth.
 * 3. Rules: `firestore.rules` restricts reads to the owner; `register` relies on the support
 *    documents `contadores` (ids) and `placas` (plate uniqueness), which any signed-in user may
 *    write within their rules. Catalog seeding still requires a STAFF account, so load the
 *    catalogs first or use the Firestore emulator. The baseline step writes
 *    `estadosMantenimiento`, allowed for the vehicle's owner.
 * 4. Set `SMOKE_ENABLED = true` in `MainActivity`, install the debug APK and open the app.
 *
 * `VehicleService.register` does not create maintenance records: the baseline is created by
 * `MaintenanceService.registerBaseline`, which is called explicitly here.
 */
suspend fun runSmoke(graph: AppGraph, email: String, password: String) {
    try {
        val uid = graph.authGateway.registerWithEmail("Cliente Prueba", "7000-0000", email, password)
        val user = withContext(Dispatchers.IO) { graph.repositories.users.findById(uid) }
            ?: error("usuarios/$uid was not created")
        Log.i(TAG, "usuarios/$uid created (${user.role})")

        Log.i(TAG, "catalogs ready: ${graph.ensureCatalogs()}")

        val mileage = 30_000
        val plate = "P" + (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
        val outcome = withContext(Dispatchers.IO) {
            graph.vehicleService.register(user, uid, plate, "Toyota", "Corolla", 2020, mileage, LocalDate.now())
        }
        when (outcome) {
            is Outcome.Failure -> Log.e(TAG, "register failed: ${outcome.error.message}")
            is Outcome.Success -> {
                val vehicle = outcome.value
                Log.i(TAG, "vehiculos/${vehicle.id} created (plate ${vehicle.plate})")
                val record = withContext(Dispatchers.IO) {
                    graph.maintenanceService.registerBaseline(vehicle.id, SMOKE_MAINTENANCE_TYPE, mileage)
                }
                Log.i(TAG, "estadosMantenimiento/${record.id} created (${record.maintenanceTypeId})")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Smoke test aborted", e)
    } finally {
        graph.authGateway.signOut()
    }
}

private const val TAG = "EficazSmoke"
private const val SMOKE_MAINTENANCE_TYPE = "MNT-001"