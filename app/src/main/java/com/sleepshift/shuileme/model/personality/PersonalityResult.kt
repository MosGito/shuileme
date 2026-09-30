package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §20.1 OUTPUT / ADR-004：Personality Domain 唯一对外结果契约。
 *
 * - 只描述**人格判定结果**；禁止混入 Moon / Bubble / UI / Social / Clock 字段；
 * - [boundary] 表示 Boundary / Insufficiently Classified（不是人格定义）；
 * - [insufficientData] 表示 effectiveSessions < 5（冷启动 / 数据不足，Matcher 不执行判定），
 *   **不等于** Boundary，更不等于任何 PersonalityId；
 * - [confidence] 为 null 表示 R undefined（不产生正式 Confidence，禁止默认值）。
 */
data class PersonalityResult(
    /** 最终人格（Base 或 Special Gate）；Boundary / 数据不足时为 null */
    val primary: PersonalityDefinition?,
    /** S_primary（仅 Base Primary 非空；Gate / Boundary 为 null） */
    val primaryScore: Double?,
    /** 相邻人格中得分最高的有效候选；无有效候选为 null */
    val secondary: PersonalityDefinition?,
    /** S_secondary */
    val secondaryScore: Double?,
    /** ClassificationMargin = Top-1 − Top-2 eligible adjacent；仅 Base Primary 且有 Secondary 时非空 */
    val margin: Double?,
    /** margin < δ（0.15）；Gate Primary / Boundary / 无 Secondary 时恒为 false */
    val isTransitioning: Boolean,
    /** Boundary / Insufficiently Classified */
    val boundary: Boolean,
    /** 触发的 Special Gate（WOLF / CHAMELEON）；Base 时为 null */
    val gate: PersonalityId?,
    /** V2.0.7 §4.6 Confidence；R undefined → null */
    val confidence: Double?,
    /** 诊断：Chronotype 状态（含 undefined 原因） */
    val chronotype: ChronotypeState,
    /** 诊断：daytimeOnset 三态 */
    val daytimeOnset: DaytimeOnsetState,
    /** effectiveSessions */
    val effectiveSessionCount: Int,
    /** effectiveSessions < 5 → 冷启动 / 数据不足，未执行判定 */
    val insufficientData: Boolean,
) {
    /** 是否存在正式判定结果（Base 或 Gate） */
    val isClassified: Boolean get() = primary != null
}
