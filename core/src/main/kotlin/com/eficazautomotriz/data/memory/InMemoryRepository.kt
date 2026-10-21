package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.Repository
import com.eficazautomotriz.domain.model.Identifiable

/**
 * Shared base for every repository: it stores entries in an in-memory map and
 * generates human-readable identifiers with a prefix (VEH-001, APT-014).
 */
abstract class InMemoryRepository<T : Identifiable>(
    private val idPrefix: String,
) : Repository<T> {

    private val items = mutableMapOf<String, T>()
    private var sequence = 0

    override fun findById(id: String): T? = items[id]

    /** Immutable copy: the internal collection is never exposed. */
    override fun findAll(): List<T> = items.values.toList()

    override fun save(entity: T): T {
        items[entity.id] = entity
        alignSequenceWith(entity.id)
        return entity
    }

    override fun deleteById(id: String): Boolean = items.remove(id) != null

    /** Generates the next identifier in the repository sequence. */
    override fun nextId(): String {
        sequence += 1
        return format(sequence)
    }

    /** Keeps an identifier loaded from the seed data from being issued again later. */
    private fun alignSequenceWith(id: String) {
        val number = id.removePrefix("$idPrefix-").toIntOrNull() ?: return
        if (number > sequence) sequence = number
    }

    private fun format(number: Int): String = "$idPrefix-" + number.toString().padStart(ID_DIGITS, '0')

    private companion object {
        const val ID_DIGITS = 3
    }
}
