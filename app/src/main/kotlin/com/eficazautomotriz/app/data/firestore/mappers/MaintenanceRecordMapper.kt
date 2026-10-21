package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.intOrNull
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.MaintenanceRecord
import com.google.firebase.firestore.DocumentSnapshot

object MaintenanceRecordMapper {
    const val COLLECTION = "estadosMantenimiento"

    fun toMap(r: MaintenanceRecord): Map<String, Any?> = mapOf(
        "id" to r.id, "vehicleId" to r.vehicleId, "maintenanceTypeId" to r.maintenanceTypeId,
        "lastServiceMileage" to r.lastServiceMileage,
    )

    fun fromMap(m: Map<String, Any?>): MaintenanceRecord = MaintenanceRecord(
        id = m.string("id"), vehicleId = m.string("vehicleId"), maintenanceTypeId = m.string("maintenanceTypeId"),
        lastServiceMileage = m.intOrNull("lastServiceMileage"),
    )

    fun fromDoc(d: DocumentSnapshot): MaintenanceRecord = fromMap(requireData(d))
}
