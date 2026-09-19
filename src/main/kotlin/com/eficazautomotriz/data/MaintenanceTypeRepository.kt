package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.MaintenanceType

class MaintenanceTypeRepository : InMemoryRepository<MaintenanceType>("MNT") {

    fun findActive(): List<MaintenanceType> = findAll().filter { it.active }
}
