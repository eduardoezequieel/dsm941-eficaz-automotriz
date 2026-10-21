package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.EvidenceMapper
import com.eficazautomotriz.data.EvidenceRepository
import com.eficazautomotriz.domain.model.Evidence
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreEvidenceRepository(db: FirebaseFirestore) :
    FirestoreRepository<Evidence>(db, EvidenceMapper.COLLECTION, "EVD", EvidenceMapper::toMap, EvidenceMapper::fromDoc),
    EvidenceRepository {

    override fun findByServiceOrder(serviceOrderId: String): List<Evidence> =
        query("serviceOrderId" to serviceOrderId)
}