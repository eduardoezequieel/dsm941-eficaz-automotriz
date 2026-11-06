package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.EvidenceMapper
import com.eficazautomotriz.data.EvidenceRepository
import com.eficazautomotriz.domain.model.Evidence
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class FirestoreEvidenceRepository(db: FirebaseFirestore) :
    FirestoreRepository<Evidence>(db, EvidenceMapper.COLLECTION, "EVD", EvidenceMapper::toMap, EvidenceMapper::fromDoc),
    EvidenceRepository {

    override fun findByServiceOrder(serviceOrderId: String): List<Evidence> =
        query("serviceOrderId" to serviceOrderId)

    override fun observeByOrder(serviceOrderId: String): Flow<List<Evidence>> =
        observe(col.whereEqualTo("serviceOrderId", serviceOrderId))
}
