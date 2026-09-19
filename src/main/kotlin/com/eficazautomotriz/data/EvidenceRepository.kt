package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Evidence

class EvidenceRepository : InMemoryRepository<Evidence>("EVD") {

    fun findByServiceOrder(serviceOrderId: String): List<Evidence> =
        findAll().filter { it.serviceOrderId == serviceOrderId }
}
