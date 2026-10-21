package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.domain.model.ServiceType

class InMemoryServiceTypeRepository : InMemoryRepository<ServiceType>("SVT"), ServiceTypeRepository {

    override fun findActive(): List<ServiceType> = findAll().filter { it.active }

    override fun nameOf(serviceTypeId: String): String = findById(serviceTypeId)?.name ?: serviceTypeId
}