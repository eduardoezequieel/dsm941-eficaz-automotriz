package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
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
}