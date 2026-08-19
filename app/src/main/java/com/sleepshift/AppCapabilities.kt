package com.sleepshift

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.sleepshift.admin.DeviceOwner

/** 应用能力状态（首次启动引导展示用；以普通用户可理解的方式呈现） */
object AppCapabilities {

    data class Status(
        val deviceOwnerGranted: Boolean,
        val autoTimeZoneEnabled: Boolean,
        val exactAlarmGranted: Boolean,
    ) {
        val allReady: Boolean get() = deviceOwnerGranted && !autoTimeZoneEnabled && exactAlarmGranted
    }

    fun check(context: Context): Status {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return Status(
            deviceOwnerGranted = DeviceOwner.isDeviceOwner(context),
            autoTimeZoneEnabled =
                Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 1) != 0,
            exactAlarmGranted = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms(),
        )
    }
}
