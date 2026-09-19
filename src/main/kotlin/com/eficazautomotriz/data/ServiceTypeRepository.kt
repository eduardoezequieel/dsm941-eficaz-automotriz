package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.ServiceType

class ServiceTypeRepository : InMemoryRepository<ServiceType>("SVT") {

    fun findActive(): List<ServiceType> = findAll().filter { it.active }

    /** Cae al identificador cuando el tipo ya no existe: un listado no debe quedar en blanco. */
    fun nameOf(serviceTypeId: String): String = findById(serviceTypeId)?.name ?: serviceTypeId
}
