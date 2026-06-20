package com.cafeminsu.data.remote

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

fun Throwable.toDomainError(): DomainError =
    when (this) {
        is SocketTimeoutException -> DomainError.Timeout
        is IOException -> DomainError.Network
        is HttpException -> code().toDomainError()
        else -> DomainError.Unknown
    }

suspend inline fun <T> runCatchingToAppResult(
    crossinline block: suspend () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) {
            throw throwable
        }
        AppResult.Failure(throwable.toDomainError())
    }

private fun Int.toDomainError(): DomainError =
    when (this) {
        401 -> DomainError.Unauthorized
        404 -> DomainError.NotFound
        else -> DomainError.Unknown
    }
