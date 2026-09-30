package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.0.7 §4.6 Confidence 公式测试（含规范 sanity 表：n=5/8/14/20 × R=0/0.5/1）。
 */
class ConfidenceTest {

    @Test
    fun `sampleFactor - n 小于等于 14 线性 之后封顶为 1`() {
        assertEquals(0.0, Confidence.sampleFactor(0), 1e-12)
        assertEquals(5.0 / 14.0, Confidence.sampleFactor(5), 1e-12)
        assertEquals(8.0 / 14.0, Confidence.sampleFactor(8), 1e-12)
        assertEquals(1.0, Confidence.sampleFactor(14), 1e-12)
        assertEquals(1.0, Confidence.sampleFactor(20), 1e-12)
        assertEquals(1.0, Confidence.sampleFactor(100), 1e-12)
    }

    @Test
    fun `baseConfidence - 规范 sanity 表（completeness 为 1）`() {
        // n=5
        assertEquals(0.4071428571428572, Confidence.baseConfidence(5, 0.0, 1.0), 1e-9)
        assertEquals(0.5821428571428572, Confidence.baseConfidence(5, 0.5, 1.0), 1e-9)
        assertEquals(0.7571428571428572, Confidence.baseConfidence(5, 1.0, 1.0), 1e-9)
        // n=8
        assertEquals(0.4714285714285714, Confidence.baseConfidence(8, 0.0, 1.0), 1e-9)
        assertEquals(0.6464285714285715, Confidence.baseConfidence(8, 0.5, 1.0), 1e-9)
        assertEquals(0.8214285714285714, Confidence.baseConfidence(8, 1.0, 1.0), 1e-9)
        // n=14 / 20（样本项封顶）
        assertEquals(0.60, Confidence.baseConfidence(14, 0.0, 1.0), 1e-9)
        assertEquals(0.775, Confidence.baseConfidence(14, 0.5, 1.0), 1e-9)
        assertEquals(0.95, Confidence.baseConfidence(14, 1.0, 1.0), 1e-9)
        assertEquals(Confidence.baseConfidence(14, 1.0, 1.0), Confidence.baseConfidence(20, 1.0, 1.0), 1e-12)
    }

    @Test
    fun `alignPenalty - startDeviation 环形距离除以 1200 且上限 0_20`() {
        // circDist(02:00=120, 23:00=1380) = 180 → 180/1200 = 0.15
        assertEquals(0.15, Confidence.alignPenalty(120, 1380), 1e-12)
        // dev=720 → 0.6 → clamp 0.20
        assertEquals(0.20, Confidence.alignPenalty(0, 720), 1e-12)
        // 目标缺失或 μ_C 缺失 → 0（安全降级）
        assertEquals(0.0, Confidence.alignPenalty(120, null), 1e-12)
        assertEquals(0.0, Confidence.alignPenalty(null, 1380), 1e-12)
    }

    @Test
    fun `compute - 公式与 clamp`() {
        // n=14, R=1, comp=1, dev=0 → 0.95（上限）
        assertEquals(0.95, Confidence.compute(14, 1.0, 1.0, 120, 120)!!, 1e-9)
        // dev=180 → 0.95 − 0.15 = 0.80
        assertEquals(0.80, Confidence.compute(14, 1.0, 1.0, 120, 1380)!!, 1e-9)
        // 下限 clamp：base=0.2（n=0, R=0, comp=0），penalty=0.2 → 0.0
        assertEquals(0.0, Confidence.compute(0, 0.0, 0.0, 0, 720)!!, 1e-12)
    }

    @Test
    fun `compute - R undefined 返回 null`() {
        assertNull(Confidence.compute(5, null, 1.0, 120, 1380))
    }

    @Test
    fun `compute - 随 n R completeness 单调不减`() {
        val base1 = Confidence.compute(5, 0.5, 1.0, 120, 1380)!!
        val base2 = Confidence.compute(14, 0.5, 1.0, 120, 1380)!!
        val base3 = Confidence.compute(14, 0.8, 1.0, 120, 1380)!!
        val base4 = Confidence.compute(14, 0.8, 0.5, 120, 1380)!!
        assertTrue(base2 >= base1)
        assertTrue(base3 >= base2)
        assertTrue(base4 <= base3)
        assertTrue(Confidence.compute(14, 1.0, 1.0, 120, 1380)!! <= 0.95)
    }

    @Test
    fun `compute - n 小于 5 仍按公式（Matcher 负责门槛）`() {
        assertNotNull(Confidence.compute(4, 0.5, 1.0, 120, 1380))
    }

    @Test
    fun `compute - completeness 越高 confidence 越高`() {
        val low = Confidence.compute(14, 0.5, 0.0, 120, 1380)!!
        val high = Confidence.compute(14, 0.5, 1.0, 120, 1380)!!
        assertTrue(high > low)
    }
}
