package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.ServiceStatus

class ServiceOrderRepository : InMemoryRepository<ServiceOrder>("SRV") {

    fun findByVehicle(vehicleId: String): List<ServiceOrder> =
        findAll().filter { it.vehicleId == vehicleId }

    fun findByStatus(status: ServiceStatus): List<ServiceOrder> =
        findAll().filter { it.status == status }
}
