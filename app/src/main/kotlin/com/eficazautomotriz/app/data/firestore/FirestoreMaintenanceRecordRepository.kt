package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.MaintenanceRecordMapper
import com.eficazautomotriz.data.MaintenanceRecordRepository
import com.eficazautomotriz.domain.model.MaintenanceRecord
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreMaintenanceRecordRepository(db: FirebaseFirestore) :
    FirestoreRepository<MaintenanceRecord>(
        db, MaintenanceRecordMapper.COLLECTION, "MRC", MaintenanceRecordMapper::toMap, MaintenanceRecordMapper::fromDoc,
    ),
    MaintenanceRecordRepository {

    override fun findByVehicle(vehicleId: String): List<MaintenanceRecord> = query("vehicleId" to vehicleId)

    override fun findByVehicleAndType(vehicleId: String, typeId: String): MaintenanceRecord? =
        query("vehicleId" to vehicleId, "maintenanceTypeId" to typeId).firstOrNull()
}