package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MoonProgressTest {

    private val target23 = 23 * 60 // 23:00

    /** 构造指定本地时刻的 epoch ms（用系统默认时区，与引擎一致） */
    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun qualified(min: Long = 480) = SleepResult(min, true, null)
    private fun unqualified() = SleepResult(300, false, SleepResultReason.TOO_SHORT)

    // ── 睡眠完成判定 ──

    @Test
    fun `8小时正常睡眠判定为合格`() {
        val sleepStart = at(22, 30)      // 22:30 入睡
        val wake = sleepStart + 8 * 3_600_000 // 次日 06:30，8h
        val r = MoonProgress().evaluate(sleepStart, wake, target23)
        assertEquals(480, r.durationMinutes)
        assertTrue("8h 应合格", r.isQualified)
        assertNull(r.reason)
    }

    @Test
    fun `5小时睡眠时长不足判定为失败`() {
        val sleepStart = at(23, 0)
        val wake = sleepStart + 5 * 3_600_000 // 05:00，5h
        val r = MoonProgress().evaluate(sleepStart, wake, target23)
        assertEquals(300, r.durationMinutes)
        assertFalse("5h 不应合格", r.isQualified)
        assertEquals(SleepResultReason.TOO_SHORT, r.reason)
    }

    @Test
    fun `凌晨3点入睡判定为失败（过晚）`() {
        val sleepStart = at(3, 0)   // 03:00 入睡
        val wake = at(11, 0)        // 08:00，8h
        val r = MoonProgress().evaluate(sleepStart, wake, target23)
        assertEquals(480, r.durationMinutes)
        assertFalse("凌晨3点不应合格", r.isQualified)
        assertEquals(SleepResultReason.STARTED_TOO_LATE, r.reason)
    }

    @Test
    fun `23点30分宽限边界内仍合格`() {
        val sleepStart = at(23, 30) // 目标 23:00 + 30 宽限
        val wake = sleepStart + 7 * 3_600_000 + 30 * 60_000L
        val r = MoonProgress().evaluate(sleepStart, wake, target23)
        assertTrue("23:30 入睡应合格（宽限边界）", r.isQualified)
    }

    // ── 月亮成长 ──

    @Test
    fun `不合格不增加成长且清空连续计数`() {
        var m = MoonProgress()
        m = m.applyResult(qualified())      // 合格 growth=20
        m = m.applyResult(unqualified())    // 不合格
        assertEquals(20, m.growthPercent)
        assertEquals(0, m.consecutiveQualified)
        assertEquals(2, m.totalCompletedSleeps)
        assertFalse(m.lastSleepResult!!.isQualified)
    }

    @Test
    fun `连续5晚成长达满月`() {
        var m = MoonProgress()
        m = m.applyResult(qualified())
        assertEquals(20, m.growthPercent)
        assertEquals("🌒", m.phaseEmoji)
        m = m.applyResult(qualified())
        assertEquals(40, m.growthPercent)
        assertEquals("🌓", m.phaseEmoji)
        m = m.applyResult(qualified())
        assertEquals(60, m.growthPercent)
        m = m.applyResult(qualified())
        assertEquals(80, m.growthPercent)
        assertEquals(4, m.consecutiveQualified)
        assertEquals("🌔", m.phaseEmoji)
        // 第 5 晚 → 满月
        m = m.applyResult(qualified())
        assertEquals("growth 应重置为 0", 0, m.growthPercent)
        assertEquals(0, m.consecutiveQualified)
        assertEquals(5, m.totalCompletedSleeps)
        assertTrue("满月奖励待展示", m.fullMoonRewardPending)
    }

    @Test
    fun `满月重置后重新开始成长`() {
        var m = MoonProgress()
        repeat(5) { m = m.applyResult(qualified()) } // 满月 + 重置
        assertTrue(m.fullMoonRewardPending)
        assertEquals(0, m.growthPercent)
        // 下一晚合格：重新从 20 开始
        m = m.applyResult(qualified())
        assertEquals(20, m.growthPercent)
        assertEquals(1, m.consecutiveQualified)
        assertEquals(6, m.totalCompletedSleeps)
    }

    @Test
    fun `阶段映射正确`() {
        assertEquals(MoonPhase.NEW, MoonPhase.fromGrowth(0))
        assertEquals(MoonPhase.CRESCENT, MoonPhase.fromGrowth(20))
        assertEquals(MoonPhase.FIRST_QUARTER, MoonPhase.fromGrowth(40))
        assertEquals(MoonPhase.GIBBOUS, MoonPhase.fromGrowth(80))
        assertEquals(MoonPhase.FULL, MoonPhase.fromGrowth(100))
        assertEquals("🌑", MoonPhase.NEW.emoji)
        assertEquals("🌕", MoonPhase.FULL.emoji)
    }
}
