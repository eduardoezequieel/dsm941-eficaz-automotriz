package com.eficazautomotriz.data

/**
 * Contenedor de las dependencias de persistencia. Se construye una sola vez en Main
 * y se inyecta por constructor: no hay singletons mutables globales.
 */
data class Repositories(
    val users: UserRepository = UserRepository(),
    val vehicles: VehicleRepository = VehicleRepository(),
    val appointments: AppointmentRepository = AppointmentRepository(),
    val serviceOrders: ServiceOrderRepository = ServiceOrderRepository(),
    val evidences: EvidenceRepository = EvidenceRepository(),
    val maintenanceRecords: MaintenanceRecordRepository = MaintenanceRecordRepository(),
    val serviceTypes: ServiceTypeRepository = ServiceTypeRepository(),
    val maintenanceTypes: MaintenanceTypeRepository = MaintenanceTypeRepository(),
    val timeSlots: TimeSlotRepository = TimeSlotRepository(),
)
