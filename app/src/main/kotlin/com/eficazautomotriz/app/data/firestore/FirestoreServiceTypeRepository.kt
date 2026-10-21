package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.ServiceTypeMapper
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.domain.model.ServiceType
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreServiceTypeRepository(db: FirebaseFirestore) :
    FirestoreRepository<ServiceType>(
        db, ServiceTypeMapper.COLLECTION, "SVT", ServiceTypeMapper::toMap, ServiceTypeMapper::fromDoc,
    ),
    ServiceTypeRepository {

    override fun findActive(): List<ServiceType> = query("active" to true)

    override fun nameOf(serviceTypeId: String): String = findById(serviceTypeId)?.name ?: serviceTypeId
}
