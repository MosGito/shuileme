package com.sleepshift.time

import com.sleepshift.model.SchedulerState
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings
import com.sleepshift.model.computeNightWindow
import com.sleepshift.model.nightLengthMin
import com.sleepshift.strategy.GradualStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimezoneSchedulerTest {

    private val originalZone = "Asia/Shanghai"

    /** 案例：start 22:30、restore 06:30、offset 120 → 22:30 进入偏移，真实 04:30 恢复（显示 06:30） */
    @Test
    fun `planNight - shift and restore epoch for 2230-0630 offset 120`() {
        val settings = SleepShiftSettings(
            enabled = true,
            startTimeMin = 22 * 60 + 30,
            restoreTimeMin = 6 * 60 + 30,
            offsetMin = 120,
        )
        // 夜间显示时长 8h，真实窗口 8h - 2h = 6h
        assertEquals(480, nightLengthMin(settings.startTimeMin, settings.restoreTimeMin))
        assertEquals(360, computeNightWindow(settings)!!.realWindowMin)

        val now = ZonedDateTime.of(2026, 8, 19, 12, 0, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        val planned = TimezoneScheduler.planNight(settings, SchedulerState(), now, originalZone)

        assertTrue(planned.valid)
        // 开始偏移：8/19 22:30（北京时间）
        val expectedShift = ZonedDateTime.of(2026, 8, 19, 22, 30, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        assertEquals(expectedShift, planned.shiftEpoch)
        // 恢复：8/20 04:30（真实），显示 06:30
        val expectedRestore = ZonedDateTime.of(2026, 8, 20, 4, 30, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        assertEquals(expectedRestore, planned.restoreEpoch)

        // 状态更新：当前偏移 120、已武装、下次时刻已记录
        assertEquals(120, planned.offsetMin)
        assertEquals(120, planned.state.currentOffsetMin)
        assertTrue(planned.state.armed)
        assertEquals(expectedShift, planned.state.nextShiftEpoch)
        assertEquals(expectedRestore, planned.state.nextRestoreEpoch)
    }

    @Test
    fun `re-arm same night picks up changed FIXED offset`() {
        val now = ZonedDateTime.of(2026, 8, 19, 12, 0, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        val s1 = SleepShiftSettings(enabled = true, startTimeMin = 22 * 60 + 30, restoreTimeMin = 6 * 60 + 30, offsetMin = 45)
        val p1 = TimezoneScheduler.planNight(s1, SchedulerState(), now, originalZone)
        assertEquals(45, p1.offsetMin)
        // 同一晚重新武装：FIXED 按新设置重算（不推进策略进度）
        val s2 = s1.copy(offsetMin = 180)
        val p2 = TimezoneScheduler.planNight(s2, p1.state, now, originalZone)
        assertEquals(180, p2.offsetMin)
        assertEquals(180, p2.state.currentOffsetMin)
        // 不重复推进
        assertEquals(p1.state.gradualProgressDays, p2.state.gradualProgressDays)
    }

    @Test
    fun `planNight - invalid when disabled`() {
        val settings = SleepShiftSettings(enabled = false)
        val planned = TimezoneScheduler.planNight(settings, SchedulerState(), 0L, originalZone)
        assertFalse(planned.valid)
    }

    @Test
    fun `previous shift epoch returns today's start when now is after start`() {
        // now = 23:00 > 22:30 → 上一次开始偏移是今天 22:30
        val now = ZonedDateTime.of(2026, 8, 19, 23, 0, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        val prev = TimezoneScheduler.computePreviousShiftEpoch(22 * 60 + 30, originalZone, now)
        val expected = ZonedDateTime.of(2026, 8, 19, 22, 30, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        assertEquals(expected, prev)
    }

    @Test
    fun `shift epoch wraps to next day when past today start`() {
        // now = 23:00 > 22:30 → 下一次开始偏移是明天 22:30
        val now = ZonedDateTime.of(2026, 8, 19, 23, 0, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        val shiftEpoch = TimezoneScheduler.computeNextShiftEpoch(22 * 60 + 30, originalZone, now)
        val expected = ZonedDateTime.of(2026, 8, 20, 22, 30, 0, 0, ZoneId.of(originalZone)).toInstant().toEpochMilli()
        assertEquals(expected, shiftEpoch)
    }

    @Test
    fun `dynamic zone id - whole hour uses IANA Etc, fractional falls back to GMT offset`() {
        // 整小时总偏移 → IANA Etc/GMT±H（本平台 setTimeZone 仅应用 tzdb 内 ID）
        assertEquals("Etc/GMT-10", TimezoneScheduler.buildShiftZoneId(originalZone, 120)) // +8h+2h=+10
        assertEquals("Etc/GMT-11", TimezoneScheduler.buildShiftZoneId(originalZone, 180)) // +8h+3h=+11
        assertEquals("Etc/GMT-8", TimezoneScheduler.buildShiftZoneId(originalZone, 0))    // +8h
        // 分数分钟偏移 → 回退自定义 GMT±HH:MM（平台可能不应用）
        assertEquals("GMT+10:15", TimezoneScheduler.buildShiftZoneId(originalZone, 135))
    }

    @Test
    fun `gradual strategy advances night by night up to target`() {
        val settings = SleepShiftSettings(mode = SleepShiftMode.GRADUAL, offsetMin = 120, gradualStepMin = 30)
        val night1 = GradualStrategy.next(settings, SchedulerState())
        assertEquals(30, night1.offsetMin)
        val night2 = GradualStrategy.next(settings, night1.state)
        assertEquals(60, night2.offsetMin)
        val night3 = GradualStrategy.next(settings, night2.state)
        assertEquals(90, night3.offsetMin)
        val night4 = GradualStrategy.next(settings, night3.state)
        assertEquals(120, night4.offsetMin)
    }
}
