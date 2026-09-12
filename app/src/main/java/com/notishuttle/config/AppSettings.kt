package com.notishuttle.config

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

enum class FilterMode { ALL, ALLOWLIST, BLOCKLIST }

/** Thin wrapper over SharedPreferences holding all user configuration. */
class AppSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var webhookUrl: String
        get() = prefs.getString(KEY_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_URL, value.trim()).apply()

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Shared secret used for the HMAC-SHA256 request signature. Blank disables signing. */
    var secret: String
        get() = prefs.getString(KEY_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECRET, value).apply()

    /** Optional JSON object of extra request headers, e.g. {"Authorization":"Bearer xyz"}. */
    var headersJson: String
        get() = prefs.getString(KEY_HEADERS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HEADERS, value).apply()

    /** Forward only notifications whose title/text contain this keyword (case-insensitive). Blank = all. */
    var keyword: String
        get() = prefs.getString(KEY_KEYWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_KEYWORD, value).apply()

    var ignoreOngoing: Boolean
        get() = prefs.getBoolean(KEY_IGNORE_ONGOING, false)
        set(value) = prefs.edit().putBoolean(KEY_IGNORE_ONGOING, value).apply()

    var ignoreSilent: Boolean
        get() = prefs.getBoolean(KEY_IGNORE_SILENT, false)
        set(value) = prefs.edit().putBoolean(KEY_IGNORE_SILENT, value).apply()

    var filterMode: FilterMode
        get() = runCatching { FilterMode.valueOf(prefs.getString(KEY_FILTER_MODE, FilterMode.ALL.name)!!) }
            .getOrDefault(FilterMode.ALL)
        set(value) = prefs.edit().putString(KEY_FILTER_MODE, value.name).apply()

    /** Package names selected for allow-list / block-list mode. */
    var filteredApps: Set<String>
        get() = prefs.getStringSet(KEY_FILTERED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FILTERED_APPS, value.toSet()).apply()

    /** Stable anonymous identifier for this device, included in every payload. */
    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            return id
        }

    companion object {
        private const val PREFS_NAME = "notishuttle_prefs"
        private const val KEY_URL = "webhook_url"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SECRET = "secret"
        private const val KEY_HEADERS = "headers_json"
        private const val KEY_KEYWORD = "keyword"
        private const val KEY_IGNORE_ONGOING = "ignore_ongoing"
        private const val KEY_IGNORE_SILENT = "ignore_silent"
        private const val KEY_FILTER_MODE = "filter_mode"
        private const val KEY_FILTERED_APPS = "filtered_apps"
        private const val KEY_DEVICE_ID = "device_id"

        @Volatile
        private var instance: AppSettings? = null

        fun get(context: Context): AppSettings =
            instance ?: synchronized(this) {
                instance ?: AppSettings(context).also { instance = it }
            }
    }
}
