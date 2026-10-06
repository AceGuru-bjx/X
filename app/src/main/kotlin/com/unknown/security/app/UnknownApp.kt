package com.unknown.security.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.unknown.security.R
import com.unknown.security.app.di.AppContainer

class UnknownApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val guardChannel =
            NotificationChannel(
                CHANNEL_GUARD,
                getString(R.string.channel_guard_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = getString(R.string.channel_guard_desc)
                setShowBadge(false)
            }
        val alertChannel =
            NotificationChannel(
                CHANNEL_ALERT,
                getString(R.string.channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.channel_alert_desc)
            }
        manager.createNotificationChannel(guardChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val CHANNEL_GUARD = "guard_status"
        const val CHANNEL_ALERT = "threat_alerts"

        fun container(context: Context): AppContainer = (context.applicationContext as UnknownApp).container
    }
}
