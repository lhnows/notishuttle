package com.notishuttle.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * JSON payload sent to the webhook.
 *
 * Example:
 * {
 *   "version": 1,
 *   "event": "notification",
 *   "id": 42,
 *   "key": "0|com.example|42|null|1000",
 *   "tag": null,
 *   "app": { "packageName": "...", "label": "...", "category": "...", "channelId": "..." },
 *   "notification": { "title": "...", "text": "...", ... },
 *   "time": { "postTime": 1700000000000, "when": 1700000000000 },
 *   "device": { "id": "...", "name": "...", "model": "...", "manufacturer": "...", "androidVersion": 35 }
 * }
 */
data class NotificationPayload(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("event") val event: String = "notification",
    @SerializedName("id") val id: Int,
    @SerializedName("key") val key: String?,
    @SerializedName("tag") val tag: String?,
    @SerializedName("app") val app: AppInfo,
    @SerializedName("notification") val notification: NotificationInfo,
    @SerializedName("time") val time: TimeInfo,
    @SerializedName("device") val device: DeviceInfo,
)

data class AppInfo(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("label") val label: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("channelId") val channelId: String?,
)

data class NotificationInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("bigText") val bigText: String?,
    @SerializedName("subText") val subText: String?,
    @SerializedName("tickerText") val tickerText: String?,
    @SerializedName("isOngoing") val isOngoing: Boolean,
    @SerializedName("isClearable") val isClearable: Boolean,
    @SerializedName("groupKey") val groupKey: String?,
    @SerializedName("actions") val actions: List<String>?,
)

data class TimeInfo(
    @SerializedName("postTime") val postTime: Long,
    @SerializedName("when") val `when`: Long,
)

data class DeviceInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("androidVersion") val androidVersion: Int,
)

fun NotificationPayload.toJson(): String = Gson().toJson(this)
