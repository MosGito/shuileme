package com.sleepshift.shuileme.model

/** 睡眠状态 */
enum class SleepState { AWAKE, SLEEPING }

/**
 * 一次睡眠会话（MVP 记录）。
 * - 仅记录「我要睡了」「我醒啦」两个真实时刻 + 当夜偏移；
 * - 睡眠时长由两个时刻推导（真实时长，供统计与未来人格分析）。
 */
data class SleepSession(
    val sleepStartAtMs: Long,
    val wakeAtMs: Long? = null,
    val effectiveOffsetMin: Int = 0,
) {
    /** 真实睡眠时长（分钟）；未醒时 null */
    val durationMin: Long? get() = wakeAtMs?.let { (it - sleepStartAtMs) / 60_000L }
    val isSleeping: Boolean get() = wakeAtMs == null
}

/**
 * 真实经过的睡眠时长（分钟）。
 *
 * - 严格基于真实时间基准（[sleepStartAtMs] 是真实入睡时刻），与虚拟时间偏移完全无关；
 * - 刚入睡 → 0；真实经过 30 分钟 → 30（无论偏移是多少）；
 * - 钳制 ≥ 0：防止系统时钟回拨等异常产生负数。
 *
 * 供「已睡多久 / 睡眠状态持续时间」展示使用；
 * 统计 / 人格 / 月亮成长仍走 [SleepSession.durationMin]。
 */
fun realSleepElapsedMin(sleepStartAtMs: Long, nowMs: Long): Long =
    ((nowMs - sleepStartAtMs).coerceAtLeast(0L)) / 60_000L

/**
 * SL-6 睡眠人格分析数据结构（仅存储，不生成人格）。
 *
 * 未来人格方向：
 * - 🌙🐱 夜猫子
 * - 🌞🐦 早起鸟
 * - 🐮🐴 牛马型
 * - 🌊🦦 海獭型
 * - 🌪️ 混沌型
 *
 * 本阶段只设计字段，供未来基于历史会话聚合生成人格；暂不实现算法。
 */
data class SleepPersonalityData(
    /** 平均入睡时刻（相对当日 00:00 的偏移 ms） */
    val averageSleepTimeMs: Long? = null,
    /** 平均起床时刻（相对当日 00:00 的偏移 ms） */
    val averageWakeTimeMs: Long? = null,
    /** 平均睡眠时长（分钟） */
    val averageDurationMin: Long? = null,
    /** 睡眠规律程度 0.0~1.0（越接近 1 越规律） */
    val regularityScore: Double? = null,
    /** 夜间活跃次数（如夜间亮屏次数，待 SL-7 采集） */
    val nightlyActiveCount: Int? = null,
    /** 提醒响应率 0.0~1.0（提醒后是否按时响应，待 SL-4 采集） */
    val reminderResponseRate: Double? = null,
)
