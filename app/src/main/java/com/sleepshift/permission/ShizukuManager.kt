package com.sleepshift.permission

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 状态检测（只读）。
 *
 * 只负责：是否安装 / 是否运行 / binder 状态 / 版本信息。
 * 不在这里执行权限请求，也不执行时区修改。
 */
object ShizukuManager {

    /** Shizuku 应用包名（官方应用） */
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    /** Shizuku 是否已安装 */
    fun isShizukuInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    /** Shizuku binder 是否已连接（即 Shizuku 服务是否运行） */
    fun isShizukuRunning(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }

    /** binder 状态描述（调试用） */
    fun getBinderStatus(): String = if (isShizukuRunning()) "connected" else "not-connected"

    /** Shizuku 版本号；未连接/未安装时返回 -1 */
    fun getShizukuVersion(): Int =
        try {
            Shizuku.getVersion()
        } catch (e: Throwable) {
            -1
        }
}
