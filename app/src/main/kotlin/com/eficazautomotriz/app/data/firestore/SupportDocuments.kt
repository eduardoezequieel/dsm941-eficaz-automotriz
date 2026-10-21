package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.Vehicle
import java.time.LocalDate

/**
 * Support collections that let the rules restrict reads to the owner (or staff) without
 * breaking the operations that used to scan whole collections:
 * - `contadores/{collection}` `{ value }`: last number issued for the readable ids (VEH-007).
 * - `placas/{PLATE}` `{ vehicleId, ownerId }`: plate claim, the uniqueness check.
 * - `ocupacion/{yyyy-MM-dd_slotId}` `{ date, slotId, count }`: places taken per slot and date.
 *
 * Only pure helpers live here (no Firebase types), so they are unit tested on the JVM.
 */
object SupportDocuments {

    const val COUNTERS = "contadores"
    const val PLATES = "placas"
    const val OCCUPANCY = "ocupacion"

    const val COUNTER_VALUE = "value"
    const val PLATE_VEHICLE_ID = "vehicleId"
    const val PLATE_OWNER_ID = "ownerId"
    const val OCCUPANCY_DATE = "date"
    const val OCCUPANCY_SLOT_ID = "slotId"
    const val OCCUPANCY_COUNT = "count"

    private const val ID_DIGITS = 3

    /** Plates are stored in upper case (VehicleService normalizes them). */
    fun plateKey(plate: String): String = plate.trim().uppercase()

    /** The rules check that the document id is exactly `date + '_' + slotId`. */
    fun occupancyKey(date: LocalDate, slotId: String): String = "${FirestoreCodecs.encode(date)}_$slotId"

    fun plateClaim(vehicle: Vehicle): Map<String, Any?> =
        mapOf(PLATE_VEHICLE_ID to vehicle.id, PLATE_OWNER_ID to vehicle.ownerId)

    fun occupancy(date: LocalDate, slotId: String, count: Long): Map<String, Any?> = mapOf(
        OCCUPANCY_DATE to FirestoreCodecs.encode(date),
        OCCUPANCY_SLOT_ID to slotId,
        OCCUPANCY_COUNT to count,
    )

    /** A ±1 change on one `ocupacion` document. */
    data class OccupancyChange(val date: LocalDate, val slotId: String, val delta: Int) {
        val key: String get() = occupancyKey(date, slotId)
    }

    /**
     * What saving [after] over [before] (null for a new appointment) does to the occupancy:
     * +1 on the slot it starts occupying and -1 on the one it stops occupying, whether by a
     * status change (cancel, reject, attended) or by a reschedule to another date or slot.
     */
    fun occupancyChanges(before: Appointment?, after: Appointment): List<OccupancyChange> {
        val released = before?.takeIf { it.status.occupiesSlot }?.let { OccupancyChange(it.date, it.slotId, -1) }
        val taken = after.takeIf { it.status.occupiesSlot }?.let { OccupancyChange(it.date, it.slotId, +1) }
        if (released != null && taken != null && released.key == taken.key) return emptyList()
        return listOfNotNull(released, taken)
    }

    /**
     * Count after applying [delta] to [current] (null when the document does not exist).
     * Null means "skip the write": releasing a place that was never counted (an appointment
     * created before the counters existed) must not push the count below zero.
     */
    fun nextCount(current: Long?, delta: Int): Long? {
        val next = (current ?: 0L) + delta
        return next.takeIf { it >= 0 }
    }

    /** Places taken per slot from the `ocupacion` documents of a single date. */
    fun occupancyBySlot(documents: List<Map<String, Any?>>): Map<String, Int> =
        documents.mapNotNull { doc ->
            val slotId = doc[OCCUPANCY_SLOT_ID] as? String ?: return@mapNotNull null
            val count = (doc[OCCUPANCY_COUNT] as? Number)?.toInt() ?: 0
            if (count > 0) slotId to count else null
        }.toMap()

    /**
     * The `ocupacion` documents rebuilt from the full list of appointments (staff backfill),
     * keyed by document id.
     */
    fun occupancyFrom(appointments: List<Appointment>): Map<String, Map<String, Any?>> =
        appointments.filter { it.status.occupiesSlot }
            .groupBy { occupancyKey(it.date, it.slotId) }
            .mapValues { (_, group) -> occupancy(group.first().date, group.first().slotId, group.size.toLong()) }

    /** Number part of a readable id; 0 when the id has another format (seeded by hand). */
    fun idNumber(id: String, prefix: String): Int = id.removePrefix("$prefix-").toIntOrNull() ?: 0

    fun formatId(prefix: String, number: Long): String = "$prefix-" + number.toString().padStart(ID_DIGITS, '0')
}