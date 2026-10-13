package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.boolean
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.ServiceType
import com.google.firebase.firestore.DocumentSnapshot

object ServiceTypeMapper {
    const val COLLECTION = "tiposServicio"

    fun toMap(t: ServiceType): Map<String, Any?> = mapOf(
        "id" to t.id, "name" to t.name, "description" to t.description, "active" to t.active,
    )

    fun fromMap(m: Map<String, Any?>): ServiceType = ServiceType(
        id = m.string("id"), name = m.string("name"), description = m.string("description"),
        active = m.boolean("active"),
    )

    fun fromDoc(d: DocumentSnapshot): ServiceType = fromMap(requireData(d))
}