package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.data.Repository
import com.eficazautomotriz.domain.model.Identifiable
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import java.util.concurrent.ExecutionException

/**
 * Base of the Firestore repositories. It BLOCKS on purpose: the core use cases are
 * synchronous, so every method waits for its task with `Tasks.await`. It is always called
 * from `Dispatchers.IO`; if someone calls it on the main thread, `Tasks.await` throws
 * `IllegalStateException`, which works as an early alarm.
 *
 * The document id is `entity.id`, also stored as the `id` field. Ids come from the
 * `contadores/{collection}` counter (see [SupportDocuments]), never from reading the whole
 * collection: the rules only let a client read their own documents.
 */
abstract class FirestoreRepository<T : Identifiable>(
    protected val db: FirebaseFirestore,
    private val collection: String,
    private val idPrefix: String,
    protected val toMap: (T) -> Map<String, Any?>,
    protected val fromDoc: (DocumentSnapshot) -> T,
) : Repository<T> {

    protected val col: CollectionReference get() = db.collection(collection)

    private val counter: DocumentReference get() = db.collection(SupportDocuments.COUNTERS).document(collection)

    /** Last counter value seen by this instance; -1 = not read yet. Only avoids needless reads. */
    private var knownCounter: Long = -1

    override fun findById(id: String): T? =
        Tasks.await(col.document(id).get()).takeIf { it.exists() }?.let(fromDoc)

    /** Collection-wide read: the rules allow it only for staff (and for the catalogs). */
    override fun findAll(): List<T> = Tasks.await(col.get()).documents.map(fromDoc)

    override fun save(entity: T): T {
        persist(entity)
        align(entity.id)
        return entity
    }

    /** Writes the document; subclasses override it to update support documents atomically. */
    protected open fun persist(entity: T) {
        Tasks.await(col.document(entity.id).set(toMap(entity)))
    }

    override fun deleteById(id: String): Boolean {
        val exists = findById(id) != null
        if (exists) Tasks.await(col.document(id).delete())
        return exists
    }

    /**
     * Increments the counter by exactly 1 in a transaction (what the rules allow a client).
     * An id already taken (data written before the counter existed, and no staff backfill
     * yet) is skipped: the probe reads null for a free id, and a document of someone else
     * comes back as PERMISSION_DENIED, which also means "taken".
     */
    @Synchronized
    override fun nextId(): String {
        repeat(MAX_ID_ATTEMPTS) {
            val number = await(
                db.runTransaction { tx ->
                    val next = (tx.get(counter).getLong(SupportDocuments.COUNTER_VALUE) ?: 0L) + 1
                    tx.set(counter, mapOf(SupportDocuments.COUNTER_VALUE to next))
                    next
                }
            )
            knownCounter = number
            val id = SupportDocuments.formatId(idPrefix, number)
            if (isFree(id)) return id
        }
        throw IllegalStateException("No se encontro un identificador libre para $collection")
    }

    /** Staff backfill: brings the counter up to the highest id already stored. Throws on failure. */
    fun backfillCounter() {
        val highest = findAll().maxOfOrNull { SupportDocuments.idNumber(it.id, idPrefix) } ?: 0
        if (highest > 0) raiseCounterTo(highest.toLong())
    }

    /** Chained equality (`whereEqualTo`) over each field → value pair. */
    protected fun query(vararg filters: Pair<String, Any>): List<T> =
        run(filters.fold(col as Query) { q, (field, value) -> q.whereEqualTo(field, value) })

    protected fun run(query: Query): List<T> = Tasks.await(query.get()).documents.map(fromDoc)

    /** `Tasks.await` that rethrows the Firestore cause instead of the `ExecutionException` wrapper. */
    protected fun <R> await(task: Task<R>): R = try {
        Tasks.await(task)
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }

    /**
     * After a save, raises the counter when the id was written directly (seed data or a
     * restore). Only staff may jump more than 1, so this is best-effort: a refusal is ignored
     * (the document itself is already saved) and not retried on every save; nextId still
     * skips ids that are taken.
     */
    private fun align(id: String) {
        val number = SupportDocuments.idNumber(id, idPrefix).toLong()
        if (number <= knownCounter) return
        runCatching { raiseCounterTo(number) }.onFailure { knownCounter = maxOf(knownCounter, number) }
    }

    /** Raises the counter to [number] when it is behind; never lowers it. */
    @Synchronized
    private fun raiseCounterTo(number: Long) {
        if (number <= knownCounter) return
        knownCounter = await(
            db.runTransaction { tx ->
                val current = tx.get(counter).getLong(SupportDocuments.COUNTER_VALUE) ?: 0L
                if (current < number) tx.set(counter, mapOf(SupportDocuments.COUNTER_VALUE to number))
                maxOf(current, number)
            }
        )
    }

    private fun isFree(id: String): Boolean = try {
        !await(col.document(id).get()).exists()
    } catch (e: FirebaseFirestoreException) {
        if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        false
    }

    private companion object {
        const val MAX_ID_ATTEMPTS = 50
    }
}