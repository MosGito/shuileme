package com.sleepshift.permission

import android.content.Context
import com.sleepshift.admin.DeviceOwner
import java.io.File

/**
 * 综合能力解析：聚合各引擎通道的当前状态。
 * 供调试页 / 配置向导使用；纯只读，不发起任何修改。
 */
class CapabilityResolver(private val context: Context) {

    fun resolve(): CapabilityState {
        val shizukuRunning = ShizukuManager.isShizukuRunning()
        return CapabilityState(
            deviceOwner = DeviceOwner.isDeviceOwner(context),
            shizukuInstalled = ShizukuManager.isShizukuInstalled(context),
            shizukuRunning = shizukuRunning,
            shizukuPermissionGranted = shizukuRunning && ShizukuPermission.isGranted(),
            rootAvailable = isRootAvailable(),
        )
    }

    /**
     * Root 检测（只探测 su 二进制文件是否存在，不执行任何 shell）。
     * 返回 false 不代表一定无 root（部分 magisk 隐藏了 su），仅作能力指示。
     */
    private fun isRootAvailable(): Boolean {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/system/bin/.su",
            "/data/local/bin/su",
        )
        return suPaths.any { File(it).exists() }
    }
}
