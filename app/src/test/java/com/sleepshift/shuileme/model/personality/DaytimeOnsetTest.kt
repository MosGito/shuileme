package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * V2.0.7 §4.8 / §23（#6、#9、#34、#36、#37、#38、#39、#40、#41、#42、#43）特征层测试。
 */
class DaytimeOnsetTest {

    @Test
    fun `ratio - 日间窗口 0900-1800 占比`() {
        val onsets = listOf(9 * 60, 10 * 60, 11 * 60, 23 * 60, 0) // 3/5
        assertEquals(0.6, daytimeOnsetRatio(onsets), 1e-9)
        assertEquals(0.4, daytimeOnsetRatio(listOf(9 * 60, 10 * 60, 23 * 60, 0, 1 * 60)), 1e-9)
    }

    @Test
    fun `ratio - 空输入返回 0`() {
        assertEquals(0.0, daytimeOnsetRatio(emptyList()), 1e-9)
    }

    @Test
    fun `state - n 小于 5 恒为 UNKNOWN（#36 #37 #38）`() {
        assertEquals(DaytimeOnsetState.UNKNOWN, daytimeOnsetState(1.0, 4))
        assertEquals(DaytimeOnsetState.UNKNOWN, daytimeOnsetState(1.0, 1))
        assertEquals(DaytimeOnsetState.UNKNOWN, daytimeOnsetState(0.0, 4))
        assertEquals(DaytimeOnsetState.UNKNOWN, daytimeOnsetState(0.0, 0))
    }

    @Test
    fun `state - n 大于等于 5 且 ratio 大于等于 0_60 为 TRUE（含等于，#34 #39 #41）`() {
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(0.6, 5)) // 3/5
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(0.6, 10)) // 6/10
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(0.7, 10)) // #9
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(0.6, 20)) // 12/20
    }

    @Test
    fun `state - n 大于等于 5 且 ratio 小于 0_60 为 FALSE（#40）`() {
        assertEquals(DaytimeOnsetState.FALSE, daytimeOnsetState(0.4, 5)) // 2/5
        assertEquals(DaytimeOnsetState.FALSE, daytimeOnsetState(0.59, 5))
        assertEquals(DaytimeOnsetState.FALSE, daytimeOnsetState(0.5, 10))
    }

    @Test
    fun `ratio 边界 - 0_60 使用大于等于（#42）`() {
        // 3/5、6/10、12/20 均达标；2/5、5/10、11/20 均不达标
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(3.0 / 5.0, 5))
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(6.0 / 10.0, 10))
        assertEquals(DaytimeOnsetState.TRUE, daytimeOnsetState(12.0 / 20.0, 20))
        assertEquals(DaytimeOnsetState.FALSE, daytimeOnsetState(2.0 / 5.0, 5))
        assertEquals(DaytimeOnsetState.FALSE, daytimeOnsetState(5.0 / 10.0, 10))
        assertEquals(DaytimeOnsetState.FALSE, daytimeOnsetState(11.0 / 20.0, 20))
    }
}
