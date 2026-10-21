package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.MaintenanceRecord

interface MaintenanceRecordRepository : Repository<MaintenanceRecord> {
    fun findByVehicle(vehicleId: String): List<MaintenanceRecord>
    fun findByVehicleAndType(vehicleId: String, typeId: String): MaintenanceRecord?
}
