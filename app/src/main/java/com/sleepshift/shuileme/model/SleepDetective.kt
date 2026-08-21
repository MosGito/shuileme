package com.sleepshift.shuileme.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 睡眠侦探数据（SL-7，全部弱信号）。
 * 定位：娱乐化观察，非医疗/非精确监测。
 */
data class SleepDetectiveData(
    val sleepStartTime: Long? = null,
    val wakeUpTime: Long? = null,
    val selfReportedSleepDuration: Long? = null,
    val chargingDuration: Long = 0,
    val nightActivityHints: Int = 0,
)

/** 案件等级（🕵️ 三档） */
enum class DetectiveCaseLevel(val level: Int, val emoji: String, val displayName: String) {
    QUIET(1, "🕵️", "安静夜晚"),
    LITTLE_ACTION(2, "🕵️🕵️", "有一点小动作"),
    SNEAK_BACK(3, "🕵️🕵️🕵️", "月亮发现你偷偷回来过"),
}

/** 早晨侦探报告 */
data class MorningDetectiveReport(
    val caseLevel: DetectiveCaseLevel,
    val title: String,
    val observation: String,
    val conclusion: String,
)

/** 侦探文案池（纯本地预置，禁止 AI） */
object DetectiveTemplatePool {

    fun pickConclusion(level: DetectiveCaseLevel, variant: Int): String {
        val list = when (level) {
            DetectiveCaseLevel.QUIET -> listOf(
                "月亮很满意，你睡得很安静。",
                "本案无异常，结案。",
                "一夜无事，月亮打了个盹。",
            )
            DetectiveCaseLevel.LITTLE_ACTION -> listOf(
                "月亮发现了一些小动静。",
                "手机亮了一下，可能只是翻身。",
                "有一瞬间，世界动了动。",
            )
            DetectiveCaseLevel.SNEAK_BACK -> listOf(
                "月亮发现你偷偷回来过。",
                "你离开了一下，又回来了。",
                "半夜的你，被月亮逮了个正着。",
            )
        }
        return list[(variant % list.size + list.size) % list.size]
    }
}

/** 睡眠侦探（纯函数，可单测） */
object SleepDetective {

    fun computeLevel(hints: Int): DetectiveCaseLevel = when {
        hints >= 2 -> DetectiveCaseLevel.SNEAK_BACK
        hints == 1 -> DetectiveCaseLevel.LITTLE_ACTION
        else -> DetectiveCaseLevel.QUIET
    }

    fun generateReport(data: SleepDetectiveData, variant: Int): MorningDetectiveReport {
        val level = computeLevel(data.nightActivityHints)
        val sleepTime = formatTime(data.sleepStartTime)
        val observation = when (level) {
            DetectiveCaseLevel.QUIET -> "你说 $sleepTime 睡觉，月亮守了一夜，很安静。"
            DetectiveCaseLevel.LITTLE_ACTION -> "你说 $sleepTime 睡觉，但是月亮发现夜里有些小动作。"
            DetectiveCaseLevel.SNEAK_BACK -> "你说 $sleepTime 睡觉，但是月亮发现你偷偷回来过。"
        }
        return MorningDetectiveReport(
            caseLevel = level,
            title = "昨晚睡眠案件",
            observation = observation,
            conclusion = DetectiveTemplatePool.pickConclusion(level, variant),
        )
    }

    fun formatTime(ms: Long?): String =
        ms?.let { DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) }
            ?: "？"
}
