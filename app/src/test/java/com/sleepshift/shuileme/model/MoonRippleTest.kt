package com.sleepshift.shuileme.model

import org.junit.Assert.assertTrue
import org.junit.Test

class MoonRippleTest {

    @Test
    fun `呼吸相位在0到1循环`() {
        assertTrue(MoonRipple.breathPhase(0L) in 0f..1f)
        assertTrue(MoonRipple.breathPhase(2000L) in 0f..1f)
        assertTrue(MoonRipple.breathPhase(4000L) in 0f..1f)
        assertTrue(MoonRipple.breathPhase(8000L) < 0.01f || MoonRipple.breathPhase(8000L) > 0.99f)
    }

    @Test
    fun `波纹半径0到1扩散`() {
        assertTrue(MoonRipple.radius(0f) in 0f..1f)
        assertTrue(MoonRipple.radius(1f) in 0f..1f)
    }

    @Test
    fun `常驻波纹透明度低且渐隐`() {
        assertTrue("透明度应低", MoonRipple.alpha(0f) <= 0.15f)
        assertTrue("扩散渐隐", MoonRipple.alpha(1f) < MoonRipple.alpha(0f))
    }

    @Test
    fun `点击增强波纹短促衰减`() {
        assertTrue(MoonRipple.clickBoost(0L) > 0.9f)
        assertTrue(MoonRipple.clickBoost(900L) < 0.01f)
        assertTrue(MoonRipple.clickBoost(450L) < MoonRipple.clickBoost(0L))
    }
}
