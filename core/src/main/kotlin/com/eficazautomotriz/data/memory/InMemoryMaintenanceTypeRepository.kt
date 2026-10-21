package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.MaintenanceTypeRepository
import com.eficazautomotriz.domain.model.MaintenanceType

class InMemoryMaintenanceTypeRepository :
    InMemoryRepository<MaintenanceType>("MNT"), MaintenanceTypeRepository {

    override fun findActive(): List<MaintenanceType> = findAll().filter { it.active }
}