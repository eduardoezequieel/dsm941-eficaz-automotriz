package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.Repository
import com.eficazautomotriz.domain.model.Identifiable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Shared base for every repository: it stores entries in an in-memory map and
 * generates human-readable identifiers with a prefix (VEH-001, APT-014).
 *
 * [snapshots] mirrors the map after every [save] and [deleteById]. Subclasses
 * expose filtered [Flow]s from it so the demo mode updates open screens without Firebase.
 * Synchronous reads are unchanged.
 */
abstract class InMemoryRepository<T : Identifiable>(
    private val idPrefix: String,
) : Repository<T> {

    private val items = mutableMapOf<String, T>()
    private var sequence = 0

    /** Latest copy of [items]. Collectors always receive the current contents first. */
    private val snapshots = MutableStateFlow<List<T>>(emptyList())

    override fun findById(id: String): T? = items[id]

    /** Immutable copy: the internal collection is never exposed. */
    override fun findAll(): List<T> = items.values.toList()

    override fun save(entity: T): T {
        items[entity.id] = entity
        alignSequenceWith(entity.id)
        publish()
        return entity
    }

    override fun deleteById(id: String): Boolean {
        val removed = items.remove(id) != null
        if (removed) publish()
        return removed
    }

    /** Every stored entity, in the same order as [findAll]. */
    protected fun snapshotAll(): Flow<List<T>> = snapshots.map { it.toList() }.distinctUntilChanged()

    /** Subset of [snapshotAll]. A save that does not change the subset does not emit again. */
    protected fun snapshotWhere(predicate: (T) -> Boolean): Flow<List<T>> =
        snapshots.map { items -> items.filter(predicate) }.distinctUntilChanged()

    private fun publish() {
        snapshots.value = items.values.toList()
    }

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
