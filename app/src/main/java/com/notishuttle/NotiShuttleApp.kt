package com.notishuttle

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class NotiShuttleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureStatusChannel(this)
    }
}

object NotificationHelper {
    const val CHANNEL_STATUS = "notishuttle_status"

    fun ensureStatusChannel(context: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.channel_status_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = context.getString(R.string.channel_status_desc)
            nm?.createNotificationChannel(channel)
        }
    }

    /** Posts a quiet "NotiShuttle is running" notification after boot so the user can confirm the relay is active. */
    fun postRunningStatus(context: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.app.ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureStatusChannel(context)
        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.running_title))
            .setContentText(context.getString(R.string.running_text))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        runCatching {
            androidx.core.app.NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }
}
