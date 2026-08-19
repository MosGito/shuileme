package com.sleepshift.time

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sleepshift.admin.DeviceOwner
import com.sleepshift.data.SettingsRepository
import com.sleepshift.notify.NotificationHelper
import com.sleepshift.model.MAX_NIGHT_LENGTH_MIN
import com.sleepshift.model.MIN_NIGHT_LENGTH_MIN
import com.sleepshift.model.SchedulerState
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings
import com.sleepshift.model.buildShiftTimeZoneId
import com.sleepshift.model.nightLengthMin
import com.sleepshift.strategy.FixedStrategy
import com.sleepshift.strategy.FluctuationStrategy
import com.sleepshift.strategy.GradualStrategy
import com.sleepshift.strategy.OffsetStrategy
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.flow.first

/**
 * 时区调度核心（阶段 4-A）。
 *
 * - 所有时间/偏移均来自 [SleepShiftSettings]，无任何硬编码；
 * - 恢复时刻 = 开始时刻 + realWindowMin（真实窗口 = 夜间显示时长 − 当晚实际偏移），
 *   即"系统显示时间达到恢复时间"那一刻（如 +120min：真实 04:30 恢复，显示 06:30）；
 * - 动态时区 ID：`buildShiftTimeZoneId` → `GMT±HH:MM`（15 分钟步进）；
 * - 每晚经 [OffsetStrategy] 推进实际偏移，`armedEpochDay` 防止同一晚重复推进。
 *
 * AlarmReceiver/BootReceiver 在阶段 5 接入；DPM.setTimeZone 在阶段 6 接入。
 */
class TimezoneScheduler(
    private val context: Context,
    private val repository: SettingsRepository,
) {

    /** 读取当前配置并武装下一次完整夜晚（偏移/恢复各一个精确闹钟） */
    suspend fun arm() {
        val settings = repository.settings.first()
        val state = repository.schedulerState.first()
        val originalZoneId = currentOriginalZoneId(state)
        val planned = planNight(settings, state, System.currentTimeMillis(), originalZoneId)
        if (!planned.valid) {
            Log.w(TAG, "arm: 配置无效或未启用，取消武装")
            cancel()
            return
        }
        repository.updateSchedulerState { planned.state }
        scheduleAlarms(planned.shiftEpoch, planned.restoreEpoch, planned.shiftZoneId, originalZoneId)
        Log.i(
            TAG,
            "arm: offset=${planned.offsetMin}, shift=${
                Instant.ofEpochMilli(planned.shiftEpoch)
            }, restore=${Instant.ofEpochMilli(planned.restoreEpoch)}, zone=${planned.shiftZoneId}"
        )
    }

    /**
     * Debug 测试入口：模拟"新的一晚"——清除武装标记后重新 arm()，
     * 使策略（GRADUAL/FLUCTUATION）按日推进，无需等待真实时间。
     */
    suspend fun forceAdvanceNight() {
        repository.updateSchedulerState { it.copy(armedEpochDay = -1L) }
        arm()
    }

    /** 取消武装并清空调度状态 */
    suspend fun cancel() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(shiftPendingIntent(emptyMap()))
        alarmManager.cancel(restorePendingIntent(emptyMap()))
        repository.updateSchedulerState {
            it.copy(armed = false, nextShiftEpoch = -1L, nextRestoreEpoch = -1L)
        }
        Log.i(TAG, "cancel: 闹钟已清除")
    }

    /**
     * 执行偏移：读取 DataStore 当前状态（SchedulerState.currentOffsetMin）计算偏移时区并 setTimeZone。
     * **不信任 PendingIntent 携带的 zone_id**；含防御性时区解析校验。
     */
    suspend fun applyShift() {
        val state = repository.schedulerState.first()
        val originalZoneId = state.originalTimezoneId
        if (originalZoneId.isEmpty()) {
            Log.w(TAG, "applyShift: 未保存原始时区，跳过")
            return
        }
        val offset = state.currentOffsetMin
        if (offset == 0) {
            Log.i(TAG, "applyShift: 偏移为 0，跳过")
            return
        }
        val zone = buildShiftZoneId(originalZoneId, offset)
        val expectedOffsetMs = TimeZone.getTimeZone(originalZoneId).rawOffset + offset * 60_000
        val resolved = TimeZone.getTimeZone(zone)
        Log.i(TAG, "applyShift: offset=$offset zone=$zone resolvedId=${resolved.id} rawOffset=${resolved.rawOffset}")
        if (resolved.rawOffset != expectedOffsetMs) {
            Log.e(TAG, "applyShift: 时区解析不符 expected=$expectedOffsetMs zone=$zone")
            return
        }
        val ok = DeviceOwner.setTimeZone(context, zone)
        Log.i(TAG, "applyShift: setTimeZone($zone) ok=$ok")
        if (ok) NotificationHelper.notifySleepModeStarted(context)
    }

    /** 执行恢复：读取 DataStore 原始时区并 setTimeZone */
    suspend fun applyRestore() {
        val state = repository.schedulerState.first()
        val originalZoneId = state.originalTimezoneId
        if (originalZoneId.isEmpty()) {
            Log.w(TAG, "applyRestore: 未保存原始时区，跳过")
            return
        }
        val ok = DeviceOwner.setTimeZone(context, originalZoneId)
        Log.i(TAG, "applyRestore: zone=$originalZoneId ok=$ok")
        if (ok) NotificationHelper.notifyTimeRestored(context)
    }

    /**
     * 设备重启后调用：若当前处于偏移窗口且恢复时刻在未来，补充武装当前窗口的恢复闹钟。
     * 不覆盖当前正确状态（不强行恢复/不重置偏移）。
     */
    suspend fun ensureActiveWindowRestore() {
        val settings = repository.settings.first()
        val state = repository.schedulerState.first()
        if (!state.armed || state.originalTimezoneId.isEmpty()) return

        val restoreEpoch = computeActiveRestoreEpoch(
            nowEpochMillis = System.currentTimeMillis(),
            currentZoneId = TimeZone.getDefault().id,
            originalZoneId = state.originalTimezoneId,
            startTimeMin = settings.startTimeMin,
            restoreTimeMin = settings.restoreTimeMin,
            offsetMin = state.currentOffsetMin,
        ) ?: return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, restoreEpoch,
            restorePendingIntent(mapOf(EXTRA_ZONE_ID to state.originalTimezoneId)),
        )
        Log.i(TAG, "ensureActiveWindowRestore: 当前偏移窗口恢复闹钟已补充 epoch=$restoreEpoch")
    }

    private suspend fun currentOriginalZoneId(state: SchedulerState): String =
        state.originalTimezoneId.ifEmpty { TimeZone.getDefault().id }

    private fun scheduleAlarms(shiftEpoch: Long, restoreEpoch: Long, shiftZoneId: String, originalZoneId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shiftPI = shiftPendingIntent(mapOf(EXTRA_ZONE_ID to shiftZoneId))
        val restorePI = restorePendingIntent(mapOf(EXTRA_ZONE_ID to originalZoneId))
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
            // 无 SCHEDULE_EXACT_ALARM 权限时回退 setAlarmClock（无需权限，Doze 下也精确触发）
            Log.w(TAG, "无精确闹钟权限，回退 setAlarmClock")
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(shiftEpoch, null), shiftPI)
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(restoreEpoch, null), restorePI)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, shiftEpoch, shiftPI)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restoreEpoch, restorePI)
    }

    private fun shiftPendingIntent(extras: Map<String, String>): PendingIntent =
        pendingIntent(ACTION_SHIFT, REQUEST_CODE_SHIFT, extras)

    private fun restorePendingIntent(extras: Map<String, String>): PendingIntent =
        pendingIntent(ACTION_RESTORE, REQUEST_CODE_RESTORE, extras)

    /** PendingIntent 目标为 [RECEIVER_CLASS_NAME]（阶段 5 创建同名 Receiver 并注册后即生效） */
    private fun pendingIntent(action: String, requestCode: Int, extras: Map<String, String>): PendingIntent {
        val intent = Intent()
            .setClassName(context.packageName, RECEIVER_CLASS_NAME)
            .setAction(action)
        extras.forEach { (k, v) -> intent.putExtra(k, v) }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    companion object {
        private const val TAG = "TimezoneScheduler"

        const val ACTION_SHIFT = "com.sleepshift.action.SHIFT"
        const val ACTION_RESTORE = "com.sleepshift.action.RESTORE"
        const val EXTRA_ZONE_ID = "zone_id"

        /** AlarmReceiver 完整类名（阶段 5 创建同名 Receiver 后即生效） */
        const val RECEIVER_CLASS_NAME = "com.sleepshift.AlarmReceiver"

        private const val REQUEST_CODE_SHIFT = 100
        private const val REQUEST_CODE_RESTORE = 200
        private const val DAY_MS = 86_400_000L

        private fun strategyFor(mode: SleepShiftMode): OffsetStrategy = when (mode) {
            SleepShiftMode.FIXED -> FixedStrategy
            SleepShiftMode.GRADUAL -> GradualStrategy
            SleepShiftMode.FLUCTUATION -> FluctuationStrategy()
        }

        /**
         * 规划下一次完整夜晚（纯函数，JVM 可单测）。
         * 同一晚只推进一次策略状态；重复武装同夜复用当前偏移。
         */
        fun planNight(
            settings: SleepShiftSettings,
            state: SchedulerState,
            nowEpochMillis: Long,
            originalZoneId: String,
        ): PlannedNight {
            val nightLen = nightLengthMin(settings.startTimeMin, settings.restoreTimeMin)
            if (!settings.enabled || nightLen < MIN_NIGHT_LENGTH_MIN || nightLen > MAX_NIGHT_LENGTH_MIN) {
                return PlannedNight.invalid()
            }
            val shiftEpoch = computeNextShiftEpoch(settings.startTimeMin, originalZoneId, nowEpochMillis)
            val nightEpochDay = shiftEpoch / DAY_MS
            val (offset, advancedState) = if (nightEpochDay != state.armedEpochDay) {
                // 新夜晚：推进策略状态并取偏移
                val result = strategyFor(settings.mode).next(settings, state)
                result.offsetMin to result.state
            } else {
                // 同一晚重新武装：不推进策略进度；FIXED 按当前设置重算（跟随用户修改），GRADUAL/FLUCTUATION 保留已定偏移
                when (settings.mode) {
                    SleepShiftMode.FIXED -> settings.offsetMin to state
                    else -> state.currentOffsetMin to state
                }
            }
            val realWindow = nightLen - offset
            if (realWindow <= 0) return PlannedNight.invalid()

            val restoreEpoch = computeRestoreEpoch(shiftEpoch, realWindow)
            val armedState = advancedState.copy(
                originalTimezoneId = originalZoneId,
                currentOffsetMin = offset,
                armedEpochDay = nightEpochDay,
                armed = true,
                nextShiftEpoch = shiftEpoch,
                nextRestoreEpoch = restoreEpoch,
            )
            return PlannedNight(
                valid = true,
                offsetMin = offset,
                state = armedState,
                shiftEpoch = shiftEpoch,
                restoreEpoch = restoreEpoch,
                shiftZoneId = buildShiftZoneId(originalZoneId, offset),
            )
        }

        /** 下一次开始偏移时刻：原始时区中 startTime 的下一次出现 */
        fun computeNextShiftEpoch(startTimeMin: Int, originalZoneId: String, nowEpochMillis: Long): Long {
            val zone = ZoneId.of(originalZoneId)
            val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
            var candidate = now.toLocalDate().atTime(startTimeMin / 60, startTimeMin % 60).atZone(zone)
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            return candidate.toInstant().toEpochMilli()
        }

        /** 上一次开始偏移时刻：原始时区中 startTime 在 now 之前最近的一次出现 */
        fun computePreviousShiftEpoch(startTimeMin: Int, originalZoneId: String, nowEpochMillis: Long): Long {
            val zone = ZoneId.of(originalZoneId)
            val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
            var candidate = now.toLocalDate().atTime(startTimeMin / 60, startTimeMin % 60).atZone(zone)
            if (candidate.isAfter(now)) candidate = candidate.minusDays(1)
            return candidate.toInstant().toEpochMilli()
        }

        /**
         * 计算"偏移状态启动"时当前偏移窗口的恢复时刻（纯函数，JVM 可单测）。
         * 当前未偏移、或恢复时刻已过 → 返回 null。
         */
        fun computeActiveRestoreEpoch(
            nowEpochMillis: Long,
            currentZoneId: String,
            originalZoneId: String,
            startTimeMin: Int,
            restoreTimeMin: Int,
            offsetMin: Int,
        ): Long? {
            if (currentZoneId == originalZoneId) return null
            val activeStart = computePreviousShiftEpoch(startTimeMin, originalZoneId, nowEpochMillis)
            val realWindow = nightLengthMin(startTimeMin, restoreTimeMin) - offsetMin
            val restoreEpoch = activeStart + realWindow * 60_000L
            return if (restoreEpoch > nowEpochMillis) restoreEpoch else null
        }

        /** 恢复时刻：开始时刻 + 真实窗口（分钟） */
        fun computeRestoreEpoch(shiftEpochMillis: Long, realWindowMin: Int): Long =
            shiftEpochMillis + realWindowMin * 60_000L

        /**
         * 动态偏移时区 ID。
         * 整小时总偏移 → IANA `Etc/GMT±H`（Android 新版 setTimeZone 仅应用 tzdb 内的 ID）；
         * 分数分钟偏移 → 回退自定义 `GMT±HH:MM`（注意：部分平台 setTimeZone 静默忽略，运行时告警）。
         */
        fun buildShiftZoneId(originalZoneId: String, offsetMin: Int): String {
            val originalOffsetMs = TimeZone.getTimeZone(originalZoneId).rawOffset
            val totalMinutes = originalOffsetMs / 60_000 + offsetMin
            if (totalMinutes % 60 == 0) {
                val hours = totalMinutes / 60
                // Etc/GMT 为 POSIX 符号反转：UTC+H → Etc/GMT-H
                return if (hours >= 0) "Etc/GMT-$hours" else "Etc/GMT+${-hours}"
            }
            return buildShiftTimeZoneId(originalOffsetMs, offsetMin)
        }
    }
}

/** 一次完整夜晚的调度规划结果 */
data class PlannedNight(
    val valid: Boolean,
    val offsetMin: Int = 0,
    val state: SchedulerState = SchedulerState(),
    val shiftEpoch: Long = -1L,
    val restoreEpoch: Long = -1L,
    val shiftZoneId: String = "",
) {
    companion object {
        fun invalid() = PlannedNight(valid = false)
    }
}
