package com.sleepshift.ui

import com.sleepshift.model.MINUTES_PER_DAY
import com.sleepshift.model.SleepShiftSettings
import com.sleepshift.model.computeNightWindow
import java.time.LocalDateTime
import java.time.LocalTime

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
