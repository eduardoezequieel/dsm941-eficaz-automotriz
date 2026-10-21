package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.EvidenceRepository
import com.eficazautomotriz.domain.model.Evidence

class InMemoryEvidenceRepository : InMemoryRepository<Evidence>("EVD"), EvidenceRepository {

    override fun findByServiceOrder(serviceOrderId: String): List<Evidence> =
        findAll().filter { it.serviceOrderId == serviceOrderId }
}
