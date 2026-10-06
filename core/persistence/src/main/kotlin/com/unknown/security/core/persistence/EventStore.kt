package com.unknown.security.core.persistence

import android.content.Context
import com.unknown.security.core.model.InterceptionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Append-only NDJSON event log with an in-memory ring buffer of the most
 * recent [MAX_EVENTS] events. Cheap to write, trivial to inspect.
 */
class EventStore(
    private val context: Context,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val mutex = Mutex()
    private var nextId = System.currentTimeMillis()

    private val _events = MutableStateFlow<List<InterceptionEvent>>(emptyList())
    val events: StateFlow<List<InterceptionEvent>> = _events.asStateFlow()

    suspend fun loadRecent() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val file = logFile()
                if (!file.exists()) {
                    _events.value = emptyList()
                    return@withContext
                }
                val parsed =
                    file
                        .readLines()
                        .filter { it.isNotBlank() }
                        .mapNotNull { line ->
                            runCatching { json.decodeFromString(InterceptionEvent.serializer(), line) }.getOrNull()
                        }
                _events.value = parsed.sortedByDescending { it.timestampMillis }
                nextId = (_events.value.maxOfOrNull { it.id } ?: System.currentTimeMillis()) + 1
            }
        }
    }

    suspend fun append(event: InterceptionEvent) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                logFile().appendText(json.encodeToString(InterceptionEvent.serializer(), event) + "\n")
                _events.value = (listOf(event) + _events.value).take(MAX_EVENTS)
                trimFile()
            }
        }
    }

    fun newId(): Long = nextId++

    suspend fun clear() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                logFile().delete()
                _events.value = emptyList()
            }
        }
    }

    private fun trimFile() {
        val file = logFile()
        val lines = file.readLines()
        if (lines.size > MAX_EVENTS) {
            val kept = lines.takeLast(MAX_EVENTS)
            file.writeText(kept.joinToString("\n", postfix = "\n"))
        }
    }

    private fun logFile() = context.filesDir.resolve(EVENT_FILE)

    private companion object {
        const val EVENT_FILE = "interception_events.ndjson"
        const val MAX_EVENTS = 500
    }
}
