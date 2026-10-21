package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.AppointmentMapper
import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

/**
 * Every write keeps `ocupacion/{yyyy-MM-dd_slotId}` in step with the appointment, in the same
 * transaction, so availability is computed from those counters and a client never reads
 * other clients' appointments.
 */
class FirestoreAppointmentRepository(db: FirebaseFirestore) :
    FirestoreRepository<Appointment>(
        db, AppointmentMapper.COLLECTION, "APT", AppointmentMapper::toMap, AppointmentMapper::fromDoc,
    ),
    AppointmentRepository {

    private val occupancy: CollectionReference get() = db.collection(SupportDocuments.OCCUPANCY)

    override fun findByClient(clientId: String): List<Appointment> = query("clientId" to clientId)

    override fun findByVehicle(vehicleId: String): List<Appointment> = query("vehicleId" to vehicleId)

    override fun findByStatus(status: AppointmentStatus): List<Appointment> = query("status" to status.name)

    /** ISO dates (yyyy-mm-dd) sort the same as text and as dates. */
    override fun findByDateRange(from: LocalDate, to: LocalDate): List<Appointment> = run(
        col.whereGreaterThanOrEqualTo("date", FirestoreCodecs.encode(from))
            .whereLessThanOrEqualTo("date", FirestoreCodecs.encode(to))
    )

    override fun occupancyOn(date: LocalDate): Map<String, Int> = SupportDocuments.occupancyBySlot(
        Tasks.await(occupancy.whereEqualTo(SupportDocuments.OCCUPANCY_DATE, FirestoreCodecs.encode(date)).get())
            .documents.map { it.data.orEmpty() }
    )

    /**
     * Reads the previous version and the affected counters first (a transaction requires every
     * read before any write), then writes the ±1 changes and the appointment.
     */
    override fun persist(entity: Appointment) {
        val appointmentRef = col.document(entity.id)
        await(
            db.runTransaction { tx ->
                val before = tx.get(appointmentRef).takeIf { it.exists() }?.let(fromDoc)
                val changes = SupportDocuments.occupancyChanges(before, entity)
                val counts = changes.map { change ->
                    tx.get(occupancy.document(change.key)).getLong(SupportDocuments.OCCUPANCY_COUNT)
                }
                changes.zip(counts).forEach { (change, current) ->
                    val next = SupportDocuments.nextCount(current, change.delta) ?: return@forEach
                    tx.set(occupancy.document(change.key), SupportDocuments.occupancy(change.date, change.slotId, next))
                }
                tx.set(appointmentRef, toMap(entity))
                null
            }
        )
    }

    /**
     * Staff backfill: creates the counters missing for appointments stored before they existed.
     * An existing counter is never overwritten.
     */
    fun backfillOccupancy() {
        val existing = Tasks.await(occupancy.get()).documents.map { it.id }.toSet()
        SupportDocuments.occupancyFrom(findAll()).filterKeys { it !in existing }.forEach { (key, document) ->
            val ref = occupancy.document(key)
            await(
                db.runTransaction { tx ->
                    if (!tx.get(ref).exists()) tx.set(ref, document)
                    null
                }
            )
        }
    }
}
