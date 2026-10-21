package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.boolean
import com.eficazautomotriz.app.data.firestore.int
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.MaintenanceType
import com.google.firebase.firestore.DocumentSnapshot

object MaintenanceTypeMapper {
    const val COLLECTION = "tiposMantenimiento"

    fun toMap(t: MaintenanceType): Map<String, Any?> = mapOf(
        "id" to t.id, "name" to t.name, "intervalKm" to t.intervalKm, "active" to t.active,
    )

    fun fromMap(m: Map<String, Any?>): MaintenanceType = MaintenanceType(
        id = m.string("id"), name = m.string("name"), intervalKm = m.int("intervalKm"),
        active = m.boolean("active"),
    )

    fun fromDoc(d: DocumentSnapshot): MaintenanceType = fromMap(requireData(d))
}
