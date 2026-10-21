package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.ServiceOrderRepository
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.ServiceStatus

class InMemoryServiceOrderRepository : InMemoryRepository<ServiceOrder>("SRV"), ServiceOrderRepository {

    override fun findByVehicle(vehicleId: String): List<ServiceOrder> =
        findAll().filter { it.vehicleId == vehicleId }

    override fun findByStatus(status: ServiceStatus): List<ServiceOrder> =
        findAll().filter { it.status == status }
}