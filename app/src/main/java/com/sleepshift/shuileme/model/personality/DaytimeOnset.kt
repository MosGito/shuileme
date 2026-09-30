package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §4.8：daytimeOnset 三态。
 *
 * - [TRUE]：ratio ≥ 0.60 且 effectiveSessions ≥ 5；
 * - [FALSE]：ratio < 0.60 且 effectiveSessions ≥ 5；
 * - [UNKNOWN]：effectiveSessions < 5（INSUFFICIENT：证据不足）。
 *
 * **UNKNOWN ≠ FALSE**：数据不足不得被解释为"确定非白天作息"，
 * 也不得触发 CHAMELEON Gate。
 */
enum class DaytimeOnsetState { TRUE, FALSE, UNKNOWN }

/**
 * V2.0.7 §4.8：daytimeOnsetRatio =
 * 落在日间窗口 [09:00, 18:00)（分钟 [540, 1080)）的有效 onset 数量 / 有效 onset 总数量。
 *
 * 分母为有效 onset（具备有效 `sleepStartAtMs`，= effectiveSessions），不含 invalid / missing onset。
 */
internal fun daytimeOnsetRatio(onsetMinutes: List<Int>): Double {
    if (onsetMinutes.isEmpty()) return 0.0
    val dayCount = onsetMinutes.count {
        it in PersonalityConstants.DAY_WINDOW_START_MIN until PersonalityConstants.DAY_WINDOW_END_MIN
    }
    return dayCount.toDouble() / onsetMinutes.size
}

/**
 * V2.0.7 §4.8：daytimeOnset 三态判定。
 *
 * - `effectiveSessionCount < 5` → [DaytimeOnsetState.UNKNOWN]；
 * - `effectiveSessionCount ≥ 5` 且 `ratio ≥ 0.60`（**含等于**）→ [DaytimeOnsetState.TRUE]；
 * - 否则 → [DaytimeOnsetState.FALSE]。
 *
 * `5` 与 `0.60` 是 calibration / design parameters（minimum evidence threshold），
 * 不是统计显著性、不是概率、不是 Confidence。
 */
internal fun daytimeOnsetState(ratio: Double, effectiveSessionCount: Int): DaytimeOnsetState = when {
    effectiveSessionCount < PersonalityConstants.MIN_EFFECTIVE_SESSIONS -> DaytimeOnsetState.UNKNOWN
    ratio >= PersonalityConstants.DAYTIME_ONSET_RATIO_THRESHOLD -> DaytimeOnsetState.TRUE
    else -> DaytimeOnsetState.FALSE
}
