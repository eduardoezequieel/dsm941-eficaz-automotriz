package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.FirestoreCodecs
import com.eficazautomotriz.app.data.firestore.enumValue
import com.eficazautomotriz.app.data.firestore.intOrNull
import com.eficazautomotriz.app.data.firestore.localDateTime
import com.eficazautomotriz.app.data.firestore.localDateTimeOrNull
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.app.data.firestore.stringOrNull
import com.eficazautomotriz.domain.model.ServiceOrder
import com.google.firebase.firestore.DocumentSnapshot

object ServiceOrderMapper {
    const val COLLECTION = "servicios"

    fun toMap(o: ServiceOrder): Map<String, Any?> = mapOf(
        "id" to o.id, "vehicleId" to o.vehicleId, "appointmentId" to o.appointmentId,
        "serviceTypeId" to o.serviceTypeId, "mileageAtService" to o.mileageAtService,
        "status" to FirestoreCodecs.encode(o.status), "notes" to o.notes,
        "receivedAt" to FirestoreCodecs.encode(o.receivedAt),
        "startedAt" to FirestoreCodecs.encodeOrNull(o.startedAt),
        "closedAt" to FirestoreCodecs.encodeOrNull(o.closedAt),
    )

    fun fromMap(m: Map<String, Any?>): ServiceOrder = ServiceOrder(
        id = m.string("id"), vehicleId = m.string("vehicleId"), appointmentId = m.stringOrNull("appointmentId"),
        serviceTypeId = m.string("serviceTypeId"), mileageAtService = m.intOrNull("mileageAtService"),
        status = m.enumValue("status"), notes = m.string("notes"),
        receivedAt = m.localDateTime("receivedAt"),
        startedAt = m.localDateTimeOrNull("startedAt"),
        closedAt = m.localDateTimeOrNull("closedAt"),
    )

    fun fromDoc(d: DocumentSnapshot): ServiceOrder = fromMap(requireData(d))
}