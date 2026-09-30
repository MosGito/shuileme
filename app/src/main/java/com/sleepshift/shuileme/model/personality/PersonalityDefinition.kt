package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §15：第一阶段 12 个 Primary Personality ID。
 *
 * stable ID = `enum.name`（字典序，用于 deterministic tie-break 第三优先级）。
 */
enum class PersonalityId {
    ROOSTER,
    EARLY_BIRD,
    BEAR,
    WORK_HORSE,
    HOUND,
    SLOTH,
    OTTER,
    BAT,
    NIGHT_OWL,
    OWL,
    CHAMELEON,
    WOLF,
}

/**
 * V2.0.7 §7 / §15 / §22：人格定义（声明式、纯数据）。
 *
 * - 分类只使用 [centerC] / [centerD] / [regularityRange] 与 Membership 数学（§5.1）；
 * - 表现字段（name / animal / modifier / emojis / description / culturalMeaning / explanationTemplate）
 *   仅用于展示与解释，不参与分类；
 * - CHAMELEON / WOLF 为 Special Gate（[isSpecialGate] = true，center 与 region 为 null），
 *   不是普通 C×D Region、不是 Membership candidate；
 * - Boundary **不是** PersonalityDefinition（不在此注册表内）。
 */
data class PersonalityDefinition(
    val id: PersonalityId,
    /** 中文正式名称 */
    val name: String,
    /** 动物基底 */
    val animal: String,
    /** 行为修饰词 */
    val modifier: String,
    /** 双 emoji（动物身份 + 行为/文化语义，§8.2） */
    val emojis: List<String>,
    /** 英文内部名称 */
    val englishName: String,
    /** 正式评分中心（C 轴）；Special Gate 为 null */
    val centerC: Double?,
    /** 正式评分中心（D 轴）；Special Gate 为 null */
    val centerD: Double?,
    /** C 轴语义区间（解释/邻接用）；Special Gate 为 null */
    val chronotypeRegion: String?,
    /** D 轴语义区间（解释/邻接用）；Special Gate 为 null */
    val durationRegion: String?,
    /** R 约束（名义语义，见 §5.1：仅 ramp 参与评分）；无约束为 null */
    val regularityRange: String?,
    /** §5.3 目录序 1–12；数值越大，精确同分时优先级越高；不得覆盖更高 Membership Score */
    val priority: Int,
    /** V2.0.7 minMembershipScore = 0.25（与 Confidence 分离） */
    val minMembershipScore: Double,
    /** 官方描述 / 用户解释 */
    val description: String,
    /** 文化 / 网络语义依据 */
    val culturalMeaning: String,
    /** 可解释性模板（§8.3） */
    val explanationTemplate: String,
    /** 相邻定义（Secondary 候选；Special-Gate 语义见 §10.7） */
    val transitionTargets: List<PersonalityId>,
    /** true = Special Gate（WOLF / CHAMELEON），非 C×D Region */
    val isSpecialGate: Boolean = false,
)
