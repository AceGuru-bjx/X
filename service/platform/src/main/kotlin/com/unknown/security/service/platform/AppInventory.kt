package com.unknown.security.service.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.unknown.security.core.model.PackageSnapshot
import java.security.MessageDigest

/**
 * Baseline inventory: reads the full application list via PackageManager —
 * works on every tier without any special privilege.
 */
class AppInventory(
    private val context: Context,
) {
    private val packageManager: PackageManager get() = context.packageManager

    /** All launchable + installed non-system and system packages. */
    fun allPackages(includeSystem: Boolean = true): List<PackageSnapshot> {
        val flags = PackageManager.GET_PERMISSIONS
        val packages =
            if (includeSystem) {
                packageManager.getInstalledPackages(flags)
            } else {
                packageManager.getInstalledPackages(flags).filter { (it.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM == 0 }
            }
        return packages.mapNotNull { info -> snapshot(info.packageName, computeHash = false) }
    }

    /** Single package snapshot; optionally computing the APK SHA-256 (IO-heavy). */
    fun snapshot(
        packageName: String,
        computeHash: Boolean = false,
    ): PackageSnapshot? {
        val info =
            runCatching {
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }.getOrNull() ?: return null
        return buildSnapshot(info, computeHash)
    }

    fun appLabel(packageName: String): String =
        runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

    fun appIcon(packageName: String): Drawable? =
        runCatching {
            packageManager.getApplicationIcon(packageName)
        }.getOrNull()

    fun isInstalled(packageName: String): Boolean =
        runCatching {
            packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)

    fun hasAccessibilityService(packageName: String): Boolean =
        runCatching {
            val intent = Intent("android.accessibilityservice.AccessibilityService")
            intent.`package` = packageName
            packageManager.queryIntentServices(intent, 0).isNotEmpty()
        }.getOrDefault(false)

    private fun buildSnapshot(
        info: PackageInfo,
        computeHash: Boolean,
    ): PackageSnapshot? {
        val appInfo = info.applicationInfo ?: return null
        val permissions = info.requestedPermissions?.toList() ?: emptyList()
        return PackageSnapshot(
            packageName = info.packageName,
            label = runCatching { packageManager.getApplicationLabel(appInfo).toString() }.getOrDefault(info.packageName),
            versionName = info.versionName ?: "?",
            versionCode = info.longVersionCode,
            targetSdk = appInfo.targetSdkVersion,
            firstInstallMillis = info.firstInstallTime,
            lastUpdateMillis = info.lastUpdateTime,
            installer =
                runCatching {
                    packageManager.getInstallSourceInfo(info.packageName).installingPackageName
                }.getOrNull() ?: "unknown",
            permissions = permissions,
            isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
            apkPath = appInfo.sourceDir,
            apkSizeBytes = runCatching { java.io.File(appInfo.sourceDir).length() }.getOrDefault(0L),
            sha256 = if (computeHash) hashOf(appInfo.sourceDir) else null,
        )
    }

    private fun hashOf(path: String): String? =
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            java.io.File(path).inputStream().use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }.getOrNull()

    companion object {
        /** Packages that implement the system installer UI on common OEMs. */
        val INSTALLER_PACKAGES: Set<String> =
            setOf(
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.android.vending",
                "com.miui.packageinstaller",
                "com.samsung.android.packageinstaller",
                "com.huawei.android.packageinstaller",
                "com.oplus.packageinstaller",
                "com.vivo.packageinstaller",
            )
    }
}
