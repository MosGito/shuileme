package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainExperienceTest {

    // ── HomeLayoutState ──

    @Test
    fun `首页布局仅含5区 无统计或开发信息`() {
        val layout = HomeLayoutState()
        assertEquals(HomeLayoutState.EXPECTED_SECTIONS, layout.visibleSections)
        assertEquals(5, layout.visibleSections.size)
        assertTrue(HomeSection.entries.none { it.name == "STATISTICS" || it.name == "DEV_INFO" })
        assertTrue(HomeSection.PERSONALITY in layout.visibleSections)
        assertTrue(HomeSection.MOON in layout.visibleSections)
        assertTrue(HomeSection.BUBBLES in layout.visibleSections)
    }

    // ── MoonGlowState ──

    @Test
    fun `光效强度 - 健康增强 普通柔光 低状态减弱`() {
        assertEquals(1f, MoonGlow.glowIntensity(MoonLife(stage = MoonStage.FULL, mood = MoonMood.CELEBRATING)), 0f)
        assertEquals(1f, MoonGlow.glowIntensity(MoonLife(mood = MoonMood.CELEBRATING)), 0f)
        assertEquals(0.7f, MoonGlow.glowIntensity(MoonLife(mood = MoonMood.GENTLE)), 0f)
        assertEquals(0.3f, MoonGlow.glowIntensity(MoonLife(mood = MoonMood.DISAPPOINTED)), 0f)
        assertEquals(0.5f, MoonGlow.glowIntensity(MoonLife(mood = MoonMood.EXPECTANT)), 0f)
    }

    @Test
    fun `光效随强度单调 - alpha scale blur 递增`() {
        val low = 0.3f
        val high = 1f
        assertTrue(MoonGlow.glowAlpha(high) > MoonGlow.glowAlpha(low))
        assertTrue(MoonGlow.glowScale(high) > MoonGlow.glowScale(low))
        assertTrue(MoonGlow.glowBlur(high) > MoonGlow.glowBlur(low))
    }

    // ── PersonaBubblePhysics ──

    @Test
    fun `气泡受重力下落并在底部弹跳`() {
        val physics = PersonaBubblePhysics(
            width = 100f, height = 200f,
            bubbles = listOf(Bubble(x = 10f, y = 10f, vy = 0f, emoji = "🌙")),
        )
        // 多步后 y 应增大（下落）并最终不越界
        var p = physics
        repeat(200) { p = p.step(gravityY = 5f, dt = 0.1f) }
        val b = p.bubbles.first()
        assertTrue("y 应 >= 0", b.y >= 0f)
        assertTrue("y 应 <= height", b.y <= physics.height)
    }

    @Test
    fun `散布确定性且坐标在边界内`() {
        val emojis = listOf("🌙", "🐱", "🌞")
        val a = PersonaBubblePhysics.scatter(emojis, 100f, 200f, seed = 7)
        val b = PersonaBubblePhysics.scatter(emojis, 100f, 200f, seed = 7)
        assertEquals(a, b)
        a.forEach { bubble ->
            assertTrue(bubble.x in 0f..100f)
            assertTrue(bubble.y in 0f..200f)
        }
        // 不同 seed 散布不同
        val c = PersonaBubblePhysics.scatter(emojis, 100f, 200f, seed = 8)
        assertTrue(a != c)
    }

    // ── SleepGestureTrigger ──

    @Test
    fun `长按达标进入READY 短按不误触`() {
        var g = SleepGestureTrigger()
        g = g.onDown(0L)
        assertEquals(SleepGestureState.PRESSED, g.state)
        g = g.onTick(300L, longPressThreshold = 500L) // 未达标
        assertEquals(SleepGestureState.PRESSED, g.state)
        g = g.onTick(600L, longPressThreshold = 500L) // 达标
        assertEquals(SleepGestureState.READY, g.state)
    }

    @Test
    fun `确认与取消`() {
        var g = SleepGestureTrigger().onDown(0L).onTick(600L, 500L)
        g = g.onConfirm()
        assertEquals(SleepGestureState.CONFIRMED, g.state)
        // 非 READY 不能确认
        var g2 = SleepGestureTrigger().onDown(0L)
        g2 = g2.onConfirm()
        assertEquals(SleepGestureState.PRESSED, g2.state)
        // 取消回到 IDLE
        var g3 = SleepGestureTrigger().onDown(0L).onTick(600L, 500L).onCancel()
        assertEquals(SleepGestureState.IDLE, g3.state)
    }
}
