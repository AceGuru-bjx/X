package com.unknown.security.core.common

/**
 * Lightweight result type used across module boundaries so that failures stay
 * explicit instead of being hidden inside exceptions.
 */
sealed interface AppResult<out T> {
    data class Ok<T>(
        val value: T,
    ) : AppResult<T>

    data class Err(
        val error: AppError,
    ) : AppResult<Nothing>

    companion object {
        fun <T> ok(value: T): AppResult<T> = Ok(value)

        fun err(error: AppError): AppResult<Nothing> = Err(error)
    }
}

/** Describes a failure with a stable machine-readable kind. */
data class AppError(
    val kind: ErrorKind,
    val message: String,
    val cause: Throwable? = null,
) {
    enum class Kind { UNAVAILABLE, DENIED, TIMEOUT, UNSUPPORTED, IO, UNKNOWN }
}

typealias ErrorKind = AppError.Kind

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Ok -> AppResult.Ok(transform(value))
        is AppResult.Err -> this
    }

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Ok) block(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Err) block(error)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Ok)?.value
