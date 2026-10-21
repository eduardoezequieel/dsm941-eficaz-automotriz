package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.VehicleMapper
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.model.Vehicle
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Each vehicle claims its plate in `placas/{PLATE}` in the same transaction that creates it.
 * Any signed-in user may read a claim (it only reveals that the plate exists), so the
 * uniqueness check never needs to read another owner's vehicle.
 */
class FirestoreVehicleRepository(db: FirebaseFirestore) :
    FirestoreRepository<Vehicle>(db, VehicleMapper.COLLECTION, "VEH", VehicleMapper::toMap, VehicleMapper::fromDoc),
    VehicleRepository {

    private val plates: CollectionReference get() = db.collection(SupportDocuments.PLATES)

    override fun findByOwner(ownerId: String): List<Vehicle> = query("ownerId" to ownerId)

    /** Null when the plate is free or when the caller may not read that vehicle (another owner's). */
    override fun findByPlate(plate: String): Vehicle? {
        val claim = Tasks.await(plates.document(SupportDocuments.plateKey(plate)).get())
        val vehicleId = claim.getString(SupportDocuments.PLATE_VEHICLE_ID) ?: return null
        return try {
            await(col.document(vehicleId).get()).takeIf { it.exists() }?.let(fromDoc)
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            null
        }
    }

    override fun plateExists(plate: String): Boolean =
        Tasks.await(plates.document(SupportDocuments.plateKey(plate)).get()).exists()

    /**
     * Creating a vehicle also creates its plate claim; if another vehicle claimed the plate in
     * the meantime the transaction fails. An update only writes the vehicle, and recreates the
     * claim if it is missing (vehicles stored before the claims existed).
     */
    override fun persist(entity: Vehicle) {
        val vehicleRef = col.document(entity.id)
        val plateRef = plates.document(SupportDocuments.plateKey(entity.plate))
        await(
            db.runTransaction { tx ->
                val isNew = !tx.get(vehicleRef).exists()
                val claimedBy = tx.get(plateRef).getString(SupportDocuments.PLATE_VEHICLE_ID)
                when {
                    claimedBy == null -> tx.set(plateRef, SupportDocuments.plateClaim(entity))
                    isNew && claimedBy != entity.id ->
                        throw IllegalStateException("La placa ${entity.plate} ya esta registrada")
                }
                tx.set(vehicleRef, toMap(entity))
                null
            }
        )
    }

    /** Deletes the vehicle and releases its plate claim in one transaction. */
    override fun deleteById(id: String): Boolean {
        val vehicleRef = col.document(id)
        return await(
            db.runTransaction { tx ->
                val snapshot = tx.get(vehicleRef)
                if (!snapshot.exists()) return@runTransaction false
                val plateRef = plates.document(SupportDocuments.plateKey(fromDoc(snapshot).plate))
                val claimedBy = tx.get(plateRef).getString(SupportDocuments.PLATE_VEHICLE_ID)
                tx.delete(vehicleRef)
                if (claimedBy == id) tx.delete(plateRef)
                true
            }
        )
    }

    /** Staff backfill: claims the plate of every stored vehicle that has no claim yet. */
    fun backfillPlates() {
        val claimed = Tasks.await(plates.get()).documents.map { it.id }.toSet()
        findAll().filter { SupportDocuments.plateKey(it.plate) !in claimed }.forEach { vehicle ->
            val plateRef = plates.document(SupportDocuments.plateKey(vehicle.plate))
            await(
                db.runTransaction { tx ->
                    if (!tx.get(plateRef).exists()) tx.set(plateRef, SupportDocuments.plateClaim(vehicle))
                    null
                }
            )
        }
    }
}
