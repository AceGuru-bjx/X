package com.unknown.security.core.persistence

import android.content.Context
import com.unknown.security.core.model.WhitelistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Whitelist persistence — small JSON file, never acted on automatically. */
class WhitelistStore(
    private val context: Context,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val serializer = ListSerializer(WhitelistEntry.serializer())

    private val mutex = Mutex()

    private val _entries = MutableStateFlow<List<WhitelistEntry>>(emptyList())
    val entries: StateFlow<List<WhitelistEntry>> = _entries.asStateFlow()

    val packageNames: Set<String> get() = _entries.value.map { it.packageName }.toSet()

    suspend fun load() {
        mutex.withLock {
            withContext(Dispatchers.IO) { publishLocked() }
        }
    }

    suspend fun add(
        packageName: String,
        note: String = "",
    ) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readLocked().filterNot { it.packageName == packageName }
                val updated =
                    current +
                        WhitelistEntry(
                            packageName = packageName,
                            addedMillis = System.currentTimeMillis(),
                            note = note,
                        )
                writeLocked(updated)
                publishLocked()
            }
        }
    }

    suspend fun remove(packageName: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                writeLocked(readLocked().filterNot { it.packageName == packageName })
                publishLocked()
            }
        }
    }

    suspend fun isWhitelisted(packageName: String): Boolean = _entries.value.any { it.packageName == packageName }

    private fun publishLocked() {
        _entries.value = readLocked()
    }

    private fun readLocked(): List<WhitelistEntry> {
        val file = whitelistFile()
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString(serializer, file.readText()) }
            .getOrDefault(emptyList())
    }

    private fun writeLocked(entries: List<WhitelistEntry>) {
        whitelistFile().writeText(json.encodeToString(serializer, entries))
    }

    private fun whitelistFile() = context.filesDir.resolve(WHITELIST_FILE)

    private companion object {
        const val WHITELIST_FILE = "whitelist.json"
    }
}
