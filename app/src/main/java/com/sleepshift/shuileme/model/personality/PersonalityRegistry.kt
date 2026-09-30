package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §7 / §22：第一阶段 12 个 PersonalityDefinition 注册表。
 *
 * - 10 个 Base Definitions（9 个 Base C×D Regions，其中 Neutral×Long 含 SLOTH / OTTER 两个 R-分裂定义）；
 * - 2 个 Special Gate Definitions（CHAMELEON / WOLF）。
 *
 * Boundary **不**在此注册表内（Boundary 是分类输出状态，不是人格定义）。
 * CHAOS 等旧类型不进入 Primary Registry。
 */
internal object PersonalityRegistry {

    /** 通用可解释性模板（§8.3）；实现时以实际数据填充占位符。 */
    const val EXPLANATION_TEMPLATE =
        "你通常在 {sleep} 入睡、{wake} 醒来、睡眠时长约 {duration} 小时，" +
            "并且这种作息保持了 {regularity} 的稳定性，因此被归为「{name}」。"

    private const val CENTER_MIN = PersonalityConstants.CENTER_MIN
    private const val CENTER_MAX = PersonalityConstants.CENTER_MAX

    val definitions: List<PersonalityDefinition> = listOf(
        PersonalityDefinition(
            id = PersonalityId.ROOSTER, name = "报晓鸡", animal = "公鸡", modifier = "报晓（黎明即鸣）",
            emojis = listOf("🐓", "⏰"), englishName = "ROOSTER",
            centerC = CENTER_MIN, centerD = CENTER_MIN,
            chronotypeRegion = "Early", durationRegion = "Short", regularityRange = null,
            priority = 1, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你通常在晚上 10 点半前入睡，但睡得不够久，像公鸡一样天没亮就醒。",
            culturalMeaning = "金鸡报晓；公鸡打鸣 = 早起闹钟",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.EARLY_BIRD, PersonalityId.WORK_HORSE),
        ),
        PersonalityDefinition(
            id = PersonalityId.EARLY_BIRD, name = "早起鸟", animal = "鸟", modifier = "早起",
            emojis = listOf("🐦", "🌅"), englishName = "EARLY_BIRD",
            centerC = CENTER_MIN, centerD = PersonalityConstants.CENTER_NEUTRAL,
            chronotypeRegion = "Early", durationRegion = "Medium", regularityRange = null,
            priority = 2, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你早睡早起，睡眠时长正常，符合'早起的鸟儿有虫吃'。",
            culturalMeaning = "早起的鸟儿有虫吃；早起鸟打卡",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.ROOSTER, PersonalityId.BEAR, PersonalityId.HOUND),
        ),
        PersonalityDefinition(
            id = PersonalityId.BEAR, name = "安眠熊", animal = "熊", modifier = "安眠（早睡 + 长眠）",
            emojis = listOf("🐻", "❄️"), englishName = "BEAR",
            centerC = CENTER_MIN, centerD = CENTER_MAX,
            chronotypeRegion = "Early", durationRegion = "Long", regularityRange = null,
            priority = 3, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你睡得很早也睡得很久，像进入冬眠的熊一样需要充足睡眠。",
            culturalMeaning = "冬眠网络梗；睡熊意象",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.EARLY_BIRD, PersonalityId.SLOTH, PersonalityId.OTTER),
        ),
        PersonalityDefinition(
            id = PersonalityId.WORK_HORSE, name = "牛马", animal = "牛/马", modifier = "高强度劳动（牺牲睡眠）",
            emojis = listOf("🐂", "💼"), englishName = "WORK_HORSE",
            centerC = PersonalityConstants.CENTER_NEUTRAL, centerD = CENTER_MIN,
            chronotypeRegion = "Neutral", durationRegion = "Short", regularityRange = null,
            priority = 4, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你的入睡时间不算极端，但睡眠时长明显不足，睡眠受到高负荷工作或生活节奏的挤压。",
            culturalMeaning = "网络流行语'牛马'；做牛做马",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.HOUND, PersonalityId.ROOSTER, PersonalityId.BAT),
        ),
        PersonalityDefinition(
            id = PersonalityId.HOUND, name = "守序犬", animal = "犬", modifier = "守序（准时、规律）",
            emojis = listOf("🐕", "⏰"), englishName = "HOUND",
            centerC = PersonalityConstants.CENTER_NEUTRAL, centerD = PersonalityConstants.CENTER_NEUTRAL,
            chronotypeRegion = "Neutral", durationRegion = "Medium", regularityRange = null,
            priority = 5, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你的作息时间与时长都处于常规范围，且相当稳定，像忠于时刻表的家犬。",
            culturalMeaning = "狗的时刻感；规律得像狗一样",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(
                PersonalityId.EARLY_BIRD, PersonalityId.NIGHT_OWL,
                PersonalityId.WORK_HORSE, PersonalityId.SLOTH, PersonalityId.OTTER,
            ),
        ),
        PersonalityDefinition(
            id = PersonalityId.SLOTH, name = "树懒", animal = "树懒", modifier = "长睡、节奏慢",
            emojis = listOf("🦥", "🛋️"), englishName = "SLOTH",
            centerC = PersonalityConstants.CENTER_NEUTRAL, centerD = CENTER_MAX,
            chronotypeRegion = "Neutral", durationRegion = "Long", regularityRange = "R<0.6（名义）",
            priority = 6, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你睡得很久但时间不太稳定，像树懒一样随时都能睡、也随时醒。",
            culturalMeaning = "树懒式生活；躺平",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.OTTER, PersonalityId.HOUND, PersonalityId.OWL),
        ),
        PersonalityDefinition(
            id = PersonalityId.OTTER, name = "安睡海獭", animal = "海獭", modifier = "安睡（长睡 + 高规律）",
            emojis = listOf("🦦", "🌊"), englishName = "OTTER",
            centerC = PersonalityConstants.CENTER_NEUTRAL, centerD = CENTER_MAX,
            chronotypeRegion = "Neutral", durationRegion = "Long", regularityRange = "R≥0.6（名义）",
            priority = 7, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你睡得久而且很规律，像海獭一样安稳地漂浮在自己的睡眠里。",
            culturalMeaning = "海獭手拉手漂浮睡眠的网络影像；睡得香",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.SLOTH, PersonalityId.HOUND, PersonalityId.OWL),
        ),
        PersonalityDefinition(
            id = PersonalityId.BAT, name = "夜蝙蝠", animal = "蝙蝠", modifier = "深夜活动、睡眠不足",
            emojis = listOf("🦇", "🌃"), englishName = "BAT",
            centerC = CENTER_MAX, centerD = CENTER_MIN,
            chronotypeRegion = "Late", durationRegion = "Short", regularityRange = null,
            priority = 8, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你通常凌晨 1 点后才睡，而且睡得很少，像昼伏夜出的蝙蝠。",
            culturalMeaning = "夜行动物；蝙蝠的夜间生态",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.NIGHT_OWL, PersonalityId.WORK_HORSE),
        ),
        PersonalityDefinition(
            id = PersonalityId.NIGHT_OWL, name = "夜猫子", animal = "猫", modifier = "深夜精神",
            emojis = listOf("🐱", "🌙"), englishName = "NIGHT_OWL",
            centerC = CENTER_MAX, centerD = PersonalityConstants.CENTER_NEUTRAL,
            chronotypeRegion = "Late", durationRegion = "Medium", regularityRange = null,
            priority = 9, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你通常在凌晨入睡，睡眠时长正常，属于典型的夜间型人格。",
            culturalMeaning = "夜猫子网络语；月亮的朋友，太阳的陌生猫",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.HOUND, PersonalityId.BAT, PersonalityId.OWL),
        ),
        PersonalityDefinition(
            id = PersonalityId.OWL, name = "昼眠枭", animal = "猫头鹰", modifier = "昼伏（天亮才睡）",
            emojis = listOf("🦉", "☀️"), englishName = "OWL",
            centerC = CENTER_MAX, centerD = CENTER_MAX,
            chronotypeRegion = "Late", durationRegion = "Long", regularityRange = null,
            priority = 10, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你凌晨很晚才睡但睡得够久，醒来时间通常已经进入白天或中午以后，像白天栖息、夜间捕食的猫头鹰。",
            culturalMeaning = "夜枭文学意象；昼伏夜出",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.NIGHT_OWL, PersonalityId.SLOTH, PersonalityId.OTTER),
        ),
        PersonalityDefinition(
            id = PersonalityId.CHAMELEON, name = "变色龙", animal = "变色龙", modifier = "随变（作息不稳定）",
            emojis = listOf("🦎", "🎲"), englishName = "CHAMELEON",
            centerC = null, centerD = null,
            chronotypeRegion = null, durationRegion = null, regularityRange = "R<0.33（gate）",
            priority = 11, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你的睡眠时间和时长变化较大，还没有形成稳定的睡眠节奏。",
            culturalMeaning = "作息不固定网络语；变色龙随环境变色（比喻作息多变）",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = PersonalityId.entries.filter { it != PersonalityId.CHAMELEON && it != PersonalityId.WOLF },
            isSpecialGate = true,
        ),
        PersonalityDefinition(
            id = PersonalityId.WOLF, name = "夜行狼", animal = "狼", modifier = "昼夜相位倒置（白天入睡、夜间活动）",
            emojis = listOf("🐺", "🌌"), englishName = "WOLF",
            centerC = null, centerD = null,
            chronotypeRegion = null, durationRegion = null, regularityRange = null,
            priority = 12, minMembershipScore = PersonalityConstants.MIN_MEMBERSHIP_SCORE,
            description = "你在白天入睡、夜间活动，属于昼夜倒置的夜行作息，像在夜间巡行的狼。",
            culturalMeaning = "狼性网络语；夜行狼的夜间活动意象",
            explanationTemplate = EXPLANATION_TEMPLATE,
            transitionTargets = listOf(PersonalityId.NIGHT_OWL, PersonalityId.OWL),
            isSpecialGate = true,
        ),
    )

    /** 10 个 Base Definitions（9 个 Base C×D Regions，Neutral×Long 含 SLOTH / OTTER） */
    val baseDefinitions: List<PersonalityDefinition> = definitions.filter { !it.isSpecialGate }

    /** 2 个 Special Gate Definitions（CHAMELEON / WOLF） */
    val specialGateDefinitions: List<PersonalityDefinition> = definitions.filter { it.isSpecialGate }

    private val byId: Map<PersonalityId, PersonalityDefinition> = definitions.associateBy { it.id }

    fun definition(id: PersonalityId): PersonalityDefinition = byId.getValue(id)

    init {
        check(definitions.size == 12) { "PersonalityRegistry 必须注册恰好 12 个定义，实际 ${definitions.size}" }
        check(byId.size == 12) { "PersonalityDefinition ID 必须唯一" }
        check(baseDefinitions.size == 10) { "必须恰好 10 个 Base Definitions" }
        check(specialGateDefinitions.size == 2) { "必须恰好 2 个 Special Gate Definitions" }
        check(definitions.map { it.priority }.distinct().size == definitions.size) { "priority 必须唯一" }
        check(definitions.all { it.minMembershipScore == PersonalityConstants.MIN_MEMBERSHIP_SCORE }) {
            "minMembershipScore 必须统一为 0.25"
        }
        // 普通 C×D Region 必须有评分中心；Special Gate 必须无中心（不是 C×D Region）
        check(baseDefinitions.all { it.centerC != null && it.centerD != null }) { "Base Definition 必须具有 centerC/centerD" }
        check(specialGateDefinitions.all { it.centerC == null && it.centerD == null }) {
            "Special Gate 不得具有 centerC/centerD（不是 C×D Region）"
        }
        // 邻接约束：Base 的 transitionTargets 必须 ⊆ Base；CHAMELEON = 全部 10 Base；WOLF = {NIGHT_OWL, OWL}
        val baseIds = baseDefinitions.map { it.id }.toSet()
        check(baseDefinitions.all { d -> d.transitionTargets.isNotEmpty() && d.transitionTargets.all { it in baseIds } }) {
            "Base Definition 的 transitionTargets 必须为非空且仅指向 Base 定义"
        }
        check(definition(PersonalityId.CHAMELEON).transitionTargets.toSet() == baseIds) {
            "CHAMELEON 的 transitionTargets 必须为全部 10 个 Base 定义"
        }
        check(definition(PersonalityId.WOLF).transitionTargets == listOf(PersonalityId.NIGHT_OWL, PersonalityId.OWL)) {
            "WOLF 的 transitionTargets 必须为 {NIGHT_OWL, OWL}"
        }
    }
}
