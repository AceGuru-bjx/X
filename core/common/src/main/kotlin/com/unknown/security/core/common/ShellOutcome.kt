package com.unknown.security.core.common

/**
 * Outcome of executing a privileged shell command, shared by every tier that
 * can run one (adb-level via Shizuku, or root via su).
 */
data class ShellOutcome(
    val command: String,
    val exitCode: Int,
    val output: String,
    val durationMillis: Long,
    val via: Channel,
) {
    enum class Channel { ROOT_SU, SHIZUKU_SHELL, DEVICE_POLICY }

    val isSuccess: Boolean get() = exitCode == 0

    val trimmedOutput: String get() = output.trim()

    companion object {
        fun failed(
            command: String,
            via: Channel,
            message: String,
        ): ShellOutcome = ShellOutcome(command = command, exitCode = -1, output = message, durationMillis = 0L, via = via)
    }
}
