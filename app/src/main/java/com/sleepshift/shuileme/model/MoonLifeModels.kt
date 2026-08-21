package com.sleepshift.shuileme.model

/**
 * 月亮生命阶段（SL-5）：陪伴角色五阶段。
 * 由成长值（growth）推导；满月奖励时显示满月伙伴。
 */
enum class MoonStage(val emoji: String, val displayName: String) {
    /** 🌑 新月宝宝 */
    BABY("🌑", "新月宝宝"),
    /** 🌒 成长月牙 */
    CRESCENT("🌒", "成长月牙"),
    /** 🌓 半月伙伴 */
    HALF("🌓", "半月伙伴"),
    /** 🌔 接近满月 */
    NEAR_FULL("🌔", "接近满月"),
    /** 🌕 满月伙伴 */
    FULL("🌕", "满月伙伴");

    companion object {
        /** 由成长值取阶段：0→🌑 · 20→🌒 · 40→🌓 · 60/80→🌔 · 100→🌕 */
        fun fromGrowth(growth: Int): MoonStage = when {
            growth >= 100 -> FULL
            growth >= 60 -> NEAR_FULL
            growth >= 40 -> HALF
            growth >= 20 -> CRESCENT
            else -> BABY
        }
    }
}

/**
 * 月亮情绪（SL-5）。
 */
enum class MoonMood(val emoji: String, val label: String) {
    /** 😊 期待 */
    EXPECTANT("😊", "期待"),
    /** 🌙 温柔 */
    GENTLE("🌙", "温柔"),
    /** 😟 担心 */
    WORRIED("😟", "担心"),
    /** 😢 失望 */
    DISAPPOINTED("😢", "失望"),
    /** 🎉 庆祝 */
    CELEBRATING("🎉", "庆祝"),
}

/**
 * 月亮情绪计算（纯函数，可单测）。
 * 优先级：满月奖励 > 不合格(失望) > 合格(温柔) > 熬夜次数≥2(担心) > 连续合格(温柔) > 默认(期待)。
 */
fun computeMoonMood(
    sleepQualified: Boolean?,
    consecutiveQualified: Int,
    recentLateCount: Int,
    rewardPending: Boolean,
): MoonMood = when {
    rewardPending -> MoonMood.CELEBRATING
    sleepQualified == false -> MoonMood.DISAPPOINTED
    sleepQualified == true -> MoonMood.GENTLE
    recentLateCount >= 2 -> MoonMood.WORRIED
    consecutiveQualified >= 1 -> MoonMood.GENTLE
    else -> MoonMood.EXPECTANT
}

/** 月亮事件（SL-5）：预置文案池，非 AI，每日最多一次 */
data class MoonEvent(val text: String, val dateEpochDay: Long)

object MoonEventPool {
    private val EVENTS = listOf(
        "🌟 月亮捡到一颗星星，送给你。",
        "🌕 满月了！月亮激动得转了个圈。",
        "🌚 月亮打了个哈欠：昨晚你太闹了。",
        "👶 月亮打了个招呼：你好呀。",
        "☄️ 一颗流星划过，月亮替你许了个愿。",
        "🦦 月亮看见海獭抱着它睡着了，很安心。",
        "🐱 月亮说：你睡的样子像只小猫咪。",
        "🎈 月亮放了个小气球，庆祝你今晚不错。",
        "🍵 月亮给你热了一杯晚安茶。",
        "⏰ 月亮说：闹钟已设好，安心睡。",
    )

    /** 按 variant 确定性取一条（取模轮换，同一 variant 恒定） */
    fun pick(variant: Int): String {
        val v = (variant % EVENTS.size + EVENTS.size) % EVENTS.size
        return EVENTS[v]
    }

    fun size(): Int = EVENTS.size
}

/** 月亮生命状态（SL-5，聚合展示） */
data class MoonLife(
    /** 当前阶段（满月奖励时 = 满月伙伴） */
    val stage: MoonStage = MoonStage.BABY,
    /** 当前情绪 */
    val mood: MoonMood = MoonMood.EXPECTANT,
    /** 最近一条事件文案 */
    val lastEvent: String? = null,
    /** 最近事件日期（epochDay） */
    val lastEventDate: Long? = null,
    /** 累计满月奖励次数 */
    val totalRewards: Int = 0,
    /** 连续熬夜次数（近因，用于情绪） */
    val recentLateNights: Int = 0,
)
