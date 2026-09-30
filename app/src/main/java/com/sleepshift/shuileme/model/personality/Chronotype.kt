package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §4.3 / §4.9：ChronotypeScore 状态。
 *
 * - [Value]：μ_C 位于夜间弧内，`C = clamp(s / 240, −1, +1)`；
 * - [OutOfDomain]：μ_C 统计可定义但落在日间窗口 [09:00, 18:00)
 *   （CHRONOTYPE_OUT_OF_DOMAIN）——统计结果存在但位于 C 语义定义域之外；
 * - [MeanUndefined]：circular mean 退化（合成向量长度 < 1e-6）
 *   （CHRONOTYPE_MEAN_UNDEFINED）——统计量本身不可定义。
 *
 * 禁止：用 `Double.NaN` 表示 undefined；禁止把 undefined C 强制转换成 0；
 * 禁止选择最近人格中心；禁止任意 fallback。
 */
sealed interface ChronotypeState {
    data class Value(val score: Double) : ChronotypeState
    data object OutOfDomain : ChronotypeState
    data object MeanUndefined : ChronotypeState
}

/**
 * V2.0.7 §4.3：夜间弧 = [18:00, 24:00) ∪ [00:00, 09:00)。
 *
 * 注意这是**两个区间的并集**（分钟 [1080, 1440) ∪ [0, 540)），
 * 不是 `[18:00, 09:00)` 这种易被误读为空区间的写法。
 */
internal fun isInNightArc(minuteOfDay: Int): Boolean =
    minuteOfDay in 0 until PersonalityConstants.NIGHT_ARC_END_MIN ||
        minuteOfDay in PersonalityConstants.NIGHT_ARC_START_MIN until PersonalityConstants.MINUTES_PER_DAY

/**
 * V2.0.7 §4.3：ChronotypeScore。
 *
 * 仅允许对**夜间弧内**的 μ_C 求 C：
 *
 * ```
 * s = μ_C；若 s > 720，则 s = s − 1440
 * C = clamp(s / 240, −1, +1)
 * ```
 *
 * - 日间窗口 [09:00, 18:00) → [OutOfDomain]（禁止强制映射为 Early / Late / C=0）；
 * - μ_C 为 null（circular mean 退化）→ [MeanUndefined]。
 *
 * 该映射在夜间弧上连续：12:00 断裂点位于日间窗口之外、不在 C 定义域内。
 */
internal fun chronotypeScore(meanOnsetMin: Int?): ChronotypeState {
    if (meanOnsetMin == null) return ChronotypeState.MeanUndefined
    if (!isInNightArc(meanOnsetMin)) return ChronotypeState.OutOfDomain
    var s = meanOnsetMin
    if (s > 720) s -= 1440
    val c = (s / PersonalityConstants.CHRONOTYPE_SCALE_MIN).coerceIn(-1.0, 1.0)
    return ChronotypeState.Value(c)
}

/**
 * V2.0.7 §4.4：DurationScore = `clamp((μ_D − 480) / 120, −1, +1)`。
 *
 * 仅使用 μ_D（时长算术平均，非环形变量）；**不得**混入 median、ratio、离差等派生变量。
 */
internal fun durationScore(meanDurationMin: Long): Double =
    ((meanDurationMin - PersonalityConstants.DURATION_REFERENCE_MIN) / PersonalityConstants.DURATION_SCALE_MIN)
        .coerceIn(-1.0, 1.0)
