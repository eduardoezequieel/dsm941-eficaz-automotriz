package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Evidence
import kotlinx.coroutines.flow.Flow

interface EvidenceRepository : Repository<Evidence> {
    fun findByServiceOrder(serviceOrderId: String): List<Evidence>

    /**
     * Live version of [findByServiceOrder]. The list rule allows a client query
     * filtered by serviceOrderId.
     */
    fun observeByOrder(serviceOrderId: String): Flow<List<Evidence>>
}
