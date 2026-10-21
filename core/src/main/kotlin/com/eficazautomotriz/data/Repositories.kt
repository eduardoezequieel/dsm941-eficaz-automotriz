package com.eficazautomotriz.data

import com.eficazautomotriz.data.memory.InMemoryAppointmentRepository
import com.eficazautomotriz.data.memory.InMemoryEvidenceRepository
import com.eficazautomotriz.data.memory.InMemoryMaintenanceRecordRepository
import com.eficazautomotriz.data.memory.InMemoryMaintenanceTypeRepository
import com.eficazautomotriz.data.memory.InMemoryServiceOrderRepository
import com.eficazautomotriz.data.memory.InMemoryServiceTypeRepository
import com.eficazautomotriz.data.memory.InMemoryTimeSlotRepository
import com.eficazautomotriz.data.memory.InMemoryUserRepository
import com.eficazautomotriz.data.memory.InMemoryVehicleRepository

/**
 * Container for the persistence dependencies. It is built once when the graph is assembled
 * and injected through the constructor: there are no global mutable singletons. By default it uses
 * the in-memory implementations; the Android app passes it the Firestore ones.
 */
data class Repositories(
    val users: UserRepository = InMemoryUserRepository(),
    val vehicles: VehicleRepository = InMemoryVehicleRepository(),
    val appointments: AppointmentRepository = InMemoryAppointmentRepository(),
    val serviceOrders: ServiceOrderRepository = InMemoryServiceOrderRepository(),
    val evidences: EvidenceRepository = InMemoryEvidenceRepository(),
    val maintenanceRecords: MaintenanceRecordRepository = InMemoryMaintenanceRecordRepository(),
    val serviceTypes: ServiceTypeRepository = InMemoryServiceTypeRepository(),
    val maintenanceTypes: MaintenanceTypeRepository = InMemoryMaintenanceTypeRepository(),
    val timeSlots: TimeSlotRepository = InMemoryTimeSlotRepository(),
)