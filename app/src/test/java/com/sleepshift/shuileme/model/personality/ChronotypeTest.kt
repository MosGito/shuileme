package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.0.7 §4.3 / §4.4 / §23（#2、#57、#58、#3）特征层测试。
 */
class ChronotypeTest {

    @Test
    fun `夜间弧 - 覆盖 1800-2400 与 0000-0900 且窗口排他`() {
        assertTrue(isInNightArc(0))
        assertTrue(isInNightArc(539))   // 08:59
        assertFalse(isInNightArc(540))  // 09:00
        assertFalse(isInNightArc(1079)) // 17:59
        assertTrue(isInNightArc(1080))  // 18:00
        assertTrue(isInNightArc(1439))  // 23:59
    }

    @Test
    fun `C 数值 - 夜间弧内映射表（#2）`() {
        assertEquals(-1.0, scoreAt(18, 0), 1e-9)
        assertEquals(-1.0, scoreAt(20, 0), 1e-9)
        assertEquals(-0.5, scoreAt(22, 0), 1e-9)
        assertEquals(-1.0 / 3.0, scoreAt(22, 40), 1e-9)
        assertEquals(-0.25, scoreAt(23, 0), 1e-9)
        assertEquals(0.0, scoreAt(0, 0), 1e-9)
        assertEquals(0.25, scoreAt(1, 0), 1e-9)
        assertEquals(1.0 / 3.0, scoreAt(1, 20), 1e-9)
        assertEquals(0.5, scoreAt(2, 0), 1e-9)
        assertEquals(1.0, scoreAt(4, 0), 1e-9)
        assertEquals(1.0, scoreAt(8, 59), 1e-9)
    }

    @Test
    fun `C 域外 - 日间窗口为 OutOfDomain 而非数值（#57 #58）`() {
        assertEquals(ChronotypeState.OutOfDomain, chronotypeScore(17 * 60 + 59)) // 17:59
        assertEquals(ChronotypeState.OutOfDomain, chronotypeScore(9 * 60))       // 09:00
        assertEquals(ChronotypeState.OutOfDomain, chronotypeScore(11 * 60 + 59)) // 11:59
        assertEquals(ChronotypeState.OutOfDomain, chronotypeScore(12 * 60))      // 12:00
        assertEquals(ChronotypeState.OutOfDomain, chronotypeScore(12 * 60 + 1))  // 12:01
    }

    @Test
    fun `C 退化 - null 为 MeanUndefined 而非 C=0 fallback（#59）`() {
        assertEquals(ChronotypeState.MeanUndefined, chronotypeScore(null))
    }

    @Test
    fun `C 数值 - 无 NaN 且范围恒在 -1 到 1`() {
        for (m in listOf(0, 60, 120, 240, 480, 539, 1080, 1200, 1380, 1439)) {
            val state = chronotypeScore(m)
            if (state is ChronotypeState.Value) {
                assertTrue("C=${state.score} 越界", state.score in -1.0..1.0)
                assertFalse(state.score.isNaN())
            }
        }
    }

    @Test
    fun `D - DurationScore 映射（#3）`() {
        assertEquals(-1.0, durationScore(6 * 60), 1e-9)
        assertEquals(-1.0 / 3.0, durationScore(7 * 60 + 20), 1e-9)
        assertEquals(0.0, durationScore(8 * 60), 1e-9)
        assertEquals(1.0 / 3.0, durationScore(8 * 60 + 40), 1e-9)
        assertEquals(1.0, durationScore(10 * 60), 1e-9)
    }

    private fun scoreAt(hour: Int, minute: Int): Double =
        (chronotypeScore(hour * 60 + minute) as ChronotypeState.Value).score
}
