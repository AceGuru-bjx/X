package com.unknown.security.core.common

/** Minimal logging facade; Android binds it in the app shell. */
interface Logger {
    fun d(
        tag: String,
        message: String,
    )

    fun i(
        tag: String,
        message: String,
    )

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

/** Logger that drops everything; useful for previews and tests. */
object NoopLogger : Logger {
    override fun d(
        tag: String,
        message: String,
    ) = Unit

    override fun i(
        tag: String,
        message: String,
    ) = Unit

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}
