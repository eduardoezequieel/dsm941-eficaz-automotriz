package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import java.time.LocalDate

class AppointmentRepository : InMemoryRepository<Appointment>("APT") {

    fun findByClient(clientId: String): List<Appointment> =
        findAll().filter { it.clientId == clientId }

    fun findByVehicle(vehicleId: String): List<Appointment> =
        findAll().filter { it.vehicleId == vehicleId }

    fun findByStatus(status: AppointmentStatus): List<Appointment> =
        findAll().filter { it.status == status }

    fun findByDateRange(from: LocalDate, to: LocalDate): List<Appointment> =
        findAll().filter { it.date >= from && it.date <= to }
}
