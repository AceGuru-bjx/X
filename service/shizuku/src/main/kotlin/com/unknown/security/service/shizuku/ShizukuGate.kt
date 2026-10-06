package com.unknown.security.service.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.unknown.security.core.common.AppError
import com.unknown.security.core.common.AppResult
import com.unknown.security.core.common.ErrorKind
import com.unknown.security.core.common.ShellOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/** Lifecycle states of the Shizuku channel. */
enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING,
    UNAUTHORIZED,
    ASKING,
    READY,
}

/**
 * Client-side bridge: owns the permission flow and the UserService binding
 * through which privileged shell commands are executed as uid 2000.
 */
class ShizukuGate(
    private val context: Context,
) : ServiceConnection {
    private val _state = MutableStateFlow(ShizukuState.NOT_INSTALLED)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val pendingPermissionRequests = ConcurrentHashMap<Int, CompletableDeferred<Boolean>>()

    @Volatile
    private var shellService: IUnknownShellService? = null

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            refreshState()
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {
            shellService = null
            refreshState()
        }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, result ->
            pendingPermissionRequests.remove(requestCode)?.complete(result == PackageManager.PERMISSION_GRANTED)
        }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refreshState()
    }

    fun refreshState() {
        val current = _state.value
        _state.value =
            when {
                !isShizukuInstalled() -> {
                    ShizukuState.NOT_INSTALLED
                }

                !Shizuku.pingBinder() -> {
                    ShizukuState.NOT_RUNNING
                }

                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                    ensureShellBound()
                    ShizukuState.READY
                }

                current == ShizukuState.ASKING -> {
                    ShizukuState.ASKING
                }

                else -> {
                    ShizukuState.UNAUTHORIZED
                }
            }
    }

    /** Requests permission (must be triggered from an interactive context). */
    suspend fun requestPermission(requestCode: Int = REQUEST_CODE): Boolean {
        if (!Shizuku.pingBinder()) return false
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true
        val deferred = CompletableDeferred<Boolean>()
        pendingPermissionRequests[requestCode] = deferred
        Shizuku.requestPermission(requestCode)
        _state.value = ShizukuState.ASKING
        return runCatching { withTimeout(PERMISSION_TIMEOUT_MILLIS) { deferred.await() } }
            .getOrDefault(false)
            .also { refreshState() }
    }

    /** Executes one shell command as the shell user; parses exit code + output. */
    suspend fun exec(command: String): AppResult<ShellOutcome> =
        withContext(Dispatchers.IO) {
            when {
                !isShizukuInstalled() -> {
                    AppResult.err(
                        AppError(ErrorKind.UNAVAILABLE, "Shizuku 未安装"),
                    )
                }

                !Shizuku.pingBinder() -> {
                    AppResult.err(
                        AppError(ErrorKind.UNAVAILABLE, "Shizuku 未启动"),
                    )
                }

                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> {
                    AppResult.err(
                        AppError(ErrorKind.DENIED, "Shizuku 未授权"),
                    )
                }

                else -> {
                    val started = System.currentTimeMillis()
                    val service =
                        obtainShellService()
                            ?: return@withContext AppResult.err(AppError(ErrorKind.UNAVAILABLE, "Shizuku shell 服务不可用"))
                    runCatching {
                        withTimeout(COMMAND_TIMEOUT_MILLIS) {
                            val raw = service.exec(command)
                            val firstLineBreak = raw.indexOf('\n')
                            val exit = raw.substring(0, firstLineBreak).trim().toIntOrNull() ?: -1
                            val output = if (firstLineBreak >= 0) raw.substring(firstLineBreak + 1) else ""
                            ShellOutcome(
                                command = command,
                                exitCode = exit,
                                output = output,
                                durationMillis = System.currentTimeMillis() - started,
                                via = ShellOutcome.Channel.SHIZUKU_SHELL,
                            )
                        }
                    }.fold(
                        onSuccess = { AppResult.ok(it) },
                        onFailure = {
                            shellService = null
                            AppResult.err(AppError(ErrorKind.UNKNOWN, "Shizuku 命令执行失败: ${it.message}", it))
                        },
                    )
                }
            }
        }

    private fun ensureShellBound() {
        if (shellService != null) return
        runCatching {
            Shizuku.bindUserService(userServiceArgs(), this)
        }
    }

    private suspend fun obtainShellService(): IUnknownShellService? {
        shellService?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            runCatching {
                Shizuku.bindUserService(
                    userServiceArgs(),
                    object : ServiceConnection {
                        override fun onServiceConnected(
                            name: ComponentName?,
                            binder: IBinder?,
                        ) {
                            val service = IUnknownShellService.Stub.asInterface(binder)
                            shellService = service
                            continuation.resume(service)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            shellService = null
                        }
                    },
                )
            }.onFailure {
                continuation.resume(null)
            }
        }
    }

    override fun onServiceConnected(
        name: ComponentName?,
        binder: IBinder?,
    ) {
        shellService = IUnknownShellService.Stub.asInterface(binder)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        shellService = null
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku
            .UserServiceArgs(
                ComponentName(context, ShizukuShellService::class.java),
            ).daemon(false)
            .processNameSuffix("shell")
            .version(SERVICE_VERSION)

    private fun isShizukuInstalled(): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        }.getOrDefault(false)

    fun dispose() {
        runCatching { Shizuku.unbindUserService(userServiceArgs(), this, true) }
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE = 47001
        private const val SERVICE_VERSION = 1
        private const val PERMISSION_TIMEOUT_MILLIS = 60_000L
        private const val COMMAND_TIMEOUT_MILLIS = 25_000L
    }
}
