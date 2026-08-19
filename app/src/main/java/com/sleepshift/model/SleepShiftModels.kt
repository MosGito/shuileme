package com.sleepshift.model

import kotlin.math.abs

/**
 * 偏移策略模式。
 * FIXED：每晚固定偏移；GRADUAL：逐日递增至目标；FLUCTUATION：目标值±波动范围。
 */
enum class SleepShiftMode { FIXED, GRADUAL, FLUCTUATION }

/** 用户可编辑的配置（Preferences DataStore 持久化） */
data class SleepShiftSettings(
    val enabled: Boolean = false,
    /** 开始偏移时间（当天第几分钟，0-1439） */
    val startTimeMin: Int = DEFAULT_START_TIME_MIN,
    /** 恢复时间（系统显示时间达到该值时恢复，当天第几分钟，0-1439） */
    val restoreTimeMin: Int = DEFAULT_RESTORE_TIME_MIN,
    /** 偏移量（分钟，0-180，15 的倍数；同时作为 GRADUAL/FLUCTUATION 的目标值） */
    val offsetMin: Int = DEFAULT_OFFSET_MIN,
    val mode: SleepShiftMode = SleepShiftMode.FIXED,
    /** 渐进模式：每日步进（分钟） */
    val gradualStepMin: Int = DEFAULT_GRADUAL_STEP_MIN,
    /** 自然波动模式：目标值 ± 波动范围（分钟） */
    val fluctuationRangeMin: Int = DEFAULT_FLUCTUATION_RANGE_MIN,
)

/** 调度器内部运行时状态（UI 只读展示；仅 Scheduler 写入） */
data class SchedulerState(
    /** 渐进模式进度：已执行第 N 晚 */
    val gradualProgressDays: Int = 0,
    /** 波动模式：前一晚实际偏移（用于限制每日变化幅度） */
    val fluctuationPrevOffsetMin: Int = DEFAULT_OFFSET_MIN,
    /** 今晚实际偏移（首页展示"当前偏移"） */
    val currentOffsetMin: Int = 0,
    /** 原始时区 ID；首次启用时保存，写保护不可覆盖 */
    val originalTimezoneId: String = "",
    /** 已武装夜晚的 epochDay（防止同一晚重复推进策略状态） */
    val armedEpochDay: Long = -1L,
    /** 是否已武装（已安排下一次偏移/恢复闹钟） */
    val armed: Boolean = false,
    /** 下次开始偏移时刻（epoch ms）；未武装为 -1 */
    val nextShiftEpoch: Long = -1L,
    /** 下次恢复时刻（epoch ms）；未武装为 -1 */
    val nextRestoreEpoch: Long = -1L,
)

/**
 * 推导出的一个睡眠窗口（真实时刻调度依据）。
 *
 * 核心语义（v2 恢复逻辑）：
 * - 开始触发时刻 = 真实时间到达 startTime 的那一刻；
 * - 恢复触发时刻 = 开始触发时刻 + realWindowMin，
 *   即"系统显示时间达到 restoreTime"的那一刻（而非原始时区的真实 restoreTime）。
 *
 * 例：start=22:30、restore=06:30、offset=+120 →
 *   nightLength=480min（显示时长）、realWindow=360min →
 *   真实 22:30 偏移，真实 04:30（显示 06:30）恢复。
 */
data class NightWindow(
    val startTimeMin: Int,
    val restoreTimeMin: Int,
    val offsetMin: Int,
    /** 显示时间下 开始→恢复 的时长（未扣偏移），分钟 */
    val nightLengthMin: Int,
    /** 真实时长 = nightLength - offset，分钟 */
    val realWindowMin: Int,
)

const val MINUTES_PER_DAY = 1440

const val MIN_OFFSET_MIN = 0
const val MAX_OFFSET_MIN = 180
const val OFFSET_STEP_MIN = 15

/** 夜间显示时长合理区间（1h ~ 16h），超出视为配置非法 */
const val MIN_NIGHT_LENGTH_MIN = 60
const val MAX_NIGHT_LENGTH_MIN = 16 * 60

const val DEFAULT_START_TIME_MIN = 22 * 60 + 30   // 22:30
const val DEFAULT_RESTORE_TIME_MIN = 6 * 60 + 30  // 06:30
const val DEFAULT_OFFSET_MIN = 120
const val DEFAULT_GRADUAL_STEP_MIN = 30
const val DEFAULT_FLUCTUATION_RANGE_MIN = 30

/** 夜间显示时长（分钟）：恢复时间与开始时间之差，跨天取模到 [0,1440) */
fun nightLengthMin(startTimeMin: Int, restoreTimeMin: Int): Int =
    ((restoreTimeMin - startTimeMin) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY

/** 计算睡眠窗口；配置非法（夜间时长过短/过长，或偏移不小于夜间时长）返回 null */
fun computeNightWindow(settings: SleepShiftSettings): NightWindow? {
    val offset = settings.offsetMin.coerceIn(MIN_OFFSET_MIN, MAX_OFFSET_MIN)
    val nightLen = nightLengthMin(settings.startTimeMin, settings.restoreTimeMin)
    if (nightLen < MIN_NIGHT_LENGTH_MIN || nightLen > MAX_NIGHT_LENGTH_MIN) return null
    if (offset >= nightLen) return null
    return NightWindow(
        startTimeMin = settings.startTimeMin,
        restoreTimeMin = settings.restoreTimeMin,
        offsetMin = offset,
        nightLengthMin = nightLen,
        realWindowMin = nightLen - offset,
    )
}

/**
 * 生成应用偏移后的系统时区 ID（如原始 UTC+8、偏移 +135min → "GMT+10:15"）。
 * 偏移为 15 的倍数，分钟位恒为 00/15/30/45，属 ICU 可解析的 GMT±HH:MM 格式。
 */
fun buildShiftTimeZoneId(originalOffsetMillis: Int, offsetMin: Int): String {
    val totalMinutes = originalOffsetMillis / 60_000 + offsetMin
    val sign = if (totalMinutes < 0) "-" else "+"
    val hh = abs(totalMinutes) / 60
    val mm = abs(totalMinutes) % 60
    return "GMT$sign${hh.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
}
