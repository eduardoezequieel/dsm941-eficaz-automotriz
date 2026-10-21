package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.ServiceOrderMapper
import com.eficazautomotriz.data.ServiceOrderRepository
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreServiceOrderRepository(db: FirebaseFirestore) :
    FirestoreRepository<ServiceOrder>(
        db, ServiceOrderMapper.COLLECTION, "SRV", ServiceOrderMapper::toMap, ServiceOrderMapper::fromDoc,
    ),
    ServiceOrderRepository {

    override fun findByVehicle(vehicleId: String): List<ServiceOrder> = query("vehicleId" to vehicleId)

    override fun findByStatus(status: ServiceStatus): List<ServiceOrder> = query("status" to status.name)
}
