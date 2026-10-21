package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.AppointmentRepository
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import java.time.LocalDate

class InMemoryAppointmentRepository : InMemoryRepository<Appointment>("APT"), AppointmentRepository {

    override fun findByClient(clientId: String): List<Appointment> =
        findAll().filter { it.clientId == clientId }

    override fun findByVehicle(vehicleId: String): List<Appointment> =
        findAll().filter { it.vehicleId == vehicleId }

    override fun findByStatus(status: AppointmentStatus): List<Appointment> =
        findAll().filter { it.status == status }

    override fun findByDateRange(from: LocalDate, to: LocalDate): List<Appointment> =
        findAll().filter { it.date >= from && it.date <= to }

    override fun occupancyOn(date: LocalDate): Map<String, Int> =
        findAll().filter { it.date == date && it.status.occupiesSlot }.groupingBy { it.slotId }.eachCount()
}
