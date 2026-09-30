package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 特征层常量。
 *
 * 所有阈值均为 design / calibration parameters（或 minimum evidence threshold），
 * **不是**统计显著性阈值、不是概率、不是 Confidence。
 */
internal object PersonalityConstants {

    /** 一天分钟数 */
    const val MINUTES_PER_DAY = 1440

    /** 夜间弧（C 有效定义域）[18:00, 24:00) ∪ [00:00, 09:00) */
    const val NIGHT_ARC_START_MIN = 1080 // 18:00
    const val NIGHT_ARC_END_MIN = 540    // 09:00（排他）

    /** 日间窗口 [09:00, 18:00)（WOLF / daytimeOnsetRatio 用） */
    const val DAY_WINDOW_START_MIN = 540  // 09:00
    const val DAY_WINDOW_END_MIN = 1080   // 18:00（排他）

    /** ChronotypeScore 尺度：C = clamp(s / 240, −1, +1) */
    const val CHRONOTYPE_SCALE_MIN = 240.0

    /** DurationScore 尺度：D = clamp((μ_D − 480) / 120, −1, +1) */
    const val DURATION_REFERENCE_MIN = 480.0
    const val DURATION_SCALE_MIN = 120.0

    /** Regularity 分量阈值（§4.5） */
    const val ONSET_STDDEV_THRESHOLD_MIN = 120.0
    const val WAKE_STDDEV_THRESHOLD_MIN = 120.0
    const val DURATION_STDDEV_THRESHOLD_MIN = 90.0

    /** daytimeOnset 三态判定（0.60 使用 ≥） */
    const val DAYTIME_ONSET_RATIO_THRESHOLD = 0.60

    /** Formal Personality / Special Gate 的 minimum evidence threshold（= effectiveSessions ≥ 5） */
    const val MIN_EFFECTIVE_SESSIONS = 5

    /** V2.0.7 §6.1 评分中心（centerC / centerD 三档） */
    const val CENTER_MIN = -2.0 / 3.0
    const val CENTER_NEUTRAL = 0.0
    const val CENTER_MAX = 2.0 / 3.0

    /** V2.0.7 §5.1 / §5.3：minMembershipScore = 0.25（最低有效 Membership 阈值，非概率/置信度） */
    const val MIN_MEMBERSHIP_SCORE = 0.25

    /** V2.0.7 §5.1：Membership 轴半宽 w = 2/3 */
    const val AXIS_HALF_WIDTH = 2.0 / 3.0

    /** V2.0.7 §5.2 / §6.2：CHAMELEON Gate 的 R 阈值（R < 0.33 才触发） */
    const val CHAMELEON_R_THRESHOLD = 0.33

    /** V2.0.7 §10.2：Transition Threshold δ = 0.15（ClassificationMargin < 0.15 → isTransitioning） */
    const val TRANSITION_MARGIN_THRESHOLD = 0.15
}
