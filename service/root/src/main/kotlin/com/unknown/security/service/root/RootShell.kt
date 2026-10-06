package com.unknown.security.service.root

import com.unknown.security.core.common.AppError
import com.unknown.security.core.common.AppResult
import com.unknown.security.core.common.ErrorKind
import com.unknown.security.core.common.ShellOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

enum class RootState {
    UNCHECKED,
    UNAVAILABLE,
    AVAILABLE,
}

/**
 * Root channel: executes commands via `su -c`. Deliberately dependency-free —
 * a plain process pipeline with output capture and timeouts.
 */
class RootShell {
    private val _state = MutableStateFlow(RootState.UNCHECKED)
    val state: StateFlow<RootState> = _state.asStateFlow()

    /** Runs `su -c id` to probe availability; caches the result. */
    suspend fun checkAvailability(): Boolean =
        withContext(Dispatchers.IO) {
            if (_state.value != RootState.UNCHECKED) return@withContext _state.value == RootState.AVAILABLE
            val available = probe()
            _state.value = if (available) RootState.AVAILABLE else RootState.UNAVAILABLE
            available
        }

    fun resetProbe() {
        _state.value = RootState.UNCHECKED
    }

    private fun probe(): Boolean =
        runCatching {
            val process = ProcessBuilder("su", "-c", "id").start()
            val output = process.inputStream.bufferedReader().readText()
            val finished = process.waitFor() == 0
            finished && output.contains("uid=0")
        }.getOrDefault(false)

    /** Executes one command as root. */
    suspend fun exec(command: String): AppResult<ShellOutcome> =
        withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            runCatching {
                withTimeout(COMMAND_TIMEOUT_MILLIS) {
                    val process = ProcessBuilder("su", "-c", command).start()
                    val stdout = StringBuilder()
                    val stderr = StringBuilder()
                    Thread {
                        process.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { synchronized(stdout) { stdout.appendLine(it) } }
                        }
                    }.start()
                    Thread {
                        process.errorStream.bufferedReader().useLines { lines ->
                            lines.forEach { synchronized(stderr) { stderr.appendLine(it) } }
                        }
                    }.start()
                    val exit = process.waitFor()
                    val output =
                        buildString {
                            append(stdout.toString().trim())
                            if (stderr.isNotBlank()) {
                                if (isNotEmpty()) append('\n')
                                append(stderr.toString().trim())
                            }
                        }
                    ShellOutcome(
                        command = command,
                        exitCode = exit,
                        output = output,
                        durationMillis = System.currentTimeMillis() - started,
                        via = ShellOutcome.Channel.ROOT_SU,
                    )
                }
            }.fold(
                onSuccess = { result ->
                    if (result.exitCode == 0 || result.output.isNotBlank()) {
                        AppResult.ok(result)
                    } else {
                        _state.value = RootState.UNAVAILABLE
                        AppResult.err(AppError(ErrorKind.DENIED, "su 拒绝执行"))
                    }
                },
                onFailure = {
                    _state.value = RootState.UNAVAILABLE
                    AppResult.err(AppError(ErrorKind.UNAVAILABLE, "root 不可用: ${it.message}", it))
                },
            )
        }

    companion object {
        private const val COMMAND_TIMEOUT_MILLIS = 25_000L
    }
}
