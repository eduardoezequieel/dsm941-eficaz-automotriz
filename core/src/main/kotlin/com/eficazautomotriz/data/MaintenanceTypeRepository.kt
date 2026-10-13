package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.MaintenanceType

interface MaintenanceTypeRepository : Repository<MaintenanceType> {
    fun findActive(): List<MaintenanceType>
}