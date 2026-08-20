package com.sleepshift.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log

/** Device Owner 能力封装（setTimeZone 仅 Device Owner 可调用） */
object DeviceOwner {

    private const val TAG = "DeviceOwner"

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, DeviceAdminReceiver::class.java)

    fun isDeviceOwner(context: Context): Boolean =
        context.getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(context.packageName)

    /**
     * 设置系统时区；非 Device Owner 或调用失败返回 false。
     * 注意：自动时区（AUTO_TIME_ZONE）开启时 setTimeZone 会拒绝并返回 false，需先关闭。
     */
    fun setTimeZone(context: Context, zoneId: String): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val isDo = dpm.isDeviceOwnerApp(context.packageName)
        if (!isDo) {
            Log.w(TAG, "setTimeZone zone=$zoneId 非 Device Owner")
            return false
        }
        ensureAutoTimeZoneDisabled(context)
        val admin = adminComponent(context)
        val result = dpm.setTimeZone(admin, zoneId)
        Log.i(TAG, "setTimeZone zone=$zoneId admin=$admin result=$result")
        return result
    }

    /** 开关自动时区（Device Owner 可写全局设置）；失败返回 false */
    fun setAutoTimeZone(context: Context, enabled: Boolean): Boolean {
        return try {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.AUTO_TIME_ZONE,
                if (enabled) 1 else 0,
            )
            Log.i(TAG, "setAutoTimeZone(enabled=$enabled)")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "setAutoTimeZone(enabled=$enabled) 失败：${e.message}")
            false
        }
    }

    /** Device Owner 可写 AUTO_TIME_ZONE；开启自动时区时系统会拒绝固定时区 */
    private fun ensureAutoTimeZoneDisabled(context: Context) {
        try {
            if (Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 1) != 0) {
                Settings.Global.putInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 0)
                Log.i(TAG, "已关闭自动时区（AUTO_TIME_ZONE=0）")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "关闭自动时区失败：${e.message}")
        }
    }
}
