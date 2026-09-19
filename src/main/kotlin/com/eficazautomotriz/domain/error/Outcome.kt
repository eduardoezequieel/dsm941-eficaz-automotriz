package com.eficazautomotriz.domain.error

/**
 * Resultado de un caso de uso. Se eligio un sealed propio en vez de kotlin.Result
 * porque este ultimo exige un Throwable en la rama de falla, y los errores de negocio
 * de este sistema se devuelven como dato, no se lanzan.
 */
sealed interface Outcome<out T> {

    data class Success<out T>(val value: T) : Outcome<T>

    data class Failure(val error: DomainError) : Outcome<Nothing>

    companion object {
        fun <T> success(value: T): Outcome<T> = Success(value)
        fun failure(error: DomainError): Outcome<Nothing> = Failure(error)
    }
}

/** Encadena casos de uso sin anidar comprobaciones de exito en cada paso. */
inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

fun <T> Outcome<T>.errorOrNull(): DomainError? = when (this) {
    is Outcome.Success -> null
    is Outcome.Failure -> error
}
