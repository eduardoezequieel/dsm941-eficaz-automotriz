package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlinx.coroutines.flow.Flow

interface ServiceOrderRepository : Repository<ServiceOrder> {
    fun findByVehicle(vehicleId: String): List<ServiceOrder>
    fun findByStatus(status: ServiceStatus): List<ServiceOrder>

    /**
     * The order as a one-element list, or empty when the id does not exist.
     * Firestore uses a document listener (get rule). A collection query by id would
     * not constrain vehicleId, which is what the list rule requires of a client.
     */
    fun observeById(id: String): Flow<List<ServiceOrder>>
}
