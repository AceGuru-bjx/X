package com.unknown.security.service.shizuku

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loaded into the Shizuku server process via UserService — every call runs
 * with adb-level privileges (shell uid 2000): force-stop, silent uninstall,
 * disable, hide and friends.
 */
class ShizukuShellService : IUnknownShellService.Stub() {
    override fun exec(command: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val output = StringBuilder()
        Thread { readStream(process.inputStream, output) }.start()
        Thread { readStream(process.errorStream, output) }.start()
        val exitCode = runCatching { process.waitFor() }.getOrDefault(-1)
        return buildString {
            append(exitCode)
            append('\n')
            append(output.toString().trim())
        }
    }

    private fun readStream(
        stream: java.io.InputStream,
        sink: StringBuilder,
    ) {
        runCatching {
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    synchronized(sink) { sink.appendLine(line) }
                    line = reader.readLine()
                }
            }
        }
    }
}
