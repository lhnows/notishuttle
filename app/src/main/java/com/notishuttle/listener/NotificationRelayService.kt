package com.notishuttle.listener

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.notishuttle.BuildConfig
import com.notishuttle.config.AppSettings
import com.notishuttle.config.FilterMode
import com.notishuttle.model.AppInfo
import com.notishuttle.model.NotificationInfo
import com.notishuttle.model.NotificationPayload
import com.notishuttle.model.TimeInfo
import com.notishuttle.model.toJson
import com.notishuttle.util.DeviceInfoBuilder
import com.notishuttle.webhook.WebhookWorker
import java.util.concurrent.TimeUnit

/**
 * Core service. The system binds it automatically once the user grants notification
 * access, keeps it alive, and re-binds it after reboot — no manual start needed.
 */
class NotificationRelayService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val context = applicationContext
        val settings = AppSettings.get(context)

        if (!settings.enabled) return
        if (settings.webhookUrl.isBlank()) return
        if (sbn.packageName == BuildConfig.APPLICATION_ID) return // ignore our own notifications
        if (settings.ignoreOngoing && sbn.isOngoing) return
        if (settings.ignoreSilent && isSilent(sbn)) return
        if (!passesAppFilter(sbn.packageName, settings)) return

        val payload = buildPayload(context, sbn, settings) ?: return
        if (!matchesKeyword(settings, payload.notification)) return

        val json = payload.toJson()
        val data = workDataOf(
            WebhookWorker.KEY_URL to settings.webhookUrl,
            WebhookWorker.KEY_PAYLOAD to json,
            WebhookWorker.KEY_SECRET to settings.secret,
            WebhookWorker.KEY_HEADERS to settings.headersJson,
        )

        val request = OneTimeWorkRequestBuilder<WebhookWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(WebhookWorker.TAG)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(request)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to enqueue webhook delivery", t)
        }
    }

    private fun buildPayload(
        context: Context,
        sbn: StatusBarNotification,
        settings: AppSettings,
    ): NotificationPayload? {
        val n = sbn.notification ?: return null
        val extras = n.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val ticker = n.tickerText?.toString()

        if (title.isNullOrBlank() && text.isNullOrBlank() && bigText.isNullOrBlank() &&
            subText.isNullOrBlank() && ticker.isNullOrBlank()
        ) {
            return null // nothing meaningful to forward
        }

        val label = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()

        val actions = n.actions
            ?.mapNotNull { it.title?.toString() }
            ?.takeIf { it.isNotEmpty() }

        return NotificationPayload(
            id = sbn.id,
            key = sbn.key,
            tag = sbn.tag,
            app = AppInfo(
                packageName = sbn.packageName,
                label = label,
                category = n.category,
                channelId = n.channelId,
            ),
            notification = NotificationInfo(
                title = title?.truncate(MAX_FIELD),
                text = text?.truncate(MAX_FIELD),
                bigText = bigText?.truncate(MAX_FIELD),
                subText = subText?.truncate(MAX_FIELD),
                tickerText = ticker?.truncate(MAX_FIELD),
                isOngoing = sbn.isOngoing,
                isClearable = sbn.isClearable,
                groupKey = sbn.groupKey,
                actions = actions,
            ),
            time = TimeInfo(postTime = sbn.postTime, `when` = n.`when`),
            device = DeviceInfoBuilder.build(context, settings),
        )
    }

    private fun passesAppFilter(packageName: String, settings: AppSettings): Boolean {
        return when (settings.filterMode) {
            FilterMode.ALL -> true
            FilterMode.BLOCKLIST -> packageName !in settings.filteredApps
            FilterMode.ALLOWLIST -> packageName in settings.filteredApps
        }
    }

    private fun matchesKeyword(settings: AppSettings, info: NotificationInfo): Boolean {
        val keyword = settings.keyword.trim()
        if (keyword.isEmpty()) return true
        val haystack = buildString {
            info.title?.let { append(it).append(' ') }
            info.text?.let { append(it).append(' ') }
            info.bigText?.let { append(it) }
        }
        return haystack.contains(keyword, ignoreCase = true)
    }

    /** A notification is treated as "silent" when its channel has low-or-below importance (no sound). */
    private fun isSilent(sbn: StatusBarNotification): Boolean {
        val channelId = sbn.notification?.channelId ?: return false
        val nm = getSystemService(NotificationManager::class.java) ?: return false
        val channel = runCatching { nm.getNotificationChannel(channelId) }.getOrNull() ?: return false
        return channel.importance <= NotificationManager.IMPORTANCE_LOW
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected")
    }

    companion object {
        private const val TAG = "NotificationRelayService"
        private const val MAX_FIELD = 2000

        fun isAccessGranted(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)

        private fun String.truncate(max: Int): String =
            if (length <= max) this else take(max) + "…"
    }
}
