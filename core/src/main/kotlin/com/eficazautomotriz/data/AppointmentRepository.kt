package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AppointmentRepository : Repository<Appointment> {
    fun findByClient(clientId: String): List<Appointment>
    fun findByVehicle(vehicleId: String): List<Appointment>
    fun findByStatus(status: AppointmentStatus): List<Appointment>

    /** Both ends included. */
    fun findByDateRange(from: LocalDate, to: LocalDate): List<Appointment>

    /**
     * Places taken on that date, by slot id: only appointments whose status occupies a slot.
     * Availability uses this instead of `findAll`, so a client never needs to read other
     * clients' appointments (Firestore keeps it as per-slot counters).
     */
    fun occupancyOn(date: LocalDate): Map<String, Int>

    /**
     * Live version of [findByClient]. Declared on this interface because the in-memory
     * repository emits it from a MutableStateFlow; use cases stay synchronous.
     * Firestore must filter by clientId so the list rule accepts the query.
     */
    fun observeByClient(clientId: String): Flow<List<Appointment>>

    /**
     * Live version of findByStatus(PENDING). A client query filtered only by status
     * is denied by firestore.rules; staff may listen.
     */
    fun observePending(): Flow<List<Appointment>>

    /**
     * Every appointment. Staff panel listens here so a cancellation of a confirmed
     * appointment updates the agenda, not only the pending list. Rules allow the
     * unfiltered list for staff and deny it for a client.
     */
    fun observeAll(): Flow<List<Appointment>>
}
