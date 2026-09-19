package com.eficazautomotriz.domain.error

sealed class Outcome<out T> {
    data class Success<out T>(
        val value: T
    ) : Outcome<T>()

    data class Failure(
        val error: DomainError
    ) : Outcome<Nothing>()

    fun <R> map(transform: (T) -> R): Outcome<R> {
        return when (this) {
            is Success -> Success(transform(value))
            is Failure -> this
        }
    }

    fun getOrNull(): T? {
        return when (this) {
            is Success -> value
            is Failure -> null
        }
    }

    fun errorOrNull(): DomainError? {
        return when (this) {
            is Success -> null
            is Failure -> error
        }
    }
}
