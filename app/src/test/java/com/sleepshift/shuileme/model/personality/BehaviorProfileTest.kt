package com.sleepshift.shuileme.model.personality

import com.sleepshift.shuileme.model.SleepPersonalityEngine
import com.sleepshift.shuileme.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * V2.0.7 §4.1 / §4.5 / §4.8 / §11.1 / §23（#1、#60、#61、#44、#45）特征层聚合测试。
 */
class BehaviorProfileTest {

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun session(startHour: Int, startMin: Int, durMin: Long, wake: Boolean = true): SleepSession {
        val start = at(startHour, startMin)
        return SleepSession(start, if (wake) start + durMin * 60_000L else null)
    }

    @Test
    fun `effectiveSessions - 具备有效 start 的会话数（#44）`() {
        assertEquals(5, SleepBehaviorProfile.effectiveSessions(List(5) { session(23, 0, 480) }))
        assertEquals(0, SleepBehaviorProfile.effectiveSessions(emptyList()))
    }

    @Test
    fun `profile - 跨午夜 onset 环形平均 与 R 三分量（#1）`() {
        val sessions = listOf(
            session(23, 50, 460), session(0, 10, 460),
            session(23, 55, 460), session(0, 5, 460),
            session(23, 50, 460),
        )
        val p = SleepBehaviorProfile.from(sessions)
        assertEquals(5, p.effectiveSessionCount)
        assertNotNull(p.meanOnsetMin)
        assertTrue(SleepPersonalityEngine.circularDistance(p.meanOnsetMin!!, 0) <= 6) // 接近午夜
        assertNotNull(p.meanWakeMin)
        val r = p.regularity
        assertNotNull(r)
        assertTrue(r!! >= 0.9) // 规律度高
        assertEquals(1.0, p.completeness, 1e-9)
        // μ_C 环形均值接近 00:00（可能落在 1438 ≈ 23:58），C 应为接近 0 的 Value
        val c = p.chronotype
        assertTrue("chronotype=$c 应为 Value", c is ChronotypeState.Value)
        assertTrue("C=${(c as ChronotypeState.Value).score} 应接近 0", abs(c.score) <= 0.01)
    }

    @Test
    fun `profile - 无 wake 时 R 等于 r_onset（§4_5）`() {
        val sessions = List(5) { session(2, 0, 480, wake = false) }
        val p = SleepBehaviorProfile.from(sessions)
        assertEquals(0.0, p.completeness, 1e-9)
        assertNotNull(p.regularity)
        assertEquals(1.0, p.regularity!!, 1e-9) // 完全相同入睡时间 → σ=0 → r_onset=1
    }

    @Test
    fun `profile - degenerate onset 使 R undefined 而非 0（#61）`() {
        // 0 与 720 等量对称（3:3）→ 合成向量 = 0 < 1e-6 → circular mean 退化
        val sessions = (0 until 6).map { i ->
            if (i % 2 == 0) session(0, 0, 480) else session(12, 0, 480)
        }
        val p = SleepBehaviorProfile.from(sessions)
        assertNull(p.meanOnsetMin)
        assertEquals(ChronotypeState.MeanUndefined, p.chronotype)
        assertNull("R 必须为 undefined，禁止自动置 0", p.regularity)
    }

    @Test
    fun `profile - 日间窗口 onset 使 chronotype 为 OutOfDomain 但 R 不受影响（#61）`() {
        // 5 条 onset 全部落在 09:00–18:00 → ratio=1.0 → daytimeOnset=TRUE；chronotype=OutOfDomain
        val sessions = listOf(
            session(10, 0, 480), session(11, 0, 480), session(12, 0, 480),
            session(13, 0, 480), session(14, 0, 480),
        )
        val p = SleepBehaviorProfile.from(sessions)
        assertEquals(ChronotypeState.OutOfDomain, p.chronotype)
        assertEquals(DaytimeOnsetState.TRUE, p.daytimeOnset)
        // R 由 Regularity 自身数据质量决定，不受 C undefined 影响（此处 R 可计算且 > 0）
        val r = p.regularity
        assertNotNull(r)
        assertTrue("R=$r 应可计算且 > 0", r!! > 0.0)
    }

    @Test
    fun `profile - duration 算术平均与 D 派生（#3）`() {
        val sessions = listOf(
            session(22, 0, 480), session(22, 0, 490),
            session(22, 0, 475), session(22, 0, 485), session(22, 0, 470),
        )
        val p = SleepBehaviorProfile.from(sessions)
        assertEquals(480L, p.meanDurationMin) // (480+490+475+485+470)/5 = 480
        assertEquals(0.0, durationScore(p.meanDurationMin!!), 1e-9)
    }

    @Test
    fun `profile - daytimeOnset 三态（#39 #43）`() {
        val dayMajority = List(3) { session(10, 0, 480) } + List(2) { session(23, 0, 480) }
        val p = SleepBehaviorProfile.from(dayMajority)
        assertEquals(0.6, p.daytimeOnsetRatio, 1e-9)
        assertEquals(DaytimeOnsetState.TRUE, p.daytimeOnset)

        val insufficient = List(4) { session(10, 0, 480) }
        val p2 = SleepBehaviorProfile.from(insufficient)
        assertEquals(DaytimeOnsetState.UNKNOWN, p2.daytimeOnset) // n<5 → UNKNOWN，非 FALSE
    }

    @Test
    fun `profile - 三分量公式权重（#60 相关）`() {
        // 构造 onset 稳定、wake 稳定、duration 有小波动的会话
        val sessions = listOf(
            session(23, 0, 480), session(23, 0, 490),
            session(23, 0, 480), session(23, 0, 490), session(23, 0, 480),
        )
        val p = SleepBehaviorProfile.from(sessions)
        assertNotNull(p.regularity)
        // onset σ=0 → r_onset=1；wake σ=0 → r_wake=1；duration σ=(0,10,0,10,0) → r_dur < 1
        // R = 0.5*1 + 0.3*1 + 0.2*r_dur ∈ [0.8, 1.0]
        assertTrue("R=${p.regularity}", p.regularity!! in 0.8..1.0)
    }
}
