package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §20.1 唯一执行顺序的判定层（control-flow，非 score competition）。
 *
 * ```
 * profile
 *   → effectiveSessions ≥ 5（否则冷启动 / 数据不足，不执行判定）
 *   → STEP 1 WOLF Gate（daytimeOnset == TRUE）
 *   → STEP 2 CHAMELEON Gate（daytimeOnset == FALSE 且 R < 0.33）
 *   → UNKNOWN：不触发任何 Gate（UNKNOWN ≠ FALSE）
 *   → STEP 3 Base Classification：
 *       C undefined → Boundary（禁止 C=0 / 最近中心 fallback）
 *       D 缺失 → Boundary（无法形成完整 Membership）
 *       scoreAllBase → deterministicArgmax → S_primary < 0.25 → Boundary
 *   → Secondary（仅 transitionTargets ∩ S ≥ 0.25，统一 tie-break）
 *   → ClassificationMargin / isTransitioning（Gate / Boundary / 无 Secondary → null / false）
 *   → Confidence（R undefined → null）
 * ```
 *
 * 本对象只消费 [SleepBehaviorProfile]（Personality Domain 已定义输入契约）；
 * 不依赖 ViewModel / Repository / UI / Bubble / Moon / Clock / Social。
 */
object PersonalityMatcher {

    /**
     * 执行完整分类。
     *
     * @param profile Sleep Analysis 产出的特征画像
     * @param targetSleepTimeMin 可选目标入睡时间（仅用于 Confidence 的 alignPenalty；缺失安全降级为 0）
     */
    fun classify(
        profile: SleepBehaviorProfile,
        targetSleepTimeMin: Int? = null,
    ): PersonalityResult {
        val n = profile.effectiveSessionCount
        if (n < PersonalityConstants.MIN_EFFECTIVE_SESSIONS) {
            return PersonalityResult(
                primary = null, primaryScore = null, secondary = null, secondaryScore = null,
                margin = null, isTransitioning = false, boundary = false, gate = null,
                confidence = null, chronotype = profile.chronotype, daytimeOnset = profile.daytimeOnset,
                effectiveSessionCount = n, insufficientData = true,
            )
        }

        return when (profile.daytimeOnset) {
            DaytimeOnsetState.TRUE -> gateResult(PersonalityId.WOLF, profile, targetSleepTimeMin)
            DaytimeOnsetState.FALSE -> {
                val r = profile.regularity
                if (r != null && r < PersonalityConstants.CHAMELEON_R_THRESHOLD) {
                    gateResult(PersonalityId.CHAMELEON, profile, targetSleepTimeMin)
                } else {
                    baseResult(profile, targetSleepTimeMin)
                }
            }
            // UNKNOWN：不触发任何 Special Gate（UNKNOWN ≠ FALSE），进入 Base
            DaytimeOnsetState.UNKNOWN -> baseResult(profile, targetSleepTimeMin)
        }
    }

    // ── STEP 3：Base Classification ──

    private fun baseResult(profile: SleepBehaviorProfile, targetSleepTimeMin: Int?): PersonalityResult {
        val chronotype = profile.chronotype
        if (chronotype !is ChronotypeState.Value) {
            // C axis unavailable（CHRONOTYPE_OUT_OF_DOMAIN / CHRONOTYPE_MEAN_UNDEFINED）→ Boundary
            return boundaryResult(profile, targetSleepTimeMin)
        }
        val d = profile.meanDurationMin?.let(::durationScore)
        if (d == null) {
            // D 缺失 → 无法形成完整 Membership → Boundary
            return boundaryResult(profile, targetSleepTimeMin)
        }

        val scores = MembershipMath.scoreAllBase(chronotype, d, profile.regularity)
        val top = MembershipMath.deterministicArgmax(scores)
            ?: return boundaryResult(profile, targetSleepTimeMin)
        if (top.score < PersonalityConstants.MIN_MEMBERSHIP_SCORE) {
            return boundaryResult(profile, targetSleepTimeMin)
        }

        val primaryDef = PersonalityRegistry.definition(top.id)
        val secondary = selectSecondary(primaryDef, scores)
        val secondaryScore = secondary?.score
        val margin = secondaryScore?.let { top.score - it }
        val isTransitioning = isTransitioning(margin)

        return PersonalityResult(
            primary = primaryDef, primaryScore = top.score,
            secondary = secondary?.let { PersonalityRegistry.definition(it.id) },
            secondaryScore = secondaryScore,
            margin = margin, isTransitioning = isTransitioning, boundary = false, gate = null,
            confidence = confidenceOf(profile, targetSleepTimeMin),
            chronotype = chronotype, daytimeOnset = profile.daytimeOnset,
            effectiveSessionCount = profile.effectiveSessionCount, insufficientData = false,
        )
    }

    // ── STEP 1 / STEP 2：Special Gate（control-flow，无 Membership Score）──

    private fun gateResult(gateId: PersonalityId, profile: SleepBehaviorProfile, targetSleepTimeMin: Int?): PersonalityResult {
        val gateDef = PersonalityRegistry.definition(gateId)
        // §10.7：Gate Primary 的 Secondary 仍取自 transitionTargets（基于 Base scores，eligibility ≥ 0.25）
        val secondary = gateSecondary(gateDef, profile)
        return PersonalityResult(
            primary = gateDef, primaryScore = null, gate = gateId,
            secondary = secondary?.first, secondaryScore = secondary?.second,
            margin = null, isTransitioning = false, boundary = false,
            confidence = confidenceOf(profile, targetSleepTimeMin),
            chronotype = profile.chronotype, daytimeOnset = profile.daytimeOnset,
            effectiveSessionCount = profile.effectiveSessionCount, insufficientData = false,
        )
    }

    private fun gateSecondary(gateDef: PersonalityDefinition, profile: SleepBehaviorProfile): Pair<PersonalityDefinition, Double>? {
        val chronotype = profile.chronotype as? ChronotypeState.Value ?: return null
        val d = profile.meanDurationMin?.let(::durationScore) ?: return null
        val scores = MembershipMath.scoreAllBase(chronotype, d, profile.regularity)
        val best = selectSecondary(gateDef, scores) ?: return null
        return PersonalityRegistry.definition(best.id) to best.score
    }

    // ── Secondary / Boundary / Confidence ──

    /** §5.3：Secondary = Primary 的 transitionTargets 中 S ≥ 0.25 且得分最高的候选（统一 tie-break）。 */
    private fun selectSecondary(primary: PersonalityDefinition, scores: List<MembershipScore>): MembershipScore? {
        val byId = scores.associateBy { it.id }
        return MembershipMath.deterministicArgmax(
            primary.transitionTargets
                .mapNotNull { byId[it] }
                .filter { it.score >= PersonalityConstants.MIN_MEMBERSHIP_SCORE },
        )
    }

    private fun boundaryResult(profile: SleepBehaviorProfile, targetSleepTimeMin: Int?): PersonalityResult =
        PersonalityResult(
            primary = null, primaryScore = null, secondary = null, secondaryScore = null,
            margin = null, isTransitioning = false, boundary = true, gate = null,
            confidence = confidenceOf(profile, targetSleepTimeMin),
            chronotype = profile.chronotype, daytimeOnset = profile.daytimeOnset,
            effectiveSessionCount = profile.effectiveSessionCount, insufficientData = false,
        )

    private fun confidenceOf(profile: SleepBehaviorProfile, targetSleepTimeMin: Int?): Double? =
        Confidence.compute(
            sampleCount = profile.effectiveSessionCount,
            regularity = profile.regularity,
            completeness = profile.completeness,
            meanOnsetMin = profile.meanOnsetMin,
            targetSleepTimeMin = targetSleepTimeMin,
        )

    /**
     * §10.2：isTransitioning = ClassificationMargin < δ（0.15）。
     * 独立为 internal 纯函数，便于对 0.15 边界做精确比较测试（禁止 epsilon）。
     * margin == null（Gate / Boundary / 无 Secondary）→ false。
     */
    internal fun isTransitioning(margin: Double?): Boolean =
        margin != null && margin < PersonalityConstants.TRANSITION_MARGIN_THRESHOLD
}
