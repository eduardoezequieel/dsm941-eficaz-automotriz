package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.ServiceType

interface ServiceTypeRepository : Repository<ServiceType> {
    fun findActive(): List<ServiceType>

    /** Falls back to the identifier when the type no longer exists: a listing must not go blank. */
    fun nameOf(serviceTypeId: String): String
}
