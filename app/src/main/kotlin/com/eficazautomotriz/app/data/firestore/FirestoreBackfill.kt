package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.logging.ErrorLogger

/**
 * Rebuilds the support documents (`contadores`, `placas`, `ocupacion`) from the full
 * collections, for data written before they existed. It needs collection-wide reads, so
 * only staff can run it. It is idempotent: counters are only raised, and a claim or an
 * occupancy document that already exists is never overwritten. Each step fails on its own
 * and is logged, never thrown. Blocking: call it on IO.
 */
class FirestoreBackfill(
    private val counted: List<FirestoreRepository<*>>,
    private val vehicles: FirestoreVehicleRepository,
    private val appointments: FirestoreAppointmentRepository,
    private val logger: ErrorLogger,
) {

    fun run() {
        counted.forEach { repository -> step("contadores") { repository.backfillCounter() } }
        step("placas") { vehicles.backfillPlates() }
        step("ocupacion") { appointments.backfillOccupancy() }
    }

    private fun step(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.error("FirestoreBackfill.$name", e::class.simpleName ?: "Exception", e.message ?: "no detail")
        }
    }
}
