package com.sleepshift.shuileme.model

import com.sleepshift.shuileme.data.ShuilemeState
import java.time.LocalDate

/**
 * 「我的睡眠人格」分享卡片模型（SL-6.5）。
 * 数据来源：SleepPersonalityEngine（metrics）+ SleepRecord + MoonLife。
 * 至少 5 次有效睡眠记录才生成。
 */
data class PersonalityCardModel(
    val personalityType: SleepPersonalityType? = null,
    /** 装饰用人格气泡（仅 emoji 符号，不赋予角色） */
    val emojiDecoration: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val averageSleepTime: String = "--:--",
    val averageWakeTime: String = "--:--",
    val averageDuration: String = "--",
    val regularityScore: Int = 0,
    val generatedDate: String = "",
    val unlocked: Boolean = false,
    /** 距解锁还差几晚 */
    val nightsNeeded: Int = 0,
)

object PersonalityCardGenerator {

    const val MIN_SESSIONS = 5
    private const val LOCKED_COPY = "再睡几晚，解锁你的睡眠人格 🌙"

    fun generate(state: ShuilemeState, personality: SleepPersonalityState): PersonalityCardModel {
        val type = personality.primaryType
        if (type == null) {
            val need = (MIN_SESSIONS - state.sessions.size).coerceAtLeast(0)
            return PersonalityCardModel(nightsNeeded = need, unlocked = false)
        }
        val metrics = personality.metrics
        return PersonalityCardModel(
            personalityType = type,
            emojiDecoration = decorateEmojis(type, state.sleepCount),
            title = "${type.displayName}型",
            description = descriptionFor(type),
            averageSleepTime = formatTime(metrics.avgSleepTimeMin),
            averageWakeTime = formatTime(metrics.avgWakeTimeMin),
            averageDuration = formatDuration(metrics.avgDurationMin),
            regularityScore = (metrics.regularityScore * 100).toInt().coerceIn(0, 100),
            generatedDate = LocalDate.now().toString(),
            unlocked = true,
            nightsNeeded = 0,
        )
    }

    /** 人格描述（固定模板，禁止 AI） */
    fun descriptionFor(type: SleepPersonalityType): String = when (type) {
        SleepPersonalityType.NIGHT_OWL -> "夜晚是你的主场，但月亮希望你早点回家。"
        SleepPersonalityType.EARLY_BIRD -> "清晨的启动速度很优秀，月亮为你骄傲。"
        SleepPersonalityType.WORK_HORSE -> "工作很努力，但月亮建议你早点休息。"
        SleepPersonalityType.OTTER -> "抱着好睡眠的海獭，月亮都想给你抱枕。"
        SleepPersonalityType.CHAOS -> "睡眠无规律，但月亮还是追上了你。"
        SleepPersonalityType.MOON_GUARDIAN -> "满月的守护者，连月亮都要谢你。"
        SleepPersonalityType.NIGHT_GHOST -> "深夜的幽灵，白天见不到你。"
        SleepPersonalityType.PROCASTINATOR -> "摆烂一时爽，一直摆烂一直爽。"
    }

    /**
     * 装饰气泡：人格 combo emoji 的确定性"随机"排列（seed 驱动），取 3~5 个。
     * 只作人格符号，不命名、不赋予角色。
     */
    fun decorateEmojis(type: SleepPersonalityType, seed: Int): List<String> {
        val combo = type.comboEmojis
        val count = 3 + (seed % 3) // 3..5
        val rotated = (0 until combo.size).map { combo[(it + seed) % combo.size] }
        return rotated.take(count)
    }

    /** 分享固定文本 */
    fun shareText(title: String): String = "我的睡眠人格是 $title 🌙\n来自睡了么"

    fun lockedCopy(): String = LOCKED_COPY

    /** 人格双 emoji 表示（如 🌙🐱） */
    fun personalityPairEmoji(type: SleepPersonalityType): String =
        type.primaryEmoji + type.comboEmojis.last()

    private fun formatTime(minOfDay: Int?): String =
        minOfDay?.let { String.format("%02d:%02d", it / 60, it % 60) } ?: "--:--"

    private fun formatDuration(min: Long?): String =
        min?.let { "${it / 60}h${it % 60}m" } ?: "--"
}
