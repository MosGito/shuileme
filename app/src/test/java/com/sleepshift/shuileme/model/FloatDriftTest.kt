package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SL-9.6：失重漂浮物理测试（无重力、无传感器，纯函数确定性漂移） */
class FloatDriftTest {

    private fun physics(width: Float = 300f, height: Float = 300f) = PersonaBubblePhysics(
        width = width,
        height = height,
        bubbles = listOf(Bubble(x = 150f, y = 150f, emoji = "🌙")),
    )

    @Test
    fun `无重力下气泡持续漂移且速度缓慢`() {
        var p = physics()
        repeat(60) { p = p.step(dt = 0.016f) } // 约 1 秒
        val b = p.bubbles[0]
        assertTrue("应发生漂移 (x=${b.x}, y=${b.y})", b.x != 150f || b.y != 150f)
        assertTrue("速度应缓慢 (vx=${b.vx}, vy=${b.vy})", kotlin.math.abs(b.vx) < 60f && kotlin.math.abs(b.vy) < 60f)
    }

    @Test
    fun `失重漂浮围绕初始位置往返`() {
        // 无重力：气泡沿漂移曲线往返移动（非只下坠），位置变化幅度可见
        var p = physics()
        var minY = p.bubbles[0].y
        var maxY = p.bubbles[0].y
        repeat(400) {
            p = p.step(dt = 0.05f)
            minY = minOf(minY, p.bubbles[0].y)
            maxY = maxOf(maxY, p.bubbles[0].y)
        }
        assertTrue("应在窗框内往返移动 range=$maxY-$minY", maxY - minY > 10f)
    }

    @Test
    fun `相同初始状态漂移结果确定性一致`() {
        var pa = physics()
        var pb = physics()
        repeat(50) {
            pa = pa.step(dt = 0.03f)
            pb = pb.step(dt = 0.03f)
        }
        assertEquals(pa.bubbles[0].x, pb.bubbles[0].x, 0.001f)
        assertEquals(pa.bubbles[0].y, pb.bubbles[0].y, 0.001f)
    }

    @Test
    fun `拖动中的气泡不漂移`() {
        var p = physics().moveBubble(0, 50f, 50f, drag = true)
        repeat(50) { p = p.step(dt = 0.05f) }
        assertEquals(50f, p.bubbles[0].x, 0.001f)
        assertEquals(50f, p.bubbles[0].y, 0.001f)
    }
}
