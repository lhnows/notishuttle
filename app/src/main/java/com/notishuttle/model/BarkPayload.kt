package com.notishuttle.model

import android.net.Uri
import com.google.gson.Gson

/** Builds Bark-compatible requests (https://github.com/Finb/Bark). */
object BarkPayload {

    /** Build a Bark JSON body from a captured notification. */
    fun toJson(payload: NotificationPayload): String {
        val appLabel = payload.app.label?.takeIf { it.isNotBlank() } ?: payload.app.packageName
        val title = payload.notification.title?.takeIf { it.isNotBlank() }
        val content = listOf(
            payload.notification.text,
            payload.notification.bigText,
            payload.notification.subText,
            payload.notification.tickerText,
        ).firstOrNull { !it.isNullOrBlank() } ?: ""

        val map = linkedMapOf<String, String>()
        map["title"] = title ?: appLabel
        if (title != null) map["subtitle"] = appLabel
        if (content.isNotEmpty()) map["body"] = content
        map["group"] = appLabel
        map["level"] = "active"
        return Gson().toJson(map)
    }

    fun testJson(): String = Gson().toJson(
        linkedMapOf(
            "title" to "NotiShuttle 测试",
            "body" to "如果你收到这条，说明 Android 通知转发成功 ✅",
            "group" to "NotiShuttle",
            "level" to "active",
        )
    )

    /**
     * Normalize a user-pasted Bark URL into its POST endpoint.
     * Accepts "https://api.day.app/{key}" or "https://api.day.app/{key}/anything/else"
     * and keeps only scheme://authority/{key}.
     */
    fun normalizeEndpoint(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return trimmed
        val uri = Uri.parse(trimmed)
        val segments = uri.pathSegments
        if (segments.isEmpty()) return trimmed
        val scheme = uri.scheme ?: return trimmed
        val authority = uri.authority ?: return trimmed
        return "$scheme://$authority/${segments.first()}"
    }
}
