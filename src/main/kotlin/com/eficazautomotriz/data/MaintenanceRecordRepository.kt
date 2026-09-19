package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.MaintenanceRecord

class MaintenanceRecordRepository : InMemoryRepository<MaintenanceRecord>("MRC") {

    fun findByVehicle(vehicleId: String): List<MaintenanceRecord> =
        findAll().filter { it.vehicleId == vehicleId }

    fun findByVehicleAndType(vehicleId: String, typeId: String): MaintenanceRecord? =
        findAll().firstOrNull { it.vehicleId == vehicleId && it.maintenanceTypeId == typeId }
}
