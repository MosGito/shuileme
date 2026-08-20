package com.sleepshift.engine

import android.content.Context
import com.sleepshift.permission.ShizukuManager
import com.sleepshift.permission.ShizukuPermission

/**
 * Shizuku 通道引擎（M1 骨架）。
 *
 * Phase 11-B 仅实现 [isAvailable] / [type]；实际 shell 时区修改
 * （settings put global auto_time_zone / service call alarm）待后续接入。
 */
class ShizukuTimeShiftEngine(private val context: Context) : TimeShiftEngine {

    override val type: EngineType = EngineType.SHIZUKU

    /** 可用 = Shizuku 服务运行中 且 已授权给本应用 */
    override val isAvailable: Boolean
        get() = ShizukuManager.isShizukuRunning() && ShizukuPermission.isGranted()

    override suspend fun setTimeZone(zoneId: String): TimeShiftResult =
        TimeShiftResult.fail(type, "Shizuku setTimeZone 未实现（待接入 shell）")

    override suspend fun setAutoTimeZoneEnabled(enabled: Boolean): TimeShiftResult =
        TimeShiftResult.fail(type, "Shizuku setAutoTimeZone 未实现（待接入 shell）")
}
