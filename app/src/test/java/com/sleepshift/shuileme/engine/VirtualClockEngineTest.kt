package com.sleepshift.shuileme.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class VirtualClockEngineTest {

    @Test
    fun `virtual time = real time + offset`() {
        val e = VirtualClockEngine(VirtualClockConfig(offsetMin = 120))
        assertEquals(1_000L + 120L * 60_000L, e.virtualTimeMs(1_000L))
        assertEquals(0L + 0L, VirtualClockEngine(VirtualClockConfig(offsetMin = 0)).virtualTimeMs(0L))
    }

    @Test
    fun `default offset is 120 minutes (later)`() {
        assertEquals(120, VirtualClockEngine.DEFAULT_OFFSET_MIN)
        val e = VirtualClockEngine()
        assertEquals(120L * 60_000L, e.virtualTimeMs(0L))
    }

    @Test
    fun `offset clamps to max 240`() {
        val e = VirtualClockEngine(VirtualClockConfig(offsetMin = 999))
        assertEquals(240L * 60_000L, e.virtualTimeMs(0L))
        assertEquals(240, e.effectiveOffsetMin(0))
    }

    @Test
    fun `offset clamps to min 0`() {
        val e = VirtualClockEngine(VirtualClockConfig(offsetMin = -50))
        assertEquals(0L, e.virtualTimeMs(0L))
        assertEquals(0, e.effectiveOffsetMin(0))
    }

    @Test
    fun `fixed mode keeps target offset`() {
        val e = VirtualClockEngine(VirtualClockConfig(offsetMin = 120, mode = OffsetMode.FIXED))
        (0..10).forEach { assertEquals(120, e.effectiveOffsetMin(it)) }
    }

    @Test
    fun `gradual ramps to target then caps`() {
        val e = VirtualClockEngine(
            VirtualClockConfig(offsetMin = 120, mode = OffsetMode.GRADUAL, gradualStepMin = 30)
        )
        assertEquals(30, e.effectiveOffsetMin(0))
        assertEquals(60, e.effectiveOffsetMin(1))
        assertEquals(90, e.effectiveOffsetMin(2))
        assertEquals(120, e.effectiveOffsetMin(3))
        assertEquals(120, e.effectiveOffsetMin(10))
    }

    @Test
    fun `fluctuation stays within target range and is deterministic`() {
        val e = VirtualClockEngine(
            VirtualClockConfig(offsetMin = 120, mode = OffsetMode.FLUCTUATION, fluctuationRangeMin = 30)
        )
        val days = (0 until 20).map { e.effectiveOffsetMin(it) }
        days.forEach { assertTrue("offset=$it 越界[90,150]", it in 90..150) }
        // 确定性：同一 dayIndex 结果恒定
        assertEquals(e.effectiveOffsetMin(5), e.effectiveOffsetMin(5))
        // 多样性：20 天应产生多种取值
        assertTrue("应产生多种偏移，实际=${days.distinct()}", days.distinct().size >= 2)
    }

    @Test
    fun `format virtual time in fixed zone`() {
        val zone = ZoneId.of("UTC")
        val real = Instant.parse("2026-08-21T23:30:00Z").toEpochMilli()
        assertEquals("23:30", VirtualClockEngine(VirtualClockConfig(offsetMin = 0)).formatVirtualTime(real, zone))
        // +120min 跨天：23:30 → 次日 01:30
        assertEquals("01:30", VirtualClockEngine(VirtualClockConfig(offsetMin = 120)).formatVirtualTime(real, zone))
    }

    @Test
    fun `virtual and real pair`() {
        val zone = ZoneId.of("UTC")
        val real = Instant.parse("2026-08-21T22:30:00Z").toEpochMilli()
        val (v, r) = VirtualClockEngine(VirtualClockConfig(offsetMin = 120)).virtualAndReal(real, zone)
        assertEquals("00:30", v)
        assertEquals("22:30", r)
    }

    @Test
    fun `窗口内虚拟时间偏移 窗口外真实时间`() {
        val zone = ZoneId.of("UTC")
        val e = VirtualClockEngine(VirtualClockConfig(offsetMin = 120))
        // 目标窗口 23:00 - 07:00（跨午夜）
        val inWindow = Instant.parse("2026-08-21T23:30:00Z").toEpochMilli()
        assertTrue(e.isInSleepWindow(inWindow, 23 * 60, 7 * 60, zone))
        assertEquals(inWindow + 120L * 60_000L, e.virtualTimeMs(inWindow, 23 * 60, 7 * 60, zone))
        // 窗口外（白天 12:00）→ 真实时间
        val outside = Instant.parse("2026-08-21T12:00:00Z").toEpochMilli()
        assertFalse(e.isInSleepWindow(outside, 23 * 60, 7 * 60, zone))
        assertEquals(outside, e.virtualTimeMs(outside, 23 * 60, 7 * 60, zone))
    }
}
