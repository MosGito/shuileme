package com.sleepshift.ui

import com.sleepshift.model.MINUTES_PER_DAY
import com.sleepshift.model.SleepShiftSettings
import com.sleepshift.model.computeNightWindow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 友好偏移文案：120 → "2 小时"；0 → "不提前" */
fun formatOffsetFriendly(minutes: Int): String = when {
    minutes <= 0 -> "不提前"
    minutes % 60 == 0 -> "${minutes / 60} 小时"
    else -> "$minutes 分钟"
}

/** 配置页实时解释文案（无需理解时区）："今晚 22:30 开始，手机时间会提前 2 小时" */
fun configExplanation(plan: NightPlan): String {
    if (!plan.valid) return "请先检查开始与恢复时间的设置"
    val start = plan.startReal?.format(TIME_FMT) ?: return ""
    return "今晚 $start 开始，手机时间会提前 ${formatOffsetFriendly(plan.offsetMin)}"
}

/** 今晚/当前睡眠窗口的展示用推算（阶段 2 UI 预览；阶段 4 起由 Scheduler 提供真实状态） */
data class NightPlan(
    val valid: Boolean,
    val offsetMin: Int,
    val startReal: LocalDateTime?,
    val startDisplay: LocalTime?,
    val restoreReal: LocalDateTime?,
    val restoreDisplay: LocalTime?,
    val nowShifted: Boolean,
)

fun computeNightPlan(now: LocalDateTime, settings: SleepShiftSettings): NightPlan {
    val window = computeNightWindow(settings)
    if (window == null) {
        return NightPlan(
            valid = false,
            offsetMin = settings.offsetMin,
            startReal = null,
            startDisplay = null,
            restoreReal = null,
            restoreDisplay = null,
            nowShifted = false,
        )
    }
    val startReal = nextOccurrenceOf(window.startTimeMin, now)
    val restoreReal = startReal.plusMinutes(window.realWindowMin.toLong())
    val startDisplayMin = (window.startTimeMin + window.offsetMin) % MINUTES_PER_DAY
    val startDisplay = LocalTime.of(startDisplayMin / 60, startDisplayMin % 60)
    val restoreDisplay = LocalTime.of(window.restoreTimeMin / 60, window.restoreTimeMin % 60)

    val activeStart = previousOccurrenceOf(window.startTimeMin, now)
    val activeEnd = activeStart.plusMinutes(window.realWindowMin.toLong())
    val nowShifted = settings.enabled && !now.isBefore(activeStart) && now.isBefore(activeEnd)

    return NightPlan(
        valid = true,
        offsetMin = window.offsetMin,
        startReal = startReal,
        startDisplay = startDisplay,
        restoreReal = restoreReal,
        restoreDisplay = restoreDisplay,
        nowShifted = nowShifted,
    )
}

private fun nextOccurrenceOf(timeMin: Int, from: LocalDateTime): LocalDateTime {
    val candidate = from.toLocalDate().atTime(timeMin / 60, timeMin % 60)
    return if (candidate.isAfter(from)) candidate else candidate.plusDays(1)
}

private fun previousOccurrenceOf(timeMin: Int, from: LocalDateTime): LocalDateTime {
    val candidate = from.toLocalDate().atTime(timeMin / 60, timeMin % 60)
    return if (!candidate.isAfter(from)) candidate else candidate.minusDays(1)
}
