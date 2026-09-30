package com.sleepshift.shuileme.model

import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 睡眠人格类型（SL-6）：5 基础 + 3 隐藏。
 * - 基础：由睡眠数据直接判定；
 * - 隐藏：满足特殊条件解锁（满月/连续熬夜/摆烂）。
 */
enum class SleepPersonalityType(
    val primaryEmoji: String,
    val displayName: String,
    val tagline: String,
    val comboEmojis: List<String>,
    val talkStyle: String,
) {
    /** 🌙🐱 夜猫子 */
    NIGHT_OWL("🌙", "夜猫子", "月亮的朋友，太阳的陌生猫", listOf("🌙", "🌙", "🌙", "🐱", "🐱"), "毒舌"),
    /** 🌞🐦 早起鸟 */
    EARLY_BIRD("🌞", "早起鸟", "早起星人，月亮还没下班你就醒了", listOf("🌞", "🌞", "🌞", "🐦", "🐦"), "元气"),
    /** 🐮🐴 牛马型 */
    WORK_HORSE("🐮", "牛马型", "睡不够的勤劳牛马", listOf("🐮", "🐮", "🐮", "🐴", "🐴"), "自嘲"),
    /** 🌊🦦 海獭型 */
    OTTER("🌊", "海獭型", "抱着月亮睡觉的海獭", listOf("🌊", "🌊", "🌊", "🦦", "🦦"), "温柔"),
    /** 🌪️ 混沌型 */
    CHAOS("🌪️", "混沌型", "睡眠无规律，月亮追不上你", listOf("🌪️", "🌪️", "🌪️", "💫", "💫"), "随缘"),
    // ── 隐藏 ──
    /** 🌕 月亮守护者 */
    MOON_GUARDIAN("🌕", "月亮守护者", "守护月亮满月的守护者", listOf("🌕", "🌕", "🌙", "✨", "✨"), "温柔"),
    /** 🌚 夜行幽灵 */
    NIGHT_GHOST("🌚", "夜行幽灵", "深夜的幽灵，白天见不到", listOf("🌚", "🌚", "🌑", "👻", "👻"), "低沉"),
    /** 💤 摆烂大师 */
    PROCASTINATOR("💤", "摆烂大师", "睡意全无，摆烂第一", listOf("💤", "💤", "😴", "😴", "🛌"), "摆烂"),
}

/** 人格计算指标（纯聚合） */
data class PersonalityMetrics(
    val avgSleepTimeMin: Int? = null,
    val avgWakeTimeMin: Int? = null,
    val avgDurationMin: Long? = null,
    val regularityScore: Double = 0.0,
)

/** 人格引擎输入 */
data class PersonalityInput(
    val sessions: List<SleepSession> = emptyList(),
    val moonLife: MoonLife = MoonLife(),
    val streakDays: Int = 0,
    val sleepCount: Int = 0,
    val reminderResponseRate: Double? = null,
    val nightlyActiveCount: Int? = null,
    /** SL-9：实际入睡 vs 目标入睡 平均偏差（分钟），影响规律度/置信度 */
    val sleepTargetDeviationMin: Long? = null,
)

/** 人格计算输出 */
data class SleepPersonalityState(
    val primaryType: SleepPersonalityType? = null,
    val confidence: Double = 0.0,
    val evolutionProgress: Double = 0.0,
    val residents: List<EmojiResident> = emptyList(),
    val unlockedHidden: Set<SleepPersonalityType> = emptySet(),
    val metrics: PersonalityMetrics = PersonalityMetrics(),
)

/**
 * 睡眠人格引擎（SL-6，纯函数，无 AI）。
 * 输入睡眠记录等 → 输出人格状态（主类型/置信度/进化进度/居民/隐藏解锁）。
 */
object SleepPersonalityEngine {

    const val MIN_SESSIONS = 5

    /** 一天的分钟数 */
    const val MINUTES_PER_DAY = 1440

    /** 环形合成向量长度阈值：低于该值认为样本在圆周上近似均匀分布，环形平均无统计意义 */
    private const val DEGENERATE_EPSILON = 1e-6

    /**
     * 一天中分钟数（0..1439）的环形平均（circular mean）。
     *
     * 数学：theta = 2π * minute / 1440，对 sin/cos 分别取平均，
     * angle = atan2(sinSum, cosSum)（负角 +2π），再换算回 0..1439 分钟。
     *
     * 解决普通算术平均的跨午夜错误：23:50(1430) 与 00:10(10)
     * 的算术平均是 12:00(720)，而环形平均是 00:00(0)。
     *
     * 退化情况：空列表，或样本近似均匀分布（合成向量长度 ≈ 0，如 0 与 720 相对）
     * 时返回 null——此时任何单一"平均时间"都没有统计意义，
     * 宁可不给时间，也不编造一个看似合理的时间。
     *
     * 舍入：roundToInt 后对 1440 取模，保证结果恒在 0..1439。
     */
    internal fun circularMeanMinutes(minutes: List<Int>): Int? {
        if (minutes.isEmpty()) return null
        var sinSum = 0.0
        var cosSum = 0.0
        for (m in minutes) {
            val theta = 2.0 * Math.PI * m / MINUTES_PER_DAY
            sinSum += sin(theta)
            cosSum += cos(theta)
        }
        sinSum /= minutes.size
        cosSum /= minutes.size
        val resultant = sqrt(sinSum * sinSum + cosSum * cosSum)
        if (resultant < DEGENERATE_EPSILON) return null
        var angle = atan2(sinSum, cosSum)
        if (angle < 0.0) angle += 2.0 * Math.PI
        val result = (angle / (2.0 * Math.PI) * MINUTES_PER_DAY).roundToInt()
        return ((result % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    /**
     * 两个分钟数（0..1439）的环形距离，结果范围 0..720。
     * 23:50(1430) 与 00:10(10) 的普通差值是 1420，环形距离是 20。
     */
    internal fun circularDistance(a: Int, b: Int): Int {
        val diff = abs(a - b)
        return min(diff, MINUTES_PER_DAY - diff)
    }

    fun compute(input: PersonalityInput): SleepPersonalityState {
        val sessions = input.sessions
        if (sessions.size < MIN_SESSIONS) {
            return SleepPersonalityState(
                primaryType = null,
                confidence = 0.0,
                evolutionProgress = 0.0,
                residents = ResidentEngine.moonBabies(),
                unlockedHidden = emptySet(),
            )
        }
        val metrics = computeMetrics(sessions)
        val primary = determineType(metrics)
        val confidence = computeConfidence(sessions.size, metrics.regularityScore, input.sleepTargetDeviationMin)
        val evolution = computeEvolution(input, metrics)
        val healthTarget = healthDirection(metrics)
        val unlocked = unlockedHidden(input)
        val residents = ResidentEngine.generateResidents(primary, evolution, healthTarget)
        return SleepPersonalityState(primary, confidence, evolution, residents, unlocked, metrics)
    }

    /** 基础人格判定（优先序：夜猫子>牛马型>早起鸟>海獭型>混沌型；短睡=牛马优先于早起鸟） */
    fun determineType(m: PersonalityMetrics): SleepPersonalityType {
        val avgStart = m.avgSleepTimeMin
        val avgWake = m.avgWakeTimeMin
        val avgDur = m.avgDurationMin
        return when {
            avgStart != null && avgStart in 0..360 -> SleepPersonalityType.NIGHT_OWL      // 00:00-06:00 入睡
            avgDur != null && avgDur < 360 -> SleepPersonalityType.WORK_HORSE              // < 6h
            avgStart != null && avgStart in 1200..1380 && avgWake != null && avgWake < 420 ->
                SleepPersonalityType.EARLY_BIRD
            avgDur != null && avgDur >= 420 -> SleepPersonalityType.OTTER                  // >= 7h
            else -> SleepPersonalityType.CHAOS
        }
    }

    /** SL-9：初始人格倾向（由用户自报作息推算，非真实数据；置信度由真实数据累计） */
    fun initialInclination(sleepTimeMin: Int, wakeTimeMin: Int, idealSleepMin: Int): SleepPersonalityType {
        val window = if (wakeTimeMin > sleepTimeMin) wakeTimeMin - sleepTimeMin
        else (24 * 60 - sleepTimeMin) + wakeTimeMin
        return determineType(
            PersonalityMetrics(
                avgSleepTimeMin = sleepTimeMin,
                avgWakeTimeMin = wakeTimeMin,
                avgDurationMin = window.toLong().coerceAtLeast(idealSleepMin.toLong()),
            )
        )
    }

    fun computeMetrics(sessions: List<SleepSession>): PersonalityMetrics {
        val starts = sessions.mapNotNull { minutesOfDay(it.sleepStartAtMs) }
        val wakes = sessions.mapNotNull { it.wakeAtMs?.let { w -> minutesOfDay(w) } }
        val durations = sessions.mapNotNull { it.durationMin }
        return PersonalityMetrics(
            avgSleepTimeMin = circularMeanMinutes(starts),
            avgWakeTimeMin = circularMeanMinutes(wakes),
            // duration 不是环形变量（时长没有"绕圈"语义），继续使用普通算术平均
            avgDurationMin = if (durations.isNotEmpty()) durations.average().toLong() else null,
            regularityScore = regularityOf(starts),
        )
    }

    fun computeConfidence(sessionCount: Int, regularity: Double, targetDeviationMin: Long? = null): Double {
        var conf = 0.4 + sessionCount * 0.03 + regularity * 0.3
        // SL-9：实际入睡偏离目标越多，置信度越低（最多扣 0.2）
        targetDeviationMin?.let { conf -= min(0.2, it / 1200.0) }
        return conf.coerceIn(0.0, 0.95)
    }

    /** 进化进度 0~1：规律度 + 连续 + 月亮成长（阶段代理）+ 少熬夜 */
    fun computeEvolution(input: PersonalityInput, metrics: PersonalityMetrics): Double {
        val r = metrics.regularityScore
        val streak = (input.streakDays / 30.0).coerceAtMost(0.3)
        val moon = (input.moonLife.stage.ordinal / 4.0) * 0.2
        val late = (1.0 - min(1.0, input.moonLife.recentLateNights / 5.0)) * 0.2
        return (r * 0.3 + streak + moon + late).coerceIn(0.0, 1.0)
    }

    /** 更健康的方向（用于居民迁移） */
    fun healthDirection(m: PersonalityMetrics): SleepPersonalityType =
        if ((m.avgDurationMin ?: 0L) >= 420) SleepPersonalityType.OTTER else SleepPersonalityType.EARLY_BIRD

    /** 隐藏人格解锁 */
    fun unlockedHidden(input: PersonalityInput): Set<SleepPersonalityType> {
        val unlocked = mutableSetOf<SleepPersonalityType>()
        if (input.moonLife.totalRewards >= 1 || input.streakDays >= 7) unlocked += SleepPersonalityType.MOON_GUARDIAN
        if (input.moonLife.recentLateNights >= 3) unlocked += SleepPersonalityType.NIGHT_GHOST
        val tooShort = input.sessions.count { (it.durationMin ?: 0L) < 4 * 60L }
        if (tooShort >= 3) unlocked += SleepPersonalityType.PROCASTINATOR
        return unlocked
    }

    private fun regularityOf(startMinutes: List<Int>): Double {
        if (startMinutes.size < 2) return 0.0
        val mean = circularMeanMinutes(startMinutes) ?: return 0.0
        // 环形标准差：每个样本到环形均值的 circularDistance 的均方根，
        // 与旧公式（普通 stddev / 120 归一化）保持同一尺度与语义，
        // 仅修复"跨午夜普通差值被放大"的数学错误（23:50 与 00:10 只差 20 分钟）。
        val variance = startMinutes.map { d ->
            val diff = circularDistance(d, mean).toDouble()
            diff * diff
        }.average()
        val circularStdDev = sqrt(variance)
        return (1.0 - min(1.0, circularStdDev / 120.0)).coerceIn(0.0, 1.0)
    }

    private fun minutesOfDay(ms: Long): Int {
        val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        return zdt.hour * 60 + zdt.minute
    }
}
