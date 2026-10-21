package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.FirestoreCodecs
import com.eficazautomotriz.app.data.firestore.int
import com.eficazautomotriz.app.data.firestore.localDate
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.Vehicle
import com.google.firebase.firestore.DocumentSnapshot

object VehicleMapper {
    const val COLLECTION = "vehiculos"

    fun toMap(v: Vehicle): Map<String, Any?> = mapOf(
        "id" to v.id, "ownerId" to v.ownerId, "plate" to v.plate, "make" to v.make,
        "model" to v.model, "year" to v.year, "currentMileage" to v.currentMileage,
        "mileageUpdatedAt" to FirestoreCodecs.encode(v.mileageUpdatedAt),
    )

    fun fromMap(m: Map<String, Any?>): Vehicle = Vehicle(
        id = m.string("id"), ownerId = m.string("ownerId"), plate = m.string("plate"),
        make = m.string("make"), model = m.string("model"),
        year = m.int("year"), currentMileage = m.int("currentMileage"),
        mileageUpdatedAt = m.localDate("mileageUpdatedAt"),
    )

    fun fromDoc(d: DocumentSnapshot): Vehicle = fromMap(requireData(d))
}
