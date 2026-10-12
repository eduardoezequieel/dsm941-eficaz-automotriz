package com.eficazautomotriz.domain.error

/**
 * Result of a use case. A custom sealed type was chosen instead of kotlin.Result
 * because the latter requires a Throwable on the failure branch, and the business errors
 * of this system are returned as data, not thrown.
 */
sealed interface Outcome<out T> {

    data class Success<out T>(val value: T) : Outcome<T>

    data class Failure(val error: DomainError) : Outcome<Nothing>

    companion object {
        fun <T> success(value: T): Outcome<T> = Success(value)
        fun failure(error: DomainError): Outcome<Nothing> = Failure(error)
    }
}

/** Chains use cases without nesting a success check at every step. */
inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

fun <T> Outcome<T>.errorOrNull(): DomainError? = when (this) {
    is Outcome.Success -> null
    is Outcome.Failure -> error
}
