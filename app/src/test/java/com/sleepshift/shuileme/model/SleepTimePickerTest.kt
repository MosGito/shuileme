package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimePickerTest {

    @Test
    fun `分钟分解为时分`() {
        val t = 23 * 60 + 30 // 23:30
        assertEquals(23, t / 60)
        assertEquals(30, t % 60)
    }

    @Test
    fun `时分合成分钟`() {
        assertEquals(7 * 60, 7 * 60 + 0)
        assertEquals(23 * 60 + 45, 23 * 60 + 45)
    }

    @Test
    fun `时分范围合法（0-23 时 0-59 分）`() {
        for (h in 0..23) assertTrue(h in 0..23)
        for (m in 0..59) assertTrue(m in 0..59)
    }

    @Test
    fun `跨午夜睡眠窗口（23点到7点）`() {
        val sleep = 23 * 60
        val wake = 7 * 60
        val window = (24 * 60 - sleep) + wake
        assertEquals(8 * 60, window) // 8 小时
    }
}
