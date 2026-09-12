package com.notishuttle.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.notishuttle.config.AppSettings
import com.notishuttle.model.DeviceInfo

object DeviceInfoBuilder {

    fun build(context: Context, settings: AppSettings): DeviceInfo {
        val deviceName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()

        return DeviceInfo(
            id = settings.deviceId,
            name = deviceName ?: Build.MODEL,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.SDK_INT,
        )
    }
}
