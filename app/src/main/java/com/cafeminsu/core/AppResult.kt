package com.cafeminsu.core

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: DomainError) : AppResult<Nothing>
}

sealed interface DomainError {
    data object Network : DomainError
    data object Timeout : DomainError
    data object Unauthorized : DomainError
    data object NotFound : DomainError
    data class Payment(val reason: String) : DomainError
    data class Validation(val field: String) : DomainError
    data object Unknown : DomainError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DomainError) -> R,
): R =
    when (this) {
        is AppResult.Success -> onSuccess(data)
        is AppResult.Failure -> onFailure(error)
    }

fun <T> AppResult<T>.getOrNull(): T? =
    when (this) {
        is AppResult.Success -> data
        is AppResult.Failure -> null
    }

val AppResult<*>.isSuccess: Boolean
    get() = this is AppResult.Success

val AppResult<*>.isFailure: Boolean
    get() = this is AppResult.Failure
