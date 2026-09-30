package com.sleepshift.shuileme.widget

import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.OffsetMode
import com.sleepshift.shuileme.model.MoonProgress
import com.sleepshift.shuileme.model.SleepState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ShuilemeWidgetDisplayTest {

    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-08-21T22:30:00Z").toEpochMilli()

    private fun awakeState(
        offset: Int = 120,
        growth: Int = 0,
        reward: Boolean = false,
        streak: Int = 3,
        count: Int = 12,
    ) = ShuilemeState(
        currentOffsetMin = offset,
        mode = OffsetMode.FIXED,
        streakDays = streak,
        sleepCount = count,
        moonProgress = MoonProgress(growthPercent = growth, fullMoonRewardPending = reward),
    )

    private fun sleepingState(startMs: Long, offset: Int = 120) = ShuilemeState(
        state = SleepState.SLEEPING,
        sleepStartAtMs = startMs,
        currentOffsetMin = offset,
        mode = OffsetMode.FIXED,
    )

    // ── 清醒状态显示 ──

    @Test
    fun `清醒状态显示 - 标题 虚拟时间 月亮 状态 成长 连续`() {
        // 用非深夜时刻（19:00 + 2h = 21:00，非"很晚"）验证正常清醒态
        val nowNormal = Instant.parse("2026-08-21T19:00:00Z").toEpochMilli()
        val s = awakeState(growth = 40)
        assertEquals("睡了么", ShuilemeWidgetDisplay.title())
        // 19:00 + 120min = 21:00
        assertEquals("21:00", ShuilemeWidgetDisplay.virtualTimeText(s, nowNormal, zone))
        // growth 40 → 🌓
        assertEquals("🌓", ShuilemeWidgetDisplay.moonEmoji(s, nowNormal, zone))
        assertEquals("今晚让月亮长大一点", ShuilemeWidgetDisplay.statusLine(s, nowNormal, zone))
        assertEquals("🌓 40%", ShuilemeWidgetDisplay.growthText(s))
        assertEquals("连续 3 天 · 共 12 次", ShuilemeWidgetDisplay.streakText(s))
    }

    // ── 睡眠状态显示 ──

    @Test
    fun `睡眠状态显示 - 已睡时长基于真实时间 不受偏移影响 月亮为进度相`() {
        val s = sleepingState(startMs = now) // 刚入睡
        assertTrue(s.isSleeping)
        assertEquals("00:30", ShuilemeWidgetDisplay.virtualTimeText(s, now, zone))
        // 刚入睡：真实经过 0，偏移 +120 不得计入时长 → 已睡 0 分钟
        assertEquals("睡眠中 · 已睡 0 分钟", ShuilemeWidgetDisplay.statusLine(s, now, zone))
        // 进度 0 → 🌑
        assertEquals("🌑", ShuilemeWidgetDisplay.moonEmoji(s, now, zone))
    }

    @Test
    fun `睡眠状态显示 - 偏移 +2h 真实经过 30 分钟仍显示 30 分钟`() {
        val s = sleepingState(startMs = now, offset = 120)
        val later = now + 30 * 60_000L
        assertEquals("睡眠中 · 已睡 30 分钟", ShuilemeWidgetDisplay.statusLine(s, later, zone))
    }

    @Test
    fun `睡眠状态显示 - 偏移 -2h 刚入睡为 0 分钟 真实 30 分钟仍为 30 分钟`() {
        val s = sleepingState(startMs = now, offset = -120)
        assertEquals("睡眠中 · 已睡 0 分钟", ShuilemeWidgetDisplay.statusLine(s, now, zone))
        val later = now + 30 * 60_000L
        assertEquals("睡眠中 · 已睡 30 分钟", ShuilemeWidgetDisplay.statusLine(s, later, zone))
    }

    @Test
    fun `睡眠状态显示 - 跨午夜虚拟时间 时长仍按真实经过`() {
        val start = Instant.parse("2026-08-21T23:50:00Z").toEpochMilli()
        val s = sleepingState(startMs = start, offset = 120)
        val nextDay = Instant.parse("2026-08-22T00:20:00Z").toEpochMilli()
        assertEquals("睡眠中 · 已睡 30 分钟", ShuilemeWidgetDisplay.statusLine(s, nextDay, zone))
    }

    @Test
    fun `睡眠中月亮随时间推进`() {
        // 睡了 3.5 小时（210min），目标 420min → 进度 0.5 → 🌕（过半=满月）
        val s = sleepingState(startMs = now)
        val later = now + 210 * 60_000L
        assertEquals("🌕", ShuilemeWidgetDisplay.moonEmoji(s, later, zone))
    }

    // ── 月亮状态显示 ──

    @Test
    fun `月亮状态显示 - 满月奖励`() {
        val s = awakeState(growth = 0, reward = true)
        assertEquals("🌝", ShuilemeWidgetDisplay.moonEmoji(s, now, zone))
        assertEquals("🌝 满月达成！", ShuilemeWidgetDisplay.statusLine(s, now, zone))
        assertEquals("满月达成！", ShuilemeWidgetDisplay.growthText(s))
    }

    @Test
    fun `月亮状态显示 - 虚拟时间过午夜显示吐槽月亮`() {
        // 真实 23:30 + 120min = 次日 01:30（< 6 点）→ 🌚
        val lateNow = Instant.parse("2026-08-21T23:30:00Z").toEpochMilli()
        val s = awakeState()
        assertEquals("🌚", ShuilemeWidgetDisplay.moonEmoji(s, lateNow, zone))
        assertEquals("🌚 这么晚还不睡？", ShuilemeWidgetDisplay.statusLine(s, lateNow, zone))
    }

    @Test
    fun `一键提示文案`() {
        assertEquals("🌙 好啦，月亮开始成长了", ShuilemeWidgetDisplay.sleepConfirmationText())
        assertEquals("☀️ 醒啦！月亮记住了", ShuilemeWidgetDisplay.wakeConfirmationText())
    }
}
