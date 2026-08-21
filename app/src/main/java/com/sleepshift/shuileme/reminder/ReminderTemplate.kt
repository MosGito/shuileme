package com.sleepshift.shuileme.reminder

/**
 * 提醒文案预置模板池（SL-4）：3 人格 × 3 类型 × 5 模板 = 45 条。
 * 非 AI 生成，可控、可离线。
 */
object ReminderTemplate {

    private val TEMPLATES: Map<ReminderPersonality, Map<ReminderType, List<String>>> = mapOf(
        ReminderPersonality.MOON to mapOf(
            ReminderType.SLEEP to listOf(
                "今天辛苦啦，月亮想你了，要不要一起睡？",
                "夜已深，月亮在等你说晚安。",
                "慢慢来，我们先把今天睡过去。",
                "月亮把星星调暗了，该睡啦。",
                "放下手机，月亮想陪你睡一会儿。",
            ),
            ReminderType.LATE to listOf(
                "这么晚还不睡？月亮有点担心你。",
                "月亮看你屏幕还亮着，有点心疼。",
                "熬夜的话，月亮会黯淡一点的……",
                "月亮数了 100 只羊，你还没睡。",
                "让月亮陪你睡吧，明天再继续。",
            ),
            ReminderType.WAKE_FEEDBACK to listOf(
                "醒啦 ☀️ 月亮陪你睡了一夜。",
                "早上好，月亮说你睡得很棒。",
                "新的一天，月亮会继续陪着你。",
                "睡醒啦，月亮为你点了赞。",
                "早安，月亮把夜收走了，把晨光留给你。",
            ),
        ),
        ReminderPersonality.SHARP to mapOf(
            ReminderType.SLEEP to listOf(
                "第几个最后一把了？你手速可没赢过月亮。",
                "再不睡，明天黑眼圈可以当烟熏妆了。",
                "手机比你对象还亲？快睡。",
                "你是不是觉得月亮会一直等你？",
                "别磨蹭了，被窝比刷手机香多了。",
            ),
            ReminderType.LATE to listOf(
                "还在熬？你是在给月亮打工吗？",
                "这个点了还精神，你比夜宵摊还敬业。",
                "再刷下去，明天的你会感谢现在的你？不会的。",
                "你的黑眼圈要集齐七颗召唤神龙了。",
                "月亮都懒得看你了，快睡！",
            ),
            ReminderType.WAKE_FEEDBACK to listOf(
                "醒了？看来月亮没白陪你。",
                "早上好，黑眼圈请到前台登记。",
                "睡了还这么久，月亮都等困了。",
                "醒啦，今天也要记得早点睡哦（别看我）。",
                "月亮：谢天谢地你终于醒了。",
            ),
        ),
        ReminderPersonality.WORK_HORSE to mapOf(
            ReminderType.SLEEP to listOf(
                "明天还要当牛马，现在不睡，明天就是死牛马。",
                "牛马需要休息，不然明天拉不动磨。",
                "睡吧，明天还得继续被生活鞭策。",
                "牛马的命也是命，先睡为敬。",
                "今天的牛马辛苦了，明天还要当牛马，先睡。",
            ),
            ReminderType.LATE to listOf(
                "凌晨还在当牛马？加班费结了吗？",
                "这个点还不睡，明天牛马状态拉满？",
                "熬夜的牛马，明天会变成死牛马。",
                "别卷了，牛马卷不动月亮。",
                "再熬下去，明天你就是被宰的牛马。",
            ),
            ReminderType.WAKE_FEEDBACK to listOf(
                "醒了？牛马开工了。",
                "早安，今天也是被生活鞭策的一天。",
                "睡好了吗？牛马要有牛马的觉悟。",
                "醒啦，月亮下班，牛马上班。",
                "新的一天，继续当牛马（但精神满满）。",
            ),
        ),
    )

    private val pool: Map<Pair<ReminderPersonality, ReminderType>, List<String>> = buildMap {
        ReminderPersonality.entries.forEach { p ->
            ReminderType.entries.forEach { t ->
                put(p to t, TEMPLATES[p]!![t]!!)
            }
        }
    }

    /** 取某人格某类型的一条模板（variant 取模 5 轮换，不重复使用同一句） */
    fun pick(personality: ReminderPersonality, type: ReminderType, variant: Int): String {
        val list = pool[personality to type] ?: return "该睡啦"
        return list[(variant % list.size + list.size) % list.size]
    }

    /** 模板总数（45） */
    fun templateCount(): Int = pool.values.sumOf { it.size }
}
