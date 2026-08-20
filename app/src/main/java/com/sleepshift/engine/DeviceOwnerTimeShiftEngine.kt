package com.sleepshift.engine

import android.content.Context
import com.sleepshift.admin.DeviceOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Device Owner 通道引擎。
 *
 * 封装原有 [DeviceOwner] 逻辑（setTimeZone / 自动时区），保证现有
 * Device Owner 环境功能不退化为前提。后续 Shizuku/Root 引擎将与其
 * 共同实现 [TimeShiftEngine]，由 [EngineManager] 按优先级选择。
 */
class DeviceOwnerTimeShiftEngine(private val context: Context) : TimeShiftEngine {

    override val type: EngineType = EngineType.DEVICE_OWNER

    override val isAvailable: Boolean
        get() = DeviceOwner.isDeviceOwner(context)

    override suspend fun setTimeZone(zoneId: String): TimeShiftResult = withContext(Dispatchers.IO) {
        val ok = DeviceOwner.setTimeZone(context, zoneId)
        if (ok) {
            TimeShiftResult.ok(type, "setTimeZone($zoneId) OK")
        } else {
            TimeShiftResult.fail(type, "setTimeZone($zoneId) 失败（非 Device Owner 或系统拒绝）")
        }
    }

    override suspend fun setAutoTimeZoneEnabled(enabled: Boolean): TimeShiftResult = withContext(Dispatchers.IO) {
        val ok = DeviceOwner.setAutoTimeZone(context, enabled)
        if (ok) {
            TimeShiftResult.ok(type, "AUTO_TIME_ZONE=${if (enabled) 1 else 0}")
        } else {
            TimeShiftResult.fail(type, "设置 AUTO_TIME_ZONE=${if (enabled) 1 else 0} 失败")
        }
    }
}
