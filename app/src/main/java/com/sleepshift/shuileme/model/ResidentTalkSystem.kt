package com.sleepshift.shuileme.model

/**
 * 居民碎碎念系统（SL-6）。
 * - 低频触发：每日默认 0~3 次；
 * - 依据：睡眠情况 / 当前时间 / 人格 / 月亮状态 / 用户活跃度；
 * - 文案为预置模板，禁止 AI。
 */
object ResidentTalkSystem {

    const val DEFAULT_MAX_PER_DAY = 3

    enum class TalkScenario { GENERAL, LATE, GOOD_SLEEP, MOON }

    /** 触发判定：今日未超上限 且 时段合适 */
    fun shouldTrigger(
        todayCount: Int,
        hourOfDay: Int,
        maxPerDay: Int = DEFAULT_MAX_PER_DAY,
    ): Boolean {
        if (todayCount >= maxPerDay) return false
        // 深夜/清晨更愿意"说话"（22-24 与 0-2 高发）
        return hourOfDay >= 21 || hourOfDay < 3
    }

    /** 场景选择：熬夜 → LATE；月亮情绪庆祝 → MOON；合格睡眠 → GOOD_SLEEP；否则 GENERAL */
    fun pickScenario(isLate: Boolean, moonMood: MoonMood, sleepQualified: Boolean?): TalkScenario = when {
        isLate -> TalkScenario.LATE
        moonMood == MoonMood.CELEBRATING -> TalkScenario.MOON
        sleepQualified == true -> TalkScenario.GOOD_SLEEP
        else -> TalkScenario.GENERAL
    }

    /** 取一条碎碎念（variant 轮换；场景缺失回退 GENERAL） */
    fun pickTalk(type: SleepPersonalityType, scenario: TalkScenario, variant: Int): String {
        val list = TALKS[type]?.get(scenario)
            ?: TALKS[type]?.get(TalkScenario.GENERAL)
            ?: listOf("该睡啦。")
        val v = (variant % list.size + list.size) % list.size
        return list[v]
    }

    private val TALKS: Map<SleepPersonalityType, Map<TalkScenario, List<String>>> = mapOf(
        SleepPersonalityType.NIGHT_OWL to mapOf(
            TalkScenario.GENERAL to listOf("说好的最后五分钟呢？", "凌晨的你精神很好，早上的你会感谢我。"),
            TalkScenario.LATE to listOf("又熬夜？夜猫子要成精了。", "月亮都困了，你怎么还醒着。"),
            TalkScenario.GOOD_SLEEP to listOf("早睡了？夜猫子改行了？", "不错，猫睡饱才有力气抓星星。"),
            TalkScenario.MOON to listOf("月亮今晚很亮，你也很棒。"),
        ),
        SleepPersonalityType.EARLY_BIRD to mapOf(
            TalkScenario.GENERAL to listOf("早起鸟打卡：月亮还在打盹。", "晨光都起来了，你还不起？"),
            TalkScenario.LATE to listOf("熬夜？早起鸟要生气的。", "再不睡，明天就没鸟叫了。"),
            TalkScenario.GOOD_SLEEP to listOf("早睡早起，小鸟都为你唱歌。", "满分！晨露是你的奖章。"),
            TalkScenario.MOON to listOf("月亮刚走，太阳就来啦。"),
        ),
        SleepPersonalityType.WORK_HORSE to mapOf(
            TalkScenario.GENERAL to listOf("牛马休息了吗？还没有。", "今天也是被生活鞭策的一天。"),
            TalkScenario.LATE to listOf("凌晨加班？牛马命也是命。", "再不睡，明天就是死牛马。"),
            TalkScenario.GOOD_SLEEP to listOf("睡饱了？牛马有牛马的觉悟。", "不错，磨刀不误砍柴工。"),
            TalkScenario.MOON to listOf("月亮下班，牛马上班。"),
        ),
        SleepPersonalityType.OTTER to mapOf(
            TalkScenario.GENERAL to listOf("海獭抱着月亮睡着了。", "水很暖，睡意刚刚好。"),
            TalkScenario.LATE to listOf("海獭都睡了，你怎么还醒着？", "熬夜的海獭会浮不起来的。"),
            TalkScenario.GOOD_SLEEP to listOf("睡得好，海獭给你比了个赞。", "今晚的海面很平静，睡得很香。"),
            TalkScenario.MOON to listOf("月亮像海獭的抱枕，很安心。"),
        ),
        SleepPersonalityType.CHAOS to mapOf(
            TalkScenario.GENERAL to listOf("今天的睡眠像风一样飘忽。", "月亮追不上你的作息。"),
            TalkScenario.LATE to listOf("混沌表示：反正也没规律，随便熬。", "再乱一点，月亮就找不到你了。"),
            TalkScenario.GOOD_SLEEP to listOf("哦？今天居然有规律？", "混沌中出现了秩序，难得。"),
            TalkScenario.MOON to listOf("月亮在混沌里找到了你。"),
        ),
        SleepPersonalityType.MOON_GUARDIAN to mapOf(
            TalkScenario.GENERAL to listOf("今天也辛苦啦。", "守护月亮的人，也要照顾好自己。"),
            TalkScenario.LATE to listOf("守护者怎么还醒着？月亮该你守护了。", "满月不会跑，但你要睡了。"),
            TalkScenario.GOOD_SLEEP to listOf("满月圆满，你功不可没。", "月亮因你而亮。"),
            TalkScenario.MOON to listOf("早点睡，月亮会成长。", "月亮收到了你的守护。"),
        ),
        SleepPersonalityType.NIGHT_GHOST to mapOf(
            TalkScenario.GENERAL to listOf("深夜的幽灵，白天见不到。", "你属于黑夜，但黑夜不属于你太久。"),
            TalkScenario.LATE to listOf("幽灵本体出没，凌晨两点。", "再熬下去，你就真的变成幽灵了。"),
            TalkScenario.GOOD_SLEEP to listOf("幽灵居然睡了？世界安静了。", "白天的你回来了。"),
            TalkScenario.MOON to listOf("幽灵也怕月亮太亮。"),
        ),
        SleepPersonalityType.PROCASTINATOR to mapOf(
            TalkScenario.GENERAL to listOf("摆烂一时爽，一直摆烂一直爽？", "睡意全无，摆烂第一。"),
            TalkScenario.LATE to listOf("摆烂大师：反正明天也没事，再熬。", "睡什么睡，起来嗨（才怪）。"),
            TalkScenario.GOOD_SLEEP to listOf("摆烂大师居然按时睡了？奇迹。", "不错，今天没摆烂。"),
            TalkScenario.MOON to listOf("月亮：你终于不摆烂了。"),
        ),
    )
}
