package com.sleepshift

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.sleepshift.admin.DeviceOwner
import com.sleepshift.permission.ShizukuManager
import com.sleepshift.permission.ShizukuPermission

/**
 * 应用能力状态（首次启动引导展示用；以普通用户可理解的方式呈现）。
 *
 * Phase 11-D：时区控制通道从「仅 Device Owner」扩展为「Device Owner 或 Shizuku」，
 * 引导页据此判断是否需要走 Shizuku 授权流程。
 */
object AppCapabilities {

    data class Status(
        val deviceOwnerGranted: Boolean,
        val shizukuInstalled: Boolean,
        val shizukuRunning: Boolean,
        val shizukuPermissionGranted: Boolean,
        val autoTimeZoneEnabled: Boolean,
        val exactAlarmGranted: Boolean,
    ) {
        /** 时区控制就绪 = 任一通道可用（Shizuku 优先；Device Owner 兜底） */
        val timezoneControlReady: Boolean get() = deviceOwnerGranted || shizukuPermissionGranted

        /** 就绪通道描述（展示用） */
        val timezoneControlDesc: String
            get() = when {
                shizukuPermissionGranted -> "Shizuku 已授权"
                deviceOwnerGranted -> "设备管理员已授权"
                else -> "未授权"
            }

        val allReady: Boolean get() = timezoneControlReady && !autoTimeZoneEnabled && exactAlarmGranted
    }

    fun check(context: Context): Status {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shizukuRunning = ShizukuManager.isShizukuRunning()
        return Status(
            deviceOwnerGranted = DeviceOwner.isDeviceOwner(context),
            shizukuInstalled = ShizukuManager.isShizukuInstalled(context),
            shizukuRunning = shizukuRunning,
            shizukuPermissionGranted = shizukuRunning && ShizukuPermission.isGranted(),
            autoTimeZoneEnabled =
                Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 1) != 0,
            exactAlarmGranted = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms(),
        )
    }
}
