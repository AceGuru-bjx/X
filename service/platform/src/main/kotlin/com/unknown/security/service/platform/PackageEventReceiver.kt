package com.unknown.security.service.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class PackageChangeKind { ADDED, REPLACED, REMOVED }

data class PackageChangeEvent(
    val packageName: String,
    val kind: PackageChangeKind,
    val replacing: Boolean,
)

/**
 * Install/replace/remove listener. Registered at runtime by the guard service
 * (RECEIVER_NOT_EXPORTED cannot be used for system broadcasts; these are
 * protected system broadcasts so exported registration is safe).
 */
class PackageEventReceiver : BroadcastReceiver() {
    private val _events =
        MutableSharedFlow<PackageChangeEvent>(
            replay = 16,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<PackageChangeEvent> = _events.asSharedFlow()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val event =
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (replacing) null else PackageChangeEvent(packageName, PackageChangeKind.ADDED, replacing)
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    PackageChangeEvent(packageName, PackageChangeKind.REPLACED, replacing = true)
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (replacing) null else PackageChangeEvent(packageName, PackageChangeKind.REMOVED, replacing)
                }

                else -> {
                    null
                }
            }
        if (event != null) {
            _events.tryEmit(event)
        }
    }

    fun register(context: Context) {
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        runCatching { context.unregisterReceiver(this) }
    }
}
