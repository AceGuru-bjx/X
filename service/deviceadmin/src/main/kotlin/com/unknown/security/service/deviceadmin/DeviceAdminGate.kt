package com.unknown.security.service.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.unknown.security.core.common.AppError
import com.unknown.security.core.common.AppResult
import com.unknown.security.core.common.ErrorKind

enum class DeviceAdminState {
    NOT_PROVISIONED,
    ACTIVE_ADMIN,
    DEVICE_OWNER,
}

/**
 * Device-owner channel: enterprise-grade prevention without root —
 * prohibit installs, hide apps, block uninstalls.
 */
class DeviceAdminGate(
    private val context: Context,
) {
    private val dpm: DevicePolicyManager get() =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin: ComponentName get() =
        ComponentName(context, UnknownDeviceAdminReceiver::class.java)

    fun state(): DeviceAdminState =
        when {
            dpm.isDeviceOwnerApp(context.packageName) -> DeviceAdminState.DEVICE_OWNER
            dpm.isAdminActive(admin) -> DeviceAdminState.ACTIVE_ADMIN
            else -> DeviceAdminState.NOT_PROVISIONED
        }

    val isDeviceOwner: Boolean get() = state() == DeviceAdminState.DEVICE_OWNER

    val isAdminActive: Boolean get() = dpm.isAdminActive(admin)

    /** adb shell dpm set-device-owner com.unknown.security/.service.deviceadmin.UnknownDeviceAdminReceiver */
    val provisioningCommand: String
        get() =
            "adb shell dpm set-device-owner ${context.packageName}/" +
                "com.unknown.security.service.deviceadmin.UnknownDeviceAdminReceiver"

    /** BLOCK_INSTALL: forbid installing any further apps (device owner only). */
    fun setInstallBlocked(blocked: Boolean): AppResult<Unit> =
        runCatching {
            requireDeviceOwner()
            if (blocked) {
                dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
            } else {
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
            }
            AppResult.ok(Unit)
        }.getOrElse {
            AppResult.err(AppError(ErrorKind.DENIED, "设置安装阻断失败: ${it.message}", it))
        }

    fun isInstallBlocked(): Boolean =
        isDeviceOwner &&
            dpm.getUserRestrictions(admin)?.getBoolean(UserManager.DISALLOW_INSTALL_APPS) == true

    /** HIDE_APP: hides the package from the launcher & system (device owner / admin). */
    fun setHidden(
        packageName: String,
        hidden: Boolean,
    ): AppResult<Unit> =
        runCatching {
            requireAdmin()
            val changed = dpm.setApplicationHidden(admin, packageName, hidden)
            if (!changed) {
                AppResult.err(AppError(ErrorKind.UNSUPPORTED, "无法隐藏 $packageName（系统应用或设备策略限制）"))
            } else {
                AppResult.ok(Unit)
            }
        }.getOrElse {
            AppResult.err(AppError(ErrorKind.DENIED, "隐藏应用失败: ${it.message}", it))
        }

    fun isHidden(packageName: String): Boolean =
        runCatching {
            dpm.isApplicationHidden(admin, packageName)
        }.getOrDefault(false)

    /** Prevents the package from being uninstalled without admin action. */
    fun setUninstallBlocked(
        packageName: String,
        blocked: Boolean,
    ): AppResult<Unit> =
        runCatching {
            requireAdmin()
            dpm.setUninstallBlocked(admin, packageName, blocked)
            AppResult.ok(Unit)
        }.getOrElse {
            AppResult.err(AppError(ErrorKind.DENIED, "设置禁止卸载失败: ${it.message}", it))
        }

    /** Removes admin rights (fails silently when running as device owner on newer Androids). */
    fun deactivate(): AppResult<Unit> =
        runCatching {
            dpm.removeActiveAdmin(admin)
            AppResult.ok(Unit)
        }.getOrElse {
            AppResult.err(AppError(ErrorKind.DENIED, "移除设备管理器失败: ${it.message}", it))
        }

    private fun requireAdmin() {
        check(dpm.isAdminActive(admin)) { "设备管理器未激活" }
    }

    private fun requireDeviceOwner() {
        check(dpm.isDeviceOwnerApp(context.packageName)) { "需要设备所有者（Device Owner）权限" }
    }
}
