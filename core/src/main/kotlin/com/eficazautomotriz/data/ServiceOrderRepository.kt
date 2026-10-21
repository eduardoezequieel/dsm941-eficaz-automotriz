package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.ServiceStatus

interface ServiceOrderRepository : Repository<ServiceOrder> {
    fun findByVehicle(vehicleId: String): List<ServiceOrder>
    fun findByStatus(status: ServiceStatus): List<ServiceOrder>
}