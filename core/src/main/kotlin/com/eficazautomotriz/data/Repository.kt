package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Identifiable

/**
 * Generic persistence contract. Stage 3 replaces the implementation with
 * Cloud Firestore without the use cases noticing.
 */
interface Repository<T : Identifiable> {
    fun findById(id: String): T?
    fun findAll(): List<T>
    fun save(entity: T): T
    fun deleteById(id: String): Boolean

    /** Next human-readable identifier for the collection (VEH-001, APT-014). */
    fun nextId(): String
}