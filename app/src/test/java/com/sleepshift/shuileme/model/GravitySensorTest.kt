package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GravitySensorTest {

    @Test
    fun `无传感器降级为模拟重力`() {
        val g = resolveGravity(null)
        assertEquals(0f, g.first, 0f)
        assertTrue("默认应下坠", g.second > 0f)
    }

    @Test
    fun `倾斜影响X方向`() {
        assertTrue(resolveGravity(0.5f to 0f).first > 0f)
        assertTrue(resolveGravity(-0.5f to 0f).first < 0f)
    }

    @Test
    fun `倾斜影响Y方向`() {
        val up = resolveGravity(0f to 0.5f).second
        val down = resolveGravity(0f to -0.5f).second
        assertTrue(up > down)
    }

    @Test
    fun `重力随传感器实时更新`() {
        val g1 = resolveGravity(0.2f to 0.1f)
        val g2 = resolveGravity(0.8f to -0.3f)
        assertTrue(g1 != g2)
    }
}
