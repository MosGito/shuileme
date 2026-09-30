package com.sleepshift.shuileme.model.personality

import kotlin.math.abs

/**
 * 单个 Base 定义在给定特征下的 Membership Score。
 *
 * Membership Score 是 **deterministic membership measure**（§5.1）：
 * 不是 probability、confidence、classification accuracy、百分比概率；
 * 不做归一化、不做 softmax / sigmoid。
 */
internal data class MembershipScore(
    val id: PersonalityId,
    val score: Double,
)

/**
 * V2.0.7 §5.1 Membership 数学层（纯函数，无 Android / 时间 / 随机依赖）。
 *
 * - `S_i = mC × mD × mR`，仅对 10 个 Base Definitions 计算；
 * - CHAMELEON / WOLF 是 Special Gate，**不参与** Membership scoring；
 * - C / R undefined 时以 null（不可用）表达，**禁止** `?: 0.0` 之类隐式 fallback。
 */
internal object MembershipMath {

    private const val SLOTH_RAMP_UPPER = 0.7
    private const val OTTER_RAMP_LOWER = 0.5
    private const val RAMP_WIDTH = 0.1

    /**
     * §5.1：`mAxis(x; c, w) = max(0, 1 − |x − c| / w)`，默认 `w = 2/3`。
     * - x == c → 1.0；|x − c| == 2/3 → 0.0；超出 → 0.0；恒非负。
     */
    fun mAxis(
        x: Double,
        center: Double,
        width: Double = PersonalityConstants.AXIS_HALF_WIDTH,
    ): Double = (1.0 - abs(x - center) / width).coerceAtLeast(0.0)

    /**
     * §5.1：`mC = mAxis(C; centerC, 2/3)`。
     *
     * 只有 [ChronotypeState.Value] 能产生 C score；
     * OUT_OF_DOMAIN / MEAN_UNDEFINED → null（不可用），**不得**强制变成 C=0，
     * 不使用"最近中心"fallback，不使用算术平均替代 Circular Mean。
     */
    fun mC(chronotype: ChronotypeState, centerC: Double): Double? =
        if (chronotype is ChronotypeState.Value) mAxis(chronotype.score, centerC) else null

    /** §5.1：`mD = mAxis(D; centerD, 2/3)`；D 严格来自 [durationScore]（仅 μ_D，无派生变量）。 */
    fun mD(d: Double, centerD: Double): Double = mAxis(d, centerD)

    /**
     * §5.1：R 分量。
     *
     * - 无 R 约束的普通 Base → 恒 1.0（不依赖 R）；
     * - SLOTH / OTTER → **唯一评分机制**是连续 ramp（不是 "R<0.6 / R≥0.6" 硬阈值）：
     *   `SLOTH = clamp((0.7 − R) / 0.1, 0, 1)`；`OTTER = clamp((R − 0.5) / 0.1, 0, 1)`；
     * - 若定义依赖 R 而 R 为 null（undefined）→ 返回 null（不可用），**禁止**置 0。
     */
    fun mR(definition: PersonalityDefinition, r: Double?): Double? = when (definition.id) {
        PersonalityId.SLOTH -> r?.let { clamp((SLOTH_RAMP_UPPER - it) / RAMP_WIDTH) }
        PersonalityId.OTTER -> r?.let { clamp((it - OTTER_RAMP_LOWER) / RAMP_WIDTH) }
        else -> 1.0
    }

    /**
     * §5.1：`S_i = mC × mD × mR`。
     *
     * 任一必要分量不可用（C undefined / D 缺失 / R 约束但 undefined）→ 返回 null，
     * 由上层 Matcher 明确识别为"该 Score 不可用"；本层不提供数值 fallback。
     *
     * Special Gate（CHAMELEON / WOLF）直接返回 null（不参与 scoring）。
     */
    fun score(
        definition: PersonalityDefinition,
        chronotype: ChronotypeState,
        d: Double?,
        r: Double?,
    ): MembershipScore? {
        if (definition.isSpecialGate) return null
        val c = chronotype as? ChronotypeState.Value ?: return null
        val dValue = d ?: return null
        val centerC = definition.centerC ?: return null
        val centerD = definition.centerD ?: return null
        val mc = mC(c, centerC) ?: return null
        val md = mD(dValue, centerD)
        val mr = mR(definition, r) ?: return null
        return MembershipScore(definition.id, mc * md * mr)
    }

    /** 对 10 个 Base Definitions 评分（跳过不可用的 Score；不含任何 Gate）。 */
    fun scoreAllBase(
        chronotype: ChronotypeState,
        d: Double?,
        r: Double?,
    ): List<MembershipScore> = PersonalityRegistry.baseDefinitions.mapNotNull { score(it, chronotype, d, r) }

    /**
     * §5.3 deterministic tie-break（exact tie，无 epsilon）：
     *
     * 1. Membership Score 高者胜；
     * 2. 仅 Score 精确相等时，`priority` 高者胜（priority 不得覆盖更高 Score）；
     * 3. 仅 Score 与 priority 都精确相等时，stable ID（`PersonalityId.name`）字典序小者胜。
     *
     * 结果与输入列表顺序无关（使用全序 comparator，不依赖任何集合遍历顺序）。
     */
    fun deterministicArgmax(scores: List<MembershipScore>): MembershipScore? {
        if (scores.isEmpty()) return null
        return scores.maxWith(comparator)
    }

    private val comparator = Comparator<MembershipScore> { a, b ->
        val byScore = a.score.compareTo(b.score)
        if (byScore != 0) byScore
        else {
            val byPriority = priorityOf(a.id).compareTo(priorityOf(b.id))
            if (byPriority != 0) byPriority
            else b.id.name.compareTo(a.id.name) // 字典序小者胜：a 胜 ⇔ a.name < b.name
        }
    }

    private fun priorityOf(id: PersonalityId): Int = PersonalityRegistry.definition(id).priority

    private fun clamp(v: Double): Double = v.coerceIn(0.0, 1.0)
}
