package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.FirestoreCodecs
import com.eficazautomotriz.app.data.firestore.boolean
import com.eficazautomotriz.app.data.firestore.dayOfWeek
import com.eficazautomotriz.app.data.firestore.int
import com.eficazautomotriz.app.data.firestore.localTime
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.TimeSlot
import com.google.firebase.firestore.DocumentSnapshot

object TimeSlotMapper {
    const val COLLECTION = "configFranjas"

    fun toMap(s: TimeSlot): Map<String, Any?> = mapOf(
        "id" to s.id, "dayOfWeek" to FirestoreCodecs.encode(s.dayOfWeek),
        "startTime" to FirestoreCodecs.encode(s.startTime), "endTime" to FirestoreCodecs.encode(s.endTime),
        "enabled" to s.enabled, "capacity" to s.capacity,
    )

    fun fromMap(m: Map<String, Any?>): TimeSlot = TimeSlot(
        id = m.string("id"), dayOfWeek = m.dayOfWeek("dayOfWeek"),
        startTime = m.localTime("startTime"), endTime = m.localTime("endTime"),
        enabled = m.boolean("enabled"), capacity = m.int("capacity"),
    )

    fun fromDoc(d: DocumentSnapshot): TimeSlot = fromMap(requireData(d))
}
