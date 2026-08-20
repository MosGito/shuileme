package com.sleepshift.permission

import com.sleepshift.engine.EngineType

/**
 * 应用整体能力状态（调试页 / 配置向导展示用）。
 */
data class CapabilityState(
    /** Device Owner 已授权 */
    val deviceOwner: Boolean,
    /** Shizuku 应用已安装 */
    val shizukuInstalled: Boolean,
    /** Shizuku 服务运行中（binder 已连接） */
    val shizukuRunning: Boolean,
    /** Shizuku 已授权给本应用 */
    val shizukuPermissionGranted: Boolean,
    /** 检测到 root（su 存在，仅文件探测，不执行） */
    val rootAvailable: Boolean,
) {
    /** 当前应激活的引擎（优先级：Shizuku > Device Owner > 无） */
    val activeEngine: EngineType
        get() = when {
            shizukuRunning && shizukuPermissionGranted -> EngineType.SHIZUKU
            deviceOwner -> EngineType.DEVICE_OWNER
            else -> EngineType.NONE
        }
}
