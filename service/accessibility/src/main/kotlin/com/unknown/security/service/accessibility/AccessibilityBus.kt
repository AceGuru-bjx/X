package com.unknown.security.service.accessibility

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AccessibilitySignal {
    /** Foreground package changed — fired on TYPE_WINDOW_STATE_CHANGED. */
    data class ForegroundChanged(
        val packageName: String,
    ) : AccessibilitySignal()

    /** An installer confirmation dialog came to the front. */
    data class InstallerDialogShown(
        val installerPackage: String,
    ) : AccessibilitySignal()

    /** A dialog was auto-cancelled by the guard. */
    data class DialogCancelled(
        val installerPackage: String,
    ) : AccessibilitySignal()

    /** Volume keys pressed [presses] times within the emergency window. */
    data class EmergencyTriggered(
        val presses: Int,
    ) : AccessibilitySignal()

    /** The service died or was disabled. */
    data object ServiceStopped : AccessibilitySignal()
}

/**
 * Process-wide bus between the accessibility service and the repository layer.
 * Services cannot receive constructor injection, so they publish through this
 * singleton; the app container subscribes exactly once.
 */
object AccessibilityBus {
    private val _signals =
        MutableSharedFlow<AccessibilitySignal>(
            replay = 32,
            extraBufferCapacity = 128,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val signals: SharedFlow<AccessibilitySignal> = _signals.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _foreground = MutableStateFlow<String?>(null)
    val foreground: StateFlow<String?> = _foreground.asStateFlow()

    /** Set by the repository when policy toggles dialog auto-cancel. */
    @Volatile
    var dialogControlEnabled: Boolean = true

    @Volatile
    var emergencyPressesRequired: Int = 3

    @Volatile
    var emergencyWindowMillis: Long = 1200L

    fun publish(signal: AccessibilitySignal) {
        _signals.tryEmit(signal)
        when (signal) {
            is AccessibilitySignal.ForegroundChanged -> _foreground.value = signal.packageName
            is AccessibilitySignal.ServiceStopped -> _connected.value = false
            else -> Unit
        }
    }

    fun markConnected() {
        _connected.value = true
    }
}
