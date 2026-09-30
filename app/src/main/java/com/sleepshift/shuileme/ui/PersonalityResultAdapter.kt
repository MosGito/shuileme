package com.sleepshift.shuileme.ui

import com.sleepshift.shuileme.model.PersonalityMetrics
import com.sleepshift.shuileme.model.SleepPersonalityState
import com.sleepshift.shuileme.model.SleepPersonalityType
import com.sleepshift.shuileme.model.personality.PersonalityId
import com.sleepshift.shuileme.model.personality.PersonalityResult
import com.sleepshift.shuileme.model.personality.SleepBehaviorProfile

/**
 * Personality Domain → 旧展示结构的**最小兼容适配层**（仅展示用途）。
 *
 * - 人格判定**唯一**来自 [PersonalityResult]（V2.0.7 §20.1 OUTPUT / ADR-004）；
 * - 本层**不**重新实现 Membership / Gate / Confidence / Chronotype 算法，
 *   也不调用旧 SleepPersonalityEngine；
 * - [legacyType] 是 12 定义体系 → 旧 8 值展示枚举的**展示投影**：旧 UI 的
 *   HomeScreen / Card / Talk / Bubble 只消费 SleepPersonalityType，因此新判定
 *   结果需要投影到旧枚举才能继续显示；权威身份始终是 [PersonalityId]，
 *   投影只影响展示文案/emoji，不影响判定；
 * - [legacyState] 仅把新结果 + 特征画像映射为旧 [SleepPersonalityState]，
 *   保留 primaryType / confidence / metrics 三项旧消费方实际使用的内容；
 *   residents / unlockedHidden / evolutionProgress 当前无线上消费者，取空默认值。
 */
object PersonalityResultAdapter {

    /** 新 PersonalityId → 旧展示枚举（兼容投影；一对一，不允许 null）。 */
    fun legacyType(id: PersonalityId): SleepPersonalityType = when (id) {
        // 早睡短眠 → 早起鸟展示语义
        PersonalityId.ROOSTER -> SleepPersonalityType.EARLY_BIRD
        PersonalityId.EARLY_BIRD -> SleepPersonalityType.EARLY_BIRD
        // 早睡长眠 / 规律健康长睡 → 海獭（安睡）展示语义
        PersonalityId.BEAR -> SleepPersonalityType.OTTER
        PersonalityId.HOUND -> SleepPersonalityType.OTTER
        // 短眠 → 牛马展示语义
        PersonalityId.WORK_HORSE -> SleepPersonalityType.WORK_HORSE
        // 不规律长睡 / 不规则 → 混沌展示语义
        PersonalityId.SLOTH -> SleepPersonalityType.CHAOS
        PersonalityId.CHAMELEON -> SleepPersonalityType.CHAOS
        PersonalityId.OTTER -> SleepPersonalityType.OTTER
        // 晚睡 / 昼伏 → 夜猫子 / 夜行幽灵展示语义
        PersonalityId.BAT -> SleepPersonalityType.NIGHT_OWL
        PersonalityId.NIGHT_OWL -> SleepPersonalityType.NIGHT_OWL
        PersonalityId.OWL -> SleepPersonalityType.NIGHT_GHOST
        PersonalityId.WOLF -> SleepPersonalityType.NIGHT_GHOST
    }

    /**
     * [PersonalityResult] + [SleepBehaviorProfile] → 旧 [SleepPersonalityState]。
     *
     * - primaryType：仅当新结果存在正式 Primary 时非空（Boundary /
     *   insufficientData → null，UI 按旧逻辑回退冷启动占位）；
     * - confidence：仅展示兼容（R undefined → 0.0，旧 UI 无实际消费）；
     * - metrics：直接透传特征画像统计（新 Domain 产出，非重新计算）。
     */
    fun legacyState(
        result: PersonalityResult,
        profile: SleepBehaviorProfile,
    ): SleepPersonalityState = SleepPersonalityState(
        primaryType = result.primary?.id?.let(::legacyType),
        confidence = result.confidence ?: 0.0,
        evolutionProgress = 0.0,
        residents = emptyList(),
        unlockedHidden = emptySet(),
        metrics = PersonalityMetrics(
            avgSleepTimeMin = profile.meanOnsetMin,
            avgWakeTimeMin = profile.meanWakeMin,
            avgDurationMin = profile.meanDurationMin,
            regularityScore = profile.regularity ?: 0.0,
        ),
    )
}
