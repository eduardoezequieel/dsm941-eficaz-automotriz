package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Identifiable

/**
 * Contrato de persistencia generico. La Etapa 3 sustituye la implementacion por
 * Cloud Firestore sin que los casos de uso se enteren.
 */
interface Repository<T : Identifiable> {
    fun findById(id: String): T?
    fun findAll(): List<T>
    fun save(entity: T): T
    fun deleteById(id: String): Boolean
}
