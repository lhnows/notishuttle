package com.notishuttle.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.notishuttle.NotificationHelper
import com.notishuttle.config.AppSettings
import com.notishuttle.listener.NotificationRelayService

/**
 * Fires after the device boots. Notification capture itself resumes automatically
 * (the system re-binds the listener service) and WorkManager re-schedules pending
 * deliveries; this receiver only surfaces a visible "running" confirmation.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT) return

        val settings = AppSettings.get(context)
        if (settings.enabled && NotificationRelayService.isAccessGranted(context)) {
            Log.i(TAG, "Boot completed; relay is active")
            NotificationHelper.postRunningStatus(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
    }
}
