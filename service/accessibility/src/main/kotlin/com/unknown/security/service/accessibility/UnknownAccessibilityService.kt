package com.unknown.security.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Real-time eyes of the guard:
 * - tracks foreground app via window state changes;
 * - detects installer confirmation dialogs and can auto-cancel them;
 * - observes volume-key sequences for the emergency escape gesture.
 */
class UnknownAccessibilityService : AccessibilityService() {
    private var lastForeground: String? = null
    private var dialogActive = false

    private var emergencyPresses = 0
    private var emergencyFirstPressMillis = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        AccessibilityBus.markConnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityBus.publish(AccessibilitySignal.ServiceStopped)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowState(event)
            else -> Unit
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || event.action != KeyEvent.ACTION_DOWN) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                observeEmergencyPress()
                // Never consume: volume control must keep working.
                false
            }

            else -> {
                false
            }
        }
    }

    private fun handleWindowState(event: AccessibilityEvent) {
        val source = event.packageName?.toString() ?: return
        val isInstaller = source in InstallerPackages

        if (isInstaller && event.className?.contains("Dialog", ignoreCase = true) == true) {
            if (!dialogActive) {
                dialogActive = true
                AccessibilityBus.publish(AccessibilitySignal.InstallerDialogShown(source))
                if (AccessibilityBus.dialogControlEnabled) {
                    maybeCancelDialog()
                }
            }
            return
        }

        if (source != lastForeground && source != this.packageName) {
            lastForeground = source
            if (source.isNotEmpty()) {
                AccessibilityBus.publish(AccessibilitySignal.ForegroundChanged(source))
            }
        }
        if (!isInstaller) dialogActive = false
    }

    /**
     * Attempts to click the negative/cancel button of the installer dialog.
     * Only an explicit "取消 / CANCEL / 安装" style negative button is pressed —
     * we never blindly press back, to avoid landing in "install anyway" states.
     */
    private fun maybeCancelDialog() {
        val root = rootInActiveWindow ?: return
        val cancel = findCancelButton(root)
        if (cancel != null && cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            AccessibilityBus.publish(AccessibilitySignal.DialogCancelled(root.packageName?.toString() ?: ""))
        }
    }

    private fun findCancelButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val texts =
            node.findAccessibilityNodeInfosByText("取消") +
                node.findAccessibilityNodeInfosByText("CANCEL") +
                node.findAccessibilityNodeInfosByText("Cancel") +
                node.findAccessibilityNodeInfosByText("取消安装")

        return texts.firstOrNull { candidate ->
            candidate.isClickable || candidate.parent?.isClickable == true
        } ?: candidateAncestorClickable(texts.firstOrNull())
    }

    private fun candidateAncestorClickable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        var depth = 0
        while (current != null && depth < MAX_CLIMB) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun observeEmergencyPress() {
        val now = SystemClock.elapsedRealtime()
        if (now - emergencyFirstPressMillis > AccessibilityBus.emergencyWindowMillis) {
            emergencyPresses = 0
            emergencyFirstPressMillis = now
        }
        emergencyPresses++
        if (emergencyPresses >= AccessibilityBus.emergencyPressesRequired) {
            emergencyPresses = 0
            AccessibilityBus.publish(AccessibilitySignal.EmergencyTriggered(AccessibilityBus.emergencyPressesRequired))
        }
    }

    private companion object {
        val InstallerPackages: Set<String> =
            setOf(
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.miui.packageinstaller",
                "com.samsung.android.packageinstaller",
                "com.huawei.android.packageinstaller",
                "com.oplus.packageinstaller",
                "com.vivo.packageinstaller",
            )

        private const val MAX_CLIMB = 6
    }
}
