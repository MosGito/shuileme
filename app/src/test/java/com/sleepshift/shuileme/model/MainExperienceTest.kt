package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `气泡受重力下落并在底部弹跳且不越出可视边界`() {
        val maxY = 200f - PersonaBubblePhysics.BUBBLE_SIZE
        val physics = PersonaBubblePhysics(
            width = 100f, height = 200f,
            bubbles = listOf(Bubble(x = 10f, y = 10f, vy = 0f, emoji = "🌙")),
        )
        var p = physics
        repeat(200) { p = p.step(dt = 0.1f) }
        val b = p.bubbles.first()
        assertTrue("y 应 >= 0", b.y >= 0f)
        assertTrue("y 应 <= maxY（不出界）", b.y <= maxY)
        // 四壁反射后速度不恒为 0（弹跳）
        var p2 = physics.copy(bubbles = listOf(Bubble(x = 1f, y = 1f, vx = 8f, vy = 0f, emoji = "🌙")))
        repeat(10) { p2 = p2.step(dt = 0.1f) }
        val b2 = p2.bubbles.first()
        assertTrue(b2.x in 0f..(100f - PersonaBubblePhysics.BUBBLE_SIZE))
    }

    @Test
    fun `散布确定性且坐标在四壁边界内`() {
        val emojis = listOf("🌙", "🐱", "🌞")
        val a = PersonaBubblePhysics.scatter(emojis, 100f, 200f, seed = 7)
        val b = PersonaBubblePhysics.scatter(emojis, 100f, 200f, seed = 7)
        assertEquals(a, b)
        val maxX = 100f - PersonaBubblePhysics.BUBBLE_SIZE
        val maxY = 200f - PersonaBubblePhysics.BUBBLE_SIZE
        a.forEach { bubble ->
            assertTrue("x=${bubble.x} 应在 [0,$maxX]", bubble.x in 0f..maxX)
            assertTrue("y=${bubble.y} 应在 [0,$maxY]", bubble.y in 0f..maxY)
        }
        // 不同 seed 散布不同
        val c = PersonaBubblePhysics.scatter(emojis, 100f, 200f, seed = 8)
        assertTrue(a != c)
    }

    // ── SleepGestureTrigger（SL-9 蓄力状态机）──

    @Test
    fun `点击到蓄力再到完成一圈`() {
        var g = SleepGestureTrigger()
        g = g.onPress()
        assertEquals(SleepGestureState.PRESSED, g.state)
        g = g.onLongPress()
        assertEquals(SleepGestureState.CHARGING, g.state)
        assertEquals(0f, g.chargeProgress, 0f)
        g = g.onCharge(0.5f)
        assertEquals(0.5f, g.chargeProgress, 0.001f)
        g = g.onCharge(1.5f) // 钳制
        assertEquals(1f, g.chargeProgress, 0.001f)
        g = g.onChargeComplete()
        assertEquals(SleepGestureState.COMPLETED, g.state)
        assertEquals(1f, g.chargeProgress, 0f)
    }

    @Test
    fun `取消回到IDLE且进度清零`() {
        val g = SleepGestureTrigger().onPress().onLongPress().onCharge(0.6f).onCancel()
        assertEquals(SleepGestureState.IDLE, g.state)
        assertEquals(0f, g.chargeProgress, 0f)
    }

    @Test
    fun `未进入蓄力不能直接完成`() {
        val g = SleepGestureTrigger().onPress().onChargeComplete()
        assertEquals(SleepGestureState.PRESSED, g.state) // 未 CHARGING 不完成
    }

    // ── SL-9.1 ──

    @Test
    fun `牛马型单emoji排版不影响布局`() {
        assertEquals("🐮", PersonalityCardGenerator.personalityPairEmoji(SleepPersonalityType.WORK_HORSE))
        assertFalse(PersonalityCardGenerator.personalityPairEmoji(SleepPersonalityType.WORK_HORSE).contains("🐴"))
        assertEquals("🌙🐱", PersonalityCardGenerator.personalityPairEmoji(SleepPersonalityType.NIGHT_OWL))
    }

    @Test
    fun `气泡不越过月亮窗台`() {
        val maxY = 200f - PersonaBubblePhysics.BUBBLE_SIZE - 40f
        val physics = PersonaBubblePhysics(
            width = 100f, height = 200f, insetBottom = 40f,
            bubbles = listOf(Bubble(x = 10f, y = 10f, vy = 0f, emoji = "🌙")),
        )
        var p = physics
        repeat(200) { p = p.step(dt = 0.1f) }
        val b = p.bubbles.first()
        assertTrue("y=${b.y} 应 <= 窗台上界 $maxY", b.y <= maxY)
    }
}
