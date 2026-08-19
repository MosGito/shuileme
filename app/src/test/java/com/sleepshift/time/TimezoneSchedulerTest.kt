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
    fun `planNight - invalid when disabled`() {
        val settings = SleepShiftSettings(enabled = false)
        val planned = TimezoneScheduler.planNight(settings, SchedulerState(), 0L, originalZone)
        assertFalse(planned.valid)
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
    fun `dynamic zone id follows 15min offset step`() {
        assertEquals("GMT+10:00", TimezoneScheduler.buildShiftZoneId(originalZone, 120))
        assertEquals("GMT+10:15", TimezoneScheduler.buildShiftZoneId(originalZone, 135))
        assertEquals("GMT+11:00", TimezoneScheduler.buildShiftZoneId(originalZone, 180))
        assertEquals("GMT+08:00", TimezoneScheduler.buildShiftZoneId(originalZone, 0))
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
