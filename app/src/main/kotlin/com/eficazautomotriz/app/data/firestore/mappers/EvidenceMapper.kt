package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.FirestoreCodecs
import com.eficazautomotriz.app.data.firestore.enumValue
import com.eficazautomotriz.app.data.firestore.localDateTime
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.Evidence
import com.google.firebase.firestore.DocumentSnapshot

object EvidenceMapper {
    const val COLLECTION = "evidencias"

    fun toMap(e: Evidence): Map<String, Any?> = mapOf(
        "id" to e.id, "serviceOrderId" to e.serviceOrderId, "stage" to FirestoreCodecs.encode(e.stage),
        "imageUrl" to e.imageUrl, "uploadedAt" to FirestoreCodecs.encode(e.uploadedAt),
    )

    fun fromMap(m: Map<String, Any?>): Evidence = Evidence(
        id = m.string("id"), serviceOrderId = m.string("serviceOrderId"), stage = m.enumValue("stage"),
        imageUrl = m.string("imageUrl"), uploadedAt = m.localDateTime("uploadedAt"),
    )

    fun fromDoc(d: DocumentSnapshot): Evidence = fromMap(requireData(d))
}
