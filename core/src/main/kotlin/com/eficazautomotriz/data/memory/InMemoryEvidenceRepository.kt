package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.EvidenceRepository
import com.eficazautomotriz.domain.model.Evidence
import kotlinx.coroutines.flow.Flow

class InMemoryEvidenceRepository : InMemoryRepository<Evidence>("EVD"), EvidenceRepository {

    override fun findByServiceOrder(serviceOrderId: String): List<Evidence> =
        findAll().filter { it.serviceOrderId == serviceOrderId }

    override fun observeByOrder(serviceOrderId: String): Flow<List<Evidence>> =
        snapshotWhere { it.serviceOrderId == serviceOrderId }
}
