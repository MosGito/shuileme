package com.sleepshift.shuileme.model.personality

import kotlin.math.abs
import kotlin.math.min

/**
 * V2.0.7 §4.6 Confidence（纯函数）。
 *
 * ```
 * sampleFactor   = min(1, n / 14)
 * baseConfidence = 0.20 + 0.30·sampleFactor + 0.35·R + 0.10·completeness
 * alignPenalty   = min(0.20, GoalAlignment.startDeviation / 1200)   // startDeviation = circDist(μ_C, targetSleepTime)
 * conf           = clamp(baseConfidence − alignPenalty, 0, 0.95)
 * ```
 *
 * - R undefined → 返回 null（不产生正式 Confidence，禁止默认值 / 禁止把 score 当 confidence）；
 * - 目标作息或 μ_C 缺失 → alignPenalty = 0（§12.1 安全降级，GA 不参与）；
 * - Confidence 不参与 Primary argmax。
 */
internal object Confidence {

    const val SAMPLE_SATURATION = 14
    const val BASE_MIN = 0.20
    const val SAMPLE_WEIGHT = 0.30
    const val REGULARITY_WEIGHT = 0.35
    const val COMPLETENESS_WEIGHT = 0.10
    const val MAX_CONFIDENCE = 0.95
    const val ALIGN_PENALTY_MAX = 0.20
    const val ALIGN_SCALE = 1200.0

    /** min(1, n / 14)：样本饱和因子（calibration parameter，非统计充分性） */
    fun sampleFactor(sampleCount: Int): Double =
        min(1.0, sampleCount.toDouble() / SAMPLE_SATURATION)

    /** 0.20 + 0.30·sampleFactor + 0.35·R + 0.10·completeness */
    fun baseConfidence(sampleCount: Int, regularity: Double, completeness: Double): Double =
        BASE_MIN + SAMPLE_WEIGHT * sampleFactor(sampleCount) +
            REGULARITY_WEIGHT * regularity + COMPLETENESS_WEIGHT * completeness

    /**
     * §12.1：alignPenalty = min(0.20, circDist(μ_C, targetSleepTime) / 1200)。
     * 目标作息或 μ_C 缺失 → 0（安全降级）。
     */
    fun alignPenalty(meanOnsetMin: Int?, targetSleepTimeMin: Int?): Double {
        if (meanOnsetMin == null || targetSleepTimeMin == null) return 0.0
        val deviation = circularDistanceMinutes(meanOnsetMin, targetSleepTimeMin)
        return min(ALIGN_PENALTY_MAX, deviation / ALIGN_SCALE)
    }

    /** §4.6：conf = clamp(base − alignPenalty, 0, 0.95)；R undefined → null。 */
    fun compute(
        sampleCount: Int,
        regularity: Double?,
        completeness: Double,
        meanOnsetMin: Int?,
        targetSleepTimeMin: Int?,
    ): Double? {
        if (regularity == null) return null
        val base = baseConfidence(sampleCount, regularity, completeness)
        return (base - alignPenalty(meanOnsetMin, targetSleepTimeMin)).coerceIn(0.0, MAX_CONFIDENCE)
    }

    /** 环形距离 min(|a−b|, 1440−|a−b|)。本地纯实现，避免把 K-3 依赖扩散到 Confidence。 */
    private fun circularDistanceMinutes(a: Int, b: Int): Int {
        val diff = abs(a - b)
        return min(diff, PersonalityConstants.MINUTES_PER_DAY - diff)
    }
}
