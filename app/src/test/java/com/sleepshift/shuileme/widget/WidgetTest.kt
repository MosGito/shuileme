package com.sleepshift.shuileme.widget

import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.OffsetMode
import com.sleepshift.shuileme.model.MoonProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetTest {

    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-08-21T22:30:00Z").toEpochMilli()

    private fun awakeState(offset: Int = 120, growth: Int = 40) = ShuilemeState(
        currentOffsetMin = offset,
        mode = OffsetMode.FIXED,
        streakDays = 3,
        sleepCount = 12,
        moonProgress = MoonProgress(growthPercent = growth),
    )

    @Test
    fun `2x1 小组件显示核心字段`() {
        val s = awakeState()
        assertEquals("睡了么", ShuilemeWidgetDisplay.title())
        // 22:30 + 2h = 00:30（虚拟时间）
        assertEquals("00:30", ShuilemeWidgetDisplay.virtualTimeText(s, now, zone))
        assertTrue(ShuilemeWidgetDisplay.moonEmoji(s, now, zone).isNotBlank())
        assertTrue(ShuilemeWidgetDisplay.statusLine(s, now, zone).isNotBlank())
    }

    @Test
    fun `4x2 中组件显示成长与连续与快捷`() {
        val s = awakeState(growth = 40)
        assertTrue(ShuilemeWidgetDisplay.growthText(s).contains("40%"))
        assertTrue(ShuilemeWidgetDisplay.streakText(s).contains("连续 3 天"))
        // 一键文案
        assertEquals("🌙 好啦，月亮开始成长了", ShuilemeWidgetDisplay.sleepConfirmationText())
        assertEquals("☀️ 醒啦！月亮记住了", ShuilemeWidgetDisplay.wakeConfirmationText())
    }

    @Test
    fun `组件状态回退为默认仍可渲染`() {
        // loadWidgetState 失败时回退 ShuilemeState()（防御性），显示默认字段
        val s = ShuilemeState()
        assertTrue(ShuilemeWidgetDisplay.virtualTimeText(s, now, zone).isNotBlank())
        assertTrue(ShuilemeWidgetDisplay.statusLine(s, now, zone).isNotBlank())
    }
}
