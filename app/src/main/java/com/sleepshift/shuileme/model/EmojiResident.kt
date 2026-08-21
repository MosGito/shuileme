package com.sleepshift.shuileme.model

/**
 * Emoji 居民（SL-6）：一个居民 = 同 emoji 的"一小群"。
 * 人格不是单个 emoji，而是组合（如夜猫子 = 🌙🌙🌙🐱🐱）。
 */
data class EmojiResident(
    val emoji: String,
    val personalityType: SleepPersonalityType,
    val amount: Int,
    val mood: MoonMood = MoonMood.EXPECTANT,
    val talkStyle: String,
    val talkFrequency: Int = 2,
)

/**
 * 居民生成引擎（SL-6，纯函数）。
 * - 首页居民数量 5~12；
 * - 随睡眠趋势动态迁移：改善睡眠 → 旧人格居民减少、新（更健康）人格居民增加。
 */
object ResidentEngine {

    const val MIN_RESIDENTS = 5
    const val MAX_RESIDENTS = 12

    /**
     * SL-9.2.1：Persona Bubble 符号（直接来自 SleepPersonalityType）。
     * 夜猫子 🌙🐱 / 早起鸟 🌞🐦 / 牛马 🐮 / 海獭 🌊🦦 / 混沌 🌪️。
     */
    fun bubbleSymbols(type: SleepPersonalityType): List<String> = when (type) {
        SleepPersonalityType.NIGHT_OWL -> listOf("🌙", "🐱")
        SleepPersonalityType.EARLY_BIRD -> listOf("🌞", "🐦")
        SleepPersonalityType.WORK_HORSE -> listOf("🐮", "🐴") // SL-9.3：随机 🐮 或 🐴
        SleepPersonalityType.OTTER -> listOf("🌊", "🦦")
        SleepPersonalityType.CHAOS -> listOf("🌪️")
        SleepPersonalityType.MOON_GUARDIAN -> listOf("🌕", "✨")
        SleepPersonalityType.NIGHT_GHOST -> listOf("🌚", "👻")
        SleepPersonalityType.PROCASTINATOR -> listOf("💤", "😴")
    }

    /** 从人格类型生成装饰气泡（确定性，仅该人格符号，非随机月亮） */
    fun bubbleEmojis(type: SleepPersonalityType, count: Int, seed: Int): List<String> {
        if (count <= 0) return emptyList()
        val symbols = bubbleSymbols(type)
        return (0 until count).map { symbols[(it + seed) % symbols.size] }
    }

    /** 月亮还在认识用户：5 个月亮宝宝 */
    fun moonBabies(): List<EmojiResident> = listOf(
        EmojiResident("🌑", SleepPersonalityType.CHAOS, 1, MoonMood.EXPECTANT, "安静", 1),
        EmojiResident("🌒", SleepPersonalityType.CHAOS, 1, MoonMood.EXPECTANT, "安静", 1),
        EmojiResident("🌓", SleepPersonalityType.CHAOS, 1, MoonMood.EXPECTANT, "安静", 1),
        EmojiResident("🌔", SleepPersonalityType.CHAOS, 1, MoonMood.EXPECTANT, "安静", 1),
        EmojiResident("🌕", SleepPersonalityType.CHAOS, 1, MoonMood.EXPECTANT, "安静", 1),
    )

    /**
     * 生成居民：主体 = 主人格组合（5），迁移 = 进化进度带来的更健康人格居民（0~7）。
     * 总数量 = 5 + extra，恒在 [5, 12]。
     */
    fun generateResidents(
        primary: SleepPersonalityType?,
        evolutionProgress: Double,
        healthTarget: SleepPersonalityType,
    ): List<EmojiResident> {
        if (primary == null) return moonBabies()
        val extra = (evolutionProgress.coerceIn(0.0, 1.0) * (MAX_RESIDENTS - MIN_RESIDENTS)).toInt() // 0..7
        val shrink = (extra / 3).coerceAtMost(2)
        val primaryResidents = groupCombo(primary.comboEmojis, primary, take = primary.comboEmojis.size - shrink)
        val healthResidents = groupCombo(healthTarget.comboEmojis, healthTarget, take = extra + shrink)
        return (primaryResidents + healthResidents).filter { it.amount > 0 }
    }

    /** 居民总量（sum of amounts），用于数量限制校验 */
    fun totalResidents(residents: List<EmojiResident>): Int = residents.sumOf { it.amount }

    private fun groupCombo(combo: List<String>, type: SleepPersonalityType, take: Int): List<EmojiResident> {
        val list = combo.take(take.coerceAtLeast(0))
        return list.groupingBy { it }.eachCount().map { (emoji, amount) ->
            EmojiResident(emoji, type, amount, MoonMood.EXPECTANT, type.talkStyle)
        }
    }
}
