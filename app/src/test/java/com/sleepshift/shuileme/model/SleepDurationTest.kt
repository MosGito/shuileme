package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * 「已睡多久」真实时长计算（Bug 修复回归测试）。
 *
 * 核心产品规则：虚拟时间偏移只影响钟表显示，绝不参与真实睡眠持续时长计算；
 * duration 必须基于真实时间经过量（sleepStartAtMs 为真实基准）。
 */
class SleepDurationTest {

    private fun ms(iso: String): Long = Instant.parse(iso).toEpochMilli()

    @Test
    fun `刚入睡 - 已睡 0 分钟（offset 无关）`() {
        val start = ms("2026-08-21T22:00:00Z")
        assertEquals(0L, realSleepElapsedMin(start, start))
        // 不足 1 分钟仍为 0
        assertEquals(0L, realSleepElapsedMin(start, start + 59_999L))
    }

    @Test
    fun `真实经过 30 分钟 - 无论偏移多少都显示 30 分钟`() {
        val start = ms("2026-08-21T22:00:00Z")
        val now = start + 30 * 60_000L
        assertEquals(30L, realSleepElapsedMin(start, now))
    }

    @Test
    fun `虚拟时间跨日期 - 时长仍按真实经过计算`() {
        val start = ms("2026-08-21T23:50:00Z")
        val now = ms("2026-08-22T00:20:00Z")
        assertEquals(30L, realSleepElapsedMin(start, now))
    }

    @Test
    fun `系统时钟回拨 - 钳制为 0 不为负`() {
        val start = ms("2026-08-21T22:30:00Z")
        val now = ms("2026-08-21T22:00:00Z")
        assertEquals(0L, realSleepElapsedMin(start, now))
    }

    @Test
    fun `真实 7 小时 - 返回 420 分钟`() {
        val start = ms("2026-08-21T23:00:00Z")
        val now = ms("2026-08-22T06:00:00Z")
        assertEquals(420L, realSleepElapsedMin(start, now))
    }
}
