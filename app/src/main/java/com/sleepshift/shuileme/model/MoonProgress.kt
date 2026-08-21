package com.sleepshift.shuileme.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 月亮阶段（emoji，无图片资源） */
enum class MoonPhase(val thresholdPercent: Int, val emoji: String) {
    /** 🌑 新月 0% */
    NEW(0, "🌑"),
    /** 🌒 娥眉月 15% */
    CRESCENT(15, "🌒"),
    /** 🌓 上弦月 30% */
    FIRST_QUARTER(30, "🌓"),
    /** 🌔 盈凸月 45% */
    GIBBOUS(45, "🌔"),
    /** 🌕 满月 100% */
    FULL(100, "🌕");

    companion object {
        /** 由成长值取阶段：取阈值 ≤ growth 的最高阶段 */
        fun fromGrowth(growth: Int): MoonPhase =
            values().lastOrNull { growth >= it.thresholdPercent } ?: NEW
    }
}

/** 睡眠不合格原因 */
enum class SleepResultReason {
    /** 睡眠时长不足 */
    TOO_SHORT,
    /** 入睡过晚 */
    STARTED_TOO_LATE,
}

/** 一次睡眠的完成判定结果 */
data class SleepResult(
    val durationMinutes: Long,
    val isQualified: Boolean,
    val reason: SleepResultReason? = null,
)

/**
 * 月亮成长状态（纯 Kotlin，可 JVM 单测）。
 *
 * 成长规则：
 * - 合格一晚 → growth +20%；
 * - 不合格 → growth 不增加，连续合格计数清零；
 * - 连续合格累计，growth 达 100% → 满月 🌕 → 触发奖励状态 🌝，成长与连续计数重置。
 */
data class MoonProgress(
    /** 当前成长值 0~100 */
    val growthPercent: Int = 0,
    /** 连续合格睡眠次数 */
    val consecutiveQualified: Int = 0,
    /** 总完成睡眠次数 */
    val totalCompletedSleeps: Int = 0,
    /** 最近一次睡眠结果 */
    val lastSleepResult: SleepResult? = null,
    /** 满月奖励待展示状态 */
    val fullMoonRewardPending: Boolean = false,
) {
    val phase: MoonPhase get() = MoonPhase.fromGrowth(growthPercent)
    val phaseEmoji: String get() = phase.emoji

    /**
     * 判定一次睡眠是否合格。
     * 合格 = 睡眠时长 ≥ minDuration（默认 7h） 且 入睡时间 ≤ 目标入睡时间 + 30min。
     */
    fun evaluate(
        sleepStartAtMs: Long,
        wakeAtMs: Long,
        targetSleepTimeMin: Int,
        minDurationMin: Long = DEFAULT_QUALIFIED_DURATION_MIN,
    ): SleepResult {
        val durationMin = (wakeAtMs - sleepStartAtMs) / 60_000L
        val durationOk = durationMin >= minDurationMin
        val startOnTime = isStartOnTime(sleepStartAtMs, targetSleepTimeMin)
        return SleepResult(
            durationMinutes = durationMin,
            isQualified = durationOk && startOnTime,
            reason = when {
                !durationOk -> SleepResultReason.TOO_SHORT
                !startOnTime -> SleepResultReason.STARTED_TOO_LATE
                else -> null
            },
        )
    }

    /** 应用一次睡眠结果，返回新的成长状态 */
    fun applyResult(result: SleepResult): MoonProgress {
        val total = totalCompletedSleeps + 1
        if (!result.isQualified) {
            return copy(
                totalCompletedSleeps = total,
                lastSleepResult = result,
                consecutiveQualified = 0,
            )
        }
        val newConsecutive = consecutiveQualified + 1
        val newGrowth = (growthPercent + GROWTH_STEP_PERCENT).coerceAtMost(100)
        return if (newGrowth >= 100) {
            // 满月：触发奖励，成长与连续计数重置
            copy(
                growthPercent = 0,
                consecutiveQualified = 0,
                totalCompletedSleeps = total,
                lastSleepResult = result,
                fullMoonRewardPending = true,
            )
        } else {
            copy(
                growthPercent = newGrowth,
                consecutiveQualified = newConsecutive,
                totalCompletedSleeps = total,
                lastSleepResult = result,
            )
        }
    }

    companion object {
        const val GROWTH_STEP_PERCENT = 20
        /** 合格时长下限：7 小时 */
        const val DEFAULT_QUALIFIED_DURATION_MIN = 420L
        /** 目标入睡时间 + 宽限 30 分钟 */
        const val START_GRACE_MIN = 30
    }
}

/** 入睡是否及时：入睡时刻 ≤ 目标入睡时间 + 30min（处理跨午夜归属） */
private fun isStartOnTime(sleepStartAtMs: Long, targetSleepTimeMin: Int, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    val zdt = Instant.ofEpochMilli(sleepStartAtMs).atZone(zone)
    // 午夜后（<12:00）入睡归属前一晚的睡眠窗口
    val evening = if (zdt.hour < 12) zdt.toLocalDate().minusDays(1) else zdt.toLocalDate()
    val deadline = LocalDateTime.of(
        evening,
        LocalTime.of((targetSleepTimeMin / 60) % 24, targetSleepTimeMin % 60),
    ).plusMinutes(MoonProgress.START_GRACE_MIN.toLong())
    return !zdt.toLocalDateTime().isAfter(deadline)
}
