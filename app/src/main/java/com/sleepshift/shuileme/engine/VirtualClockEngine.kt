package com.sleepshift.shuileme.engine

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 偏移模式：FIXED 固定 / GRADUAL 渐进 / FLUCTUATION 自然波动。
 * 复用 SleepShift Legacy 的偏移策略思想（纯计算），但为独立实现，不修改 Legacy 架构。
 */
enum class OffsetMode { FIXED, GRADUAL, FLUCTUATION }

/** 虚拟时钟用户配置（纯 Kotlin） */
data class VirtualClockConfig(
    val offsetMin: Int = VirtualClockEngine.DEFAULT_OFFSET_MIN,
    val mode: OffsetMode = OffsetMode.FIXED,
    val gradualStepMin: Int = VirtualClockEngine.DEFAULT_GRADUAL_STEP_MIN,
    val fluctuationRangeMin: Int = VirtualClockEngine.DEFAULT_FLUCTUATION_RANGE_MIN,
) {
    val clampedOffsetMin: Int get() = offsetMin.coerceIn(MIN_OFFSET_MIN, MAX_OFFSET_MIN)
    val clampedGradualStepMin: Int get() = gradualStepMin.coerceIn(15, clampedOffsetMin)
    val clampedFluctuationRangeMin: Int get() = fluctuationRangeMin.coerceIn(0, clampedOffsetMin)

    companion object {
        const val MIN_OFFSET_MIN = 0
        const val MAX_OFFSET_MIN = 240
    }
}

/**
 * 虚拟时钟引擎（纯 Kotlin，无 Android 依赖，可 JVM 单测）。
 *
 * 核心公式：`virtualTime = realTime + offsetMin * 60_000`
 * - 偏移恒 ≥ 0（让时间更晚：真实 23:30 + 120min → 虚拟 01:30）；
 * - 真实时间永不修改；虚拟时间只存在于 App 内部展示层；
 * - 无任何系统副作用（不触碰 setTimeZone / setTime / 时区 / 状态栏）。
 */
class VirtualClockEngine(private val config: VirtualClockConfig = VirtualClockConfig()) {

    /** 基础公式：真实时间 + 偏移 */
    fun virtualTimeMs(realTimeMs: Long): Long =
        realTimeMs + config.clampedOffsetMin * 60_000L

    /**
     * 某夜有效偏移（dayIndex 从 0 开始，0=今夜）。
     * - FIXED：恒为目标偏移；
     * - GRADUAL：min(目标, step × (dayIndex+1))，逐日递增至目标封顶；
     * - FLUCTUATION：目标 ± range，确定性伪随机（同一 dayIndex 结果恒定，可测）。
     */
    fun effectiveOffsetMin(dayIndex: Int, seed: Int = 0): Int {
        val target = config.clampedOffsetMin
        return when (config.mode) {
            OffsetMode.FIXED -> target
            OffsetMode.GRADUAL -> minOf(target, config.clampedGradualStepMin * (dayIndex + 1))
            OffsetMode.FLUCTUATION -> {
                val range = config.clampedFluctuationRangeMin
                (target + deterministicDelta(dayIndex, seed, range))
                    .coerceIn(VirtualClockConfig.MIN_OFFSET_MIN, VirtualClockConfig.MAX_OFFSET_MIN)
            }
        }
    }

    /** 虚拟时间格式化（本地时区 HH:mm） */
    fun formatVirtualTime(realTimeMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        TIME_FMT.format(Instant.ofEpochMilli(virtualTimeMs(realTimeMs)).atZone(zoneId))

    /** 虚拟时间与真实时间对照（HH:mm, HH:mm） */
    fun virtualAndReal(realTimeMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Pair<String, String> {
        val virtual = TIME_FMT.format(Instant.ofEpochMilli(virtualTimeMs(realTimeMs)).atZone(zoneId))
        val real = TIME_FMT.format(Instant.ofEpochMilli(realTimeMs).atZone(zoneId))
        return virtual to real
    }

    /** 确定性伪随机：[-range, +range]，同一 (dayIndex, seed) 恒定 */
    private fun deterministicDelta(dayIndex: Int, seed: Int, range: Int): Int {
        if (range <= 0) return 0
        val h = dayIndex.toLong() * 2654435761L + seed.toLong() * 40503L + 1L
        val mixed = ((h ushr 32) xor h) and 0x7fffffffL
        return ((mixed % (2L * range + 1)).toInt()) - range
    }

    companion object {
        const val MIN_OFFSET_MIN = 0
        const val MAX_OFFSET_MIN = 240
        const val DEFAULT_OFFSET_MIN = 120
        const val DEFAULT_GRADUAL_STEP_MIN = 30
        const val DEFAULT_FLUCTUATION_RANGE_MIN = 30
        /** 目标睡眠时长（分钟），用于睡眠进度/满月判定 */
        const val DEFAULT_TARGET_SLEEP_MIN = 420
        private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
