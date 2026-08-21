package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleBoundaryTest {

    private fun physicsWith(width: Float = 200f, height: Float = 400f) = PersonaBubblePhysics(
        width = width,
        height = height,
        insetBottom = 40f,
        bubbles = listOf(Bubble(x = 10f, y = 10f, vy = 0f, vx = 0f, emoji = "🌙")),
    )

    @Test
    fun `气泡被重力推到边界内不越界`() {
        var p = physicsWith()
        repeat(300) { p = p.step(gravityY = 5f, dt = 0.1f) }
        val b = p.bubbles.first()
        val maxY = 400f - PersonaBubblePhysics.BUBBLE_SIZE - 40f
        assertTrue("y=${b.y} <= $maxY", b.y <= maxY)
        assertTrue("y=${b.y} >= 0", b.y >= 0f)
        assertTrue("x=${b.x} 在边界内", b.x in 0f..(200f - PersonaBubblePhysics.BUBBLE_SIZE))
    }

    @Test
    fun `moveBubble 钳制到窗框边界`() {
        val p = physicsWith()
        // 拖到超出边界 → 钳制
        val moved = p.moveBubble(0, 999f, 999f, drag = true)
        assertTrue(moved.bubbles[0].x <= 200f - PersonaBubblePhysics.BUBBLE_SIZE)
        assertTrue(moved.bubbles[0].y <= 400f - PersonaBubblePhysics.BUBBLE_SIZE - 40f)
    }

    @Test
    fun `冻结的气泡不受重力影响`() {
        var p = physicsWith().moveBubble(0, 50f, 50f, drag = true)
        val before = p.bubbles[0]
        repeat(50) { p = p.step(gravityY = 5f, dt = 0.1f) }
        val after = p.bubbles[0]
        assertEquals(before.x, after.x, 0.01f)
        assertEquals(before.y, after.y, 0.01f) // 拖动中不落
    }

    @Test
    fun `松手后带速度漂移并弹跳`() {
        var p = physicsWith().moveBubble(0, 50f, 50f, vx = 30f, vy = 20f, drag = false)
        repeat(30) { p = p.step(gravityY = 5f, dt = 0.1f) }
        val b = p.bubbles[0]
        assertTrue("应移动", b.x != 50f || b.y != 50f)
    }

    @Test
    fun `碰撞推开重叠气泡`() {
        val p = PersonaBubblePhysics(
            width = 200f, height = 200f,
            bubbles = listOf(
                Bubble(x = 50f, y = 50f, emoji = "🌙"),
                Bubble(x = 52f, y = 50f, emoji = "🐱"),
            ),
        )
        val stepped = p.step(gravityY = 0f, dt = 0.016f)
        val dx = stepped.bubbles[1].x - stepped.bubbles[0].x
        assertTrue("重叠应被推开 dx=$dx", dx >= PersonaBubblePhysics.BUBBLE_SIZE * 1.2f - 1f)
    }
}
