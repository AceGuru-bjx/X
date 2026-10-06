package com.unknown.security.app.di

import android.content.Context
import com.unknown.security.core.common.DefaultDispatcherProvider
import com.unknown.security.core.common.DispatcherProvider
import com.unknown.security.core.persistence.EventStore
import com.unknown.security.core.persistence.RuleStore
import com.unknown.security.core.persistence.SettingsStore
import com.unknown.security.core.persistence.WhitelistStore
import com.unknown.security.data.repository.GuardRepository
import com.unknown.security.service.deviceadmin.DeviceAdminGate
import com.unknown.security.service.platform.AppInventory
import com.unknown.security.service.root.RootShell
import com.unknown.security.service.shizuku.ShizukuGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand-rolled dependency container — one instance per process, owned by the
 * Application. Deliberately no DI framework: the graph is small, explicit and
 * debuggable.
 */
class AppContainer(
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    val dispatchers: DispatcherProvider = DefaultDispatcherProvider()

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: SettingsStore = SettingsStore(appContext)
    val rules: RuleStore = RuleStore(appContext)
    val events: EventStore = EventStore(appContext)
    val whitelist: WhitelistStore = WhitelistStore(appContext)

    val inventory: AppInventory = AppInventory(appContext)
    val shizukuGate: ShizukuGate = ShizukuGate(appContext)
    val rootShell: RootShell = RootShell()
    val deviceAdminGate: DeviceAdminGate = DeviceAdminGate(appContext)

    val repository: GuardRepository by lazy {
        GuardRepository(
            context = appContext,
            scope = applicationScope,
            settings = settings,
            rules = rules,
            events = events,
            whitelist = whitelist,
            inventory = inventory,
            shizukuGate = shizukuGate,
            rootShell = rootShell,
            deviceAdminGate = deviceAdminGate,
        )
    }
}
