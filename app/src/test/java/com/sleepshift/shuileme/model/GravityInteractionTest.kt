package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GravityInteractionTest {

    @Test
    fun `倾斜向左气泡向左漂`() {
        // gravityX < 0 → x 减小（向左）
        val p = PersonaBubblePhysics(width = 300f, height = 300f, bubbles = listOf(Bubble(x = 150f, y = 50f, emoji = "🌙")))
        var left = p
        repeat(40) { left = left.step(gravityX = -200f, gravityY = 0f, dt = 0.05f) }
        assertTrue("应向左漂", left.bubbles[0].x < 150f)

        var right = p
        repeat(40) { right = right.step(gravityX = 200f, gravityY = 0f, dt = 0.05f) }
        assertTrue("应向右漂", right.bubbles[0].x > 150f)
    }

    @Test
    fun `重力实时影响速度`() {
        val p = PersonaBubblePhysics(width = 300f, height = 300f, bubbles = listOf(Bubble(x = 150f, y = 50f, emoji = "🌙")))
        val afterLeft = p.step(gravityX = -100f, gravityY = 0f, dt = 0.1f).bubbles[0]
        val afterRight = p.step(gravityX = 100f, gravityY = 0f, dt = 0.1f).bubbles[0]
        assertTrue(afterLeft.vx < afterRight.vx) // 方向实时变化
    }

    @Test
    fun `无传感器降级为模拟重力`() {
        val g = resolveGravity(null)
        assertEquals(0f, g.first, 0f)
        assertTrue(g.second > 0f)
    }
}
