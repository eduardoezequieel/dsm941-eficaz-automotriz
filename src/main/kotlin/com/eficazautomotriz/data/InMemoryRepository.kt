package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Identifiable

/**
 * Base compartida por todos los repositorios: almacena en un mapa en memoria y
 * genera identificadores legibles con prefijo (VEH-001, APT-014).
 */
abstract class InMemoryRepository<T : Identifiable>(
    private val idPrefix: String,
) : Repository<T> {

    private val items = mutableMapOf<String, T>()
    private var sequence = 0

    override fun findById(id: String): T? = items[id]

    /** Copia inmutable: nunca se expone la coleccion interna. */
    override fun findAll(): List<T> = items.values.toList()

    override fun save(entity: T): T {
        items[entity.id] = entity
        alignSequenceWith(entity.id)
        return entity
    }

    override fun deleteById(id: String): Boolean = items.remove(id) != null

    /** Genera el siguiente identificador de la secuencia del repositorio. */
    fun nextId(): String {
        sequence += 1
        return format(sequence)
    }

    /** Evita que un identificador cargado desde los datos semilla se repita despues. */
    private fun alignSequenceWith(id: String) {
        val number = id.removePrefix("$idPrefix-").toIntOrNull() ?: return
        if (number > sequence) sequence = number
    }

    private fun format(number: Int): String = "$idPrefix-" + number.toString().padStart(ID_DIGITS, '0')

    private companion object {
        const val ID_DIGITS = 3
    }
}
