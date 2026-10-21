package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.MaintenanceRecordRepository
import com.eficazautomotriz.domain.model.MaintenanceRecord

class InMemoryMaintenanceRecordRepository :
    InMemoryRepository<MaintenanceRecord>("MRC"), MaintenanceRecordRepository {

    override fun findByVehicle(vehicleId: String): List<MaintenanceRecord> =
        findAll().filter { it.vehicleId == vehicleId }

    override fun findByVehicleAndType(vehicleId: String, typeId: String): MaintenanceRecord? =
        findAll().firstOrNull { it.vehicleId == vehicleId && it.maintenanceTypeId == typeId }
}
