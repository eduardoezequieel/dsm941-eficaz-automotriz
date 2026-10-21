package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.FirestoreCodecs
import com.eficazautomotriz.app.data.firestore.enumValue
import com.eficazautomotriz.app.data.firestore.localDate
import com.eficazautomotriz.app.data.firestore.localDateTime
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.app.data.firestore.stringOrNull
import com.eficazautomotriz.domain.model.Appointment
import com.google.firebase.firestore.DocumentSnapshot

object AppointmentMapper {
    const val COLLECTION = "citas"

    fun toMap(a: Appointment): Map<String, Any?> = mapOf(
        "id" to a.id, "clientId" to a.clientId, "vehicleId" to a.vehicleId,
        "serviceTypeId" to a.serviceTypeId, "date" to FirestoreCodecs.encode(a.date),
        "slotId" to a.slotId, "status" to FirestoreCodecs.encode(a.status), "reason" to a.reason,
        "requestedAt" to FirestoreCodecs.encode(a.requestedAt),
    )

    fun fromMap(m: Map<String, Any?>): Appointment = Appointment(
        id = m.string("id"), clientId = m.string("clientId"), vehicleId = m.string("vehicleId"),
        serviceTypeId = m.string("serviceTypeId"), date = m.localDate("date"),
        slotId = m.string("slotId"), status = m.enumValue("status"), reason = m.stringOrNull("reason"),
        requestedAt = m.localDateTime("requestedAt"),
    )

    fun fromDoc(d: DocumentSnapshot): Appointment = fromMap(requireData(d))
}
