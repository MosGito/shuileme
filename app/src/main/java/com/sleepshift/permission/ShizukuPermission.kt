package com.sleepshift.permission

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 授权（权限状态 + 请求授权）。
 * 不执行时区修改。
 */
object ShizukuPermission {

    /** 授权请求码（供结果监听区分） */
    const val REQUEST_CODE = 1000

    /** Shizuku 是否已授权 */
    fun isGranted(): Boolean =
        try {
            Shizuku.isPreV11() ||
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }

    /** 发起授权请求（需 Shizuku 运行中；结果通过 OnRequestPermissionResultListener 回调） */
    fun requestPermission() {
        try {
            if (!Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
            ) {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Throwable) {
            // 未连接时无法发起授权（调用方应先行检测运行状态）
        }
    }
}
