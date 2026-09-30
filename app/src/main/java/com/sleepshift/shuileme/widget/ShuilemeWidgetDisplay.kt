package com.sleepshift.shuileme.widget

import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.model.realSleepElapsedMin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 桌面组件显示逻辑（纯函数，JVM 可单测）。
 * 组件渲染与首页复用同一套文案/emoji 推导；emoji 优先，零图片资源。
 */
object ShuilemeWidgetDisplay {

    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

    fun title(): String = "睡了么"

    /** 虚拟时间 HH:mm（真实 + 偏移） */
    fun virtualTimeText(state: ShuilemeState, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val virtualMs = virtualTimeMs(state, nowMs)
        return TIME_FMT.format(Instant.ofEpochMilli(virtualMs).atZone(zone))
    }

    /** 月亮 emoji：满月奖励=🌝；睡眠中=进度相；清醒=成长阶段或 🌚（虚拟时间过午夜） */
    fun moonEmoji(state: ShuilemeState, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (state.isSleeping) {
            val start = state.sleepStartAtMs ?: return "🌙"
            val progress = ((nowMs - start) / 60_000.0) / VirtualClockEngine.DEFAULT_TARGET_SLEEP_MIN
            return sleepProgressMoon(progress)
        }
        return when {
            state.moonProgress.fullMoonRewardPending -> "🌝"
            isVirtualVeryLate(virtualTimeMs(state, nowMs), zone) -> "🌚"
            else -> state.moonProgress.phaseEmoji
        }
    }

    /** 当前状态行 */
    fun statusLine(state: ShuilemeState, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (state.isSleeping) {
            val start = state.sleepStartAtMs ?: return "睡眠中…"
            val elapsedMin = realSleepElapsedMin(start, nowMs)
            return "睡眠中 · 已睡 ${formatElapsed(elapsedMin)}"
        }
        return when {
            state.moonProgress.fullMoonRewardPending -> "🌝 满月达成！"
            isVirtualVeryLate(virtualTimeMs(state, nowMs), zone) -> "🌚 这么晚还不睡？"
            else -> "今晚让月亮长大一点"
        }
    }

    /** 月亮成长百分比行：如 "🌒 40%" / "满月达成！" */
    fun growthText(state: ShuilemeState): String {
        val mp = state.moonProgress
        return if (mp.fullMoonRewardPending) "满月达成！"
        else "${mp.phaseEmoji} ${mp.growthPercent}%"
    }

    /** 连续/次数行：如 "连续 3 天 · 共 12 次" */
    fun streakText(state: ShuilemeState): String =
        "连续 ${state.streakDays} 天 · 共 ${state.sleepCount} 次"

    /** 一键入睡成功提示 */
    fun sleepConfirmationText(): String = "🌙 好啦，月亮开始成长了"

    /** 一键醒来提示 */
    fun wakeConfirmationText(): String = "☀️ 醒啦！月亮记住了"

    // ── 内部 ──

    private fun virtualTimeMs(state: ShuilemeState, nowMs: Long): Long =
        VirtualClockEngine(state.toVirtualClockConfig()).virtualTimeMs(nowMs)

    private fun isVirtualVeryLate(virtualMs: Long, zone: ZoneId): Boolean {
        val hour = Instant.ofEpochMilli(virtualMs).atZone(zone).hour
        return hour < 6
    }

    private val MOON_WAX = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")

    private fun sleepProgressMoon(progress: Double): String {
        val idx = (progress.coerceIn(0.0, 1.0) * MOON_WAX.size).toInt().coerceIn(0, MOON_WAX.size - 1)
        return MOON_WAX[idx]
    }

    private fun formatElapsed(totalMin: Long): String {
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h <= 0L -> "$m 分钟"
            m == 0L -> "$h 小时"
            else -> "$h 小时 $m 分钟"
        }
    }
}
