package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Base of the ViewModels. The use cases are synchronous and the Firestore repositories
 * block, so every block runs on `graph.io` (`Dispatchers.IO`, or one serialized thread in the
 * in-memory demo) and the result returns to the main
 * thread. An exception (no network, rules, corrupt document) does not crash the app: it
 * is logged and arrives as `Outcome.Failure` with the [SYSTEM_ENTITY] entity.
 */
abstract class EficazViewModel(protected val graph: AppGraph) : ViewModel() {

    protected fun <T> run(onResult: (Outcome<T>) -> Unit, block: () -> Outcome<T>): Job =
        viewModelScope.launch {
            val result = withContext(graph.io) {
                runCatching(block).getOrElse { failure ->
                    graph.errorLogger.error(
                        this@EficazViewModel::class.simpleName ?: "ViewModel",
                        failure::class.simpleName ?: "Exception",
                        failure.message.orEmpty(),
                    )
                    Outcome.failure(DomainError.NotFound(SYSTEM_ENTITY, failure.message ?: "sin detalle"))
                }
            }
            onResult(result)
        }

    protected fun today(): LocalDate = LocalDate.now()
    protected fun now(): LocalDateTime = LocalDateTime.now()

    companion object {
        /** Marks errors that are not business errors but system ones (network, permissions, data). */
        const val SYSTEM_ENTITY = "sistema"
    }
}

/** Text for the user: business errors as they are; system errors with context. */
fun DomainError.userMessage(): String =
    if (this is DomainError.NotFound && entity == EficazViewModel.SYSTEM_ENTITY) {
        "No se pudo completar la operación. Revise su conexión e intente de nuevo. ($id)"
    } else {
        message
    }
