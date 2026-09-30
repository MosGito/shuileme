package com.sleepshift.shuileme.model.personality

import com.sleepshift.shuileme.model.SleepPersonalityEngine
import com.sleepshift.shuileme.model.SleepSession
import java.time.Instant
import java.time.ZoneId
import kotlin.math.sqrt

/**
 * V2.0.7 §4：特征层聚合输出（SleepBehaviorProfile）。
 *
 * 仅承载特征统计（μ_C / μ_W / μ_D / σ 系列 / R / completeness / chronotype / daytimeOnset），
 * **不含** Membership、Confidence 或任何人格判定。
 *
 * `regularity: Double?`：null 表示 R = undefined（§4.5 / §4.9），
 * 由 Regularity 自身输入质量决定，**不得因 C undefined 自动置 0**。
 */
data class SleepBehaviorProfile(
    /** effectiveSessions（具备有效 sleepStartAtMs 的会话数） */
    val effectiveSessionCount: Int,
    /** μ_C：入睡环形平均；null = circular mean 退化 */
    val meanOnsetMin: Int?,
    /** μ_W：起床环形平均；null = 无 wake 样本或退化 */
    val meanWakeMin: Int?,
    /** μ_D：时长算术平均；null = 无时长样本 */
    val meanDurationMin: Long?,
    /** σ_circ_onset（分钟）；null = 不可计算 */
    val onsetStdDevMin: Double?,
    /** σ_circ_wake（分钟）；null = 不可计算 */
    val wakeStdDevMin: Double?,
    /** σ_dur（分钟）；null = 不可计算 */
    val durationStdDevMin: Double?,
    /** R ∈ [0,1]；null = undefined（§4.5 / §4.9） */
    val regularity: Double?,
    /** completeness = 含 wakeAtMs 的会话数 / effectiveSessions（§4.6） */
    val completeness: Double,
    /** ChronotypeScore 状态（§4.3 / §4.9） */
    val chronotype: ChronotypeState,
    /** daytimeOnsetRatio（§4.8） */
    val daytimeOnsetRatio: Double,
    /** daytimeOnset 三态（§4.8） */
    val daytimeOnset: DaytimeOnsetState,
) {
    companion object {

        /** V2.0.7 §11.1：effectiveSessions = 具备有效 sleepStartAtMs 的会话数（= 有效 onset 会话数）。 */
        fun effectiveSessions(sessions: List<SleepSession>): Int = sessions.size

        /** 由 SleepSession 列表构建特征层聚合（纯函数，无 Android 依赖）。 */
        fun from(sessions: List<SleepSession>): SleepBehaviorProfile {
            val n = effectiveSessions(sessions)
            val starts = sessions.map { minutesOfDay(it.sleepStartAtMs) }
            val wakes = sessions.mapNotNull { it.wakeAtMs?.let(::minutesOfDay) }
            val durations = sessions.mapNotNull { it.durationMin }

            val muOnset = SleepPersonalityEngine.circularMeanMinutes(starts)
            val muWake = SleepPersonalityEngine.circularMeanMinutes(wakes)
            val muDuration = if (durations.isNotEmpty()) durations.average().toLong() else null

            val sigmaOnset = muOnset?.let { circularStdDev(starts, it) }
            val sigmaWake = muWake?.let { circularStdDev(wakes, it) }
            val sigmaDuration = muDuration?.let { plainStdDev(durations.map { it.toDouble() }, it.toDouble()) }

            val rOnset = sigmaOnset?.let { regularityComponent(it, PersonalityConstants.ONSET_STDDEV_THRESHOLD_MIN) }
            val rWake = sigmaWake?.let { regularityComponent(it, PersonalityConstants.WAKE_STDDEV_THRESHOLD_MIN) }
            val rDuration = sigmaDuration?.let { regularityComponent(it, PersonalityConstants.DURATION_STDDEV_THRESHOLD_MIN) }

            // §4.5：R = 0.5·r_onset + 0.3·r_wake + 0.2·r_dur
            // 无 wake：R = r_onset
            // σ_onset 不可计算（rOnset == null）→ R = undefined（禁止置 0）
            // wake 存在但分量不足（rWake / rDuration 为 null）→ 三分量公式不可完整计算 → R = undefined
            val regularity = when {
                rOnset == null -> null
                wakes.isEmpty() -> rOnset
                rWake == null || rDuration == null -> null
                else -> 0.5 * rOnset + 0.3 * rWake + 0.2 * rDuration
            }

            val ratio = daytimeOnsetRatio(starts)
            return SleepBehaviorProfile(
                effectiveSessionCount = n,
                meanOnsetMin = muOnset,
                meanWakeMin = muWake,
                meanDurationMin = muDuration,
                onsetStdDevMin = sigmaOnset,
                wakeStdDevMin = sigmaWake,
                durationStdDevMin = sigmaDuration,
                regularity = regularity,
                completeness = if (n == 0) 0.0 else wakes.size.toDouble() / n,
                chronotype = chronotypeScore(muOnset),
                daytimeOnsetRatio = ratio,
                daytimeOnset = daytimeOnsetState(ratio, n),
            )
        }
    }
}

/** 环形标准差：各样本到环形均值的 circularDistance 的均方根（与 STEP 2 口径一致）。样本 < 2 时不可计算 → null。 */
private fun circularStdDev(values: List<Int>, mean: Int): Double? {
    if (values.size < 2) return null
    val meanSquared = values.map { d ->
        val diff = SleepPersonalityEngine.circularDistance(d, mean).toDouble()
        diff * diff
    }.average()
    return sqrt(meanSquared)
}

/** 普通标准差（时长非环形变量）。样本 < 2 时不可计算 → null。 */
private fun plainStdDev(values: List<Double>, mean: Double): Double? {
    if (values.size < 2) return null
    return sqrt(values.map { (it - mean) * (it - mean) }.average())
}

/** §4.5 分量：r = 1 − min(1, σ / 阈值)，∈ [0, 1]。 */
private fun regularityComponent(stdDevMin: Double, thresholdMin: Double): Double =
    (1.0 - kotlin.math.min(1.0, stdDevMin / thresholdMin)).coerceIn(0.0, 1.0)

private fun minutesOfDay(ms: Long): Int {
    val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return zdt.hour * 60 + zdt.minute
}
