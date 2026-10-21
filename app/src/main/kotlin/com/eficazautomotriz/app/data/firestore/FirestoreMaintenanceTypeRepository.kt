package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.MaintenanceTypeMapper
import com.eficazautomotriz.data.MaintenanceTypeRepository
import com.eficazautomotriz.domain.model.MaintenanceType
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreMaintenanceTypeRepository(db: FirebaseFirestore) :
    FirestoreRepository<MaintenanceType>(
        db, MaintenanceTypeMapper.COLLECTION, "MNT", MaintenanceTypeMapper::toMap, MaintenanceTypeMapper::fromDoc,
    ),
    MaintenanceTypeRepository {

    override fun findActive(): List<MaintenanceType> = query("active" to true)
}