package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Evidence

interface EvidenceRepository : Repository<Evidence> {
    fun findByServiceOrder(serviceOrderId: String): List<Evidence>
}
