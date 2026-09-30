package com.sleepshift.shuileme.model.personality

import com.sleepshift.shuileme.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * V2.0.7 §20.1 / §23 判定层测试（Gate / Base / Boundary / Secondary / Margin / Transition / Confidence / undefined）。
 */
class PersonalityMatcherTest {

    // ── 工具 ──

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun session(startHour: Int, startMin: Int, durMin: Long): SleepSession {
        val start = at(startHour, startMin)
        return SleepSession(start, start + durMin * 60_000L)
    }

    private fun profile(
        chronotype: ChronotypeState,
        d: Double?,
        regularity: Double?,
        state: DaytimeOnsetState = DaytimeOnsetState.FALSE,
        n: Int = 5,
        completeness: Double = 1.0,
        meanOnsetMin: Int = 120,
    ): SleepBehaviorProfile = SleepBehaviorProfile(
        effectiveSessionCount = n,
        meanOnsetMin = meanOnsetMin,
        meanWakeMin = 480,
        meanDurationMin = d?.let { (480.0 + it * 120.0).toLong() },
        onsetStdDevMin = 10.0,
        wakeStdDevMin = 10.0,
        durationStdDevMin = 5.0,
        regularity = regularity,
        completeness = completeness,
        chronotype = chronotype,
        daytimeOnsetRatio = if (state == DaytimeOnsetState.TRUE) 1.0 else 0.5,
        daytimeOnset = state,
    )

    // ── Special Gate（control-flow，非 score 竞争）──

    @Test
    fun `WOLF Gate - daytimeOnset TRUE 优先于 Base 即使 HOUND 本可得满分`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.9, DaytimeOnsetState.TRUE))
        assertEquals(PersonalityId.WOLF, r.primary?.id)
        assertEquals(PersonalityId.WOLF, r.gate)
        assertNull(r.primaryScore)
        assertNull(r.margin)
        assertFalse(r.isTransitioning)
    }

    @Test
    fun `WOLF Gate - 不依赖 chronotype 数值`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(-1.0), 0.0, 0.9, DaytimeOnsetState.TRUE))
        assertEquals(PersonalityId.WOLF, r.primary?.id)
    }

    @Test
    fun `CHAMELEON Gate - FALSE 且 R 小于 0_33`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.2, DaytimeOnsetState.FALSE))
        assertEquals(PersonalityId.CHAMELEON, r.primary?.id)
        assertEquals(PersonalityId.CHAMELEON, r.gate)
        assertNull(r.margin)
        assertFalse(r.isTransitioning)
    }

    @Test
    fun `CHAMELEON Gate - R 大于等于 0_33 不触发 进入 Base`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.5, DaytimeOnsetState.FALSE))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertNull(r.gate)
    }

    @Test
    fun `UNKNOWN 不等于 FALSE - R 小于 0_33 也不触发 CHAMELEON`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.2, DaytimeOnsetState.UNKNOWN))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertNull(r.gate)
    }

    // ── Base / 反事实 ──

    @Test
    fun `Base - 正常中心点 HOUND`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 1.0))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertEquals(1.0, r.primaryScore!!, 1e-9)
        assertNull(r.secondary)
        assertNull(r.margin)
    }

    @Test
    fun `反事实 - 真实 0200 8h 为 NIGHT_OWL 且 secondary 为 HOUND`() {
        val sessions = List(5) { session(2, 0, 480) }
        val r = PersonalityMatcher.classify(SleepBehaviorProfile.from(sessions))
        assertEquals(PersonalityId.NIGHT_OWL, r.primary?.id)
        assertEquals(PersonalityId.HOUND, r.secondary?.id)
        assertEquals(0.5, r.margin!!, 1e-6) // 0.75 − 0.25
        assertFalse(r.isTransitioning)
    }

    @Test
    fun `反事实 - 真实 2330 8h 为 HOUND`() {
        val sessions = List(5) { session(23, 30, 480) }
        val r = PersonalityMatcher.classify(SleepBehaviorProfile.from(sessions))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
    }

    @Test
    fun `Transition - margin 小于 0_15 为 isTransitioning`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.35), 0.0, 1.0))
        assertEquals(PersonalityId.NIGHT_OWL, r.primary?.id)
        assertEquals(PersonalityId.HOUND, r.secondary?.id)
        assertNotNull(r.margin)
        assertTrue(r.margin!! < 0.15)
        assertTrue(r.isTransitioning)
    }

    // ── Boundary / undefined ──

    @Test
    fun `Boundary - MeanUndefined`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.MeanUndefined, 0.0, 0.9))
        assertTrue(r.boundary)
        assertNull(r.primary)
    }

    @Test
    fun `Boundary - OutOfDomain 且 R 大于等于 0_33（μ_C 不救场）`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.OutOfDomain, 0.0, 0.9))
        assertTrue(r.boundary)
        assertNull(r.primary)
    }

    @Test
    fun `Boundary - D 缺失`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), null, 1.0))
        assertTrue(r.boundary)
        assertNull(r.primary)
    }

    @Test
    fun `Boundary - R undefined 使 SLOTH OTTER 不可评分且 CHAMELEON 不触发`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 2.0 / 3.0, null))
        assertTrue(r.boundary)
        assertNull(r.gate)
        assertNull(r.confidence) // R undefined → Confidence null
    }

    @Test
    fun `Insufficient - n 小于 5 不执行判定`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.2, n = 4))
        assertTrue(r.insufficientData)
        assertFalse(r.boundary)
        assertNull(r.primary)
    }

    // ── Secondary ──

    @Test
    fun `Secondary eligibility - S 等于 0_25 达标`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(-1.0 / 6.0), 0.0, 1.0))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertEquals(PersonalityId.EARLY_BIRD, r.secondary?.id)
        assertEquals(0.25, r.secondaryScore!!, 1e-6)
    }

    @Test
    fun `Secondary eligibility - S 小于 0_25 不达标 为 null`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(-38.0 / 240.0), 0.0, 1.0))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertNull(r.secondary)
        assertNull(r.margin)
        assertFalse(r.isTransitioning)
    }

    @Test
    fun `Secondary tie - priority 决胜（EARLY 2 vs WORK 4）`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(-1.0 / 3.0), -1.0 / 3.0, 1.0))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertEquals(PersonalityId.WORK_HORSE, r.secondary?.id)
        assertEquals(0.25, r.secondaryScore!!, 1e-6)
    }

    @Test
    fun `Gate Secondary - CHAMELEON 来自全部 10 Base`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.2))
        assertEquals(PersonalityId.CHAMELEON, r.primary?.id)
        assertEquals(PersonalityId.HOUND, r.secondary?.id) // 全 10 Base 中最高分
        assertNull(r.margin)
        assertFalse(r.isTransitioning)
    }

    @Test
    fun `Gate Secondary - WOLF 无有效候选（C OutOfDomain）`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.OutOfDomain, 0.0, 0.9, DaytimeOnsetState.TRUE))
        assertEquals(PersonalityId.WOLF, r.primary?.id)
        assertNull(r.secondary)
        assertNull(r.margin)
        assertFalse(r.isTransitioning)
    }

    // ── SLOTH / OTTER / tie-break / Goal 独立性 / Deterministic ──

    @Test
    fun `SLOTH OTTER R 0_6 - 精确同分 由 priority OTTER 胜出`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 2.0 / 3.0, 0.6))
        assertEquals(PersonalityId.OTTER, r.primary?.id)
        assertEquals(1.0, r.primaryScore!!, 1e-9)
    }

    @Test
    fun `Goal 独立性 - target 不改变 primary 只影响 confidence`() {
        val p = profile(ChronotypeState.Value(0.0), 0.0, 1.0, meanOnsetMin = 120)
        val aligned = PersonalityMatcher.classify(p, targetSleepTimeMin = 120)
        val misaligned = PersonalityMatcher.classify(p, targetSleepTimeMin = 1380)
        assertEquals(aligned.primary?.id, misaligned.primary?.id)
        assertTrue("目标偏离应降低置信度", aligned.confidence!! > misaligned.confidence!!)
    }

    @Test
    fun `Deterministic - 相同输入重复执行结果一致`() {
        val p = profile(ChronotypeState.Value(0.35), 0.0, 1.0)
        assertEquals(PersonalityMatcher.classify(p), PersonalityMatcher.classify(p))
    }

    // ── 边界与异常输入加固（PHASE 5）──

    @Test
    fun `effectiveSessions 边界 - n 0 1 4 为 insufficient n 5 6 为正式判定`() {
        for (n in listOf(0, 1, 4)) {
            val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 1.0, n = n))
            assertTrue("n=$n 应 insufficient", r.insufficientData)
            assertFalse("n=$n 不应是 boundary", r.boundary)
            assertNull("n=$n 不应产生人格", r.primary)
        }
        for (n in listOf(5, 6)) {
            val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 1.0, n = n))
            assertFalse("n=$n 不应 insufficient", r.insufficientData)
            assertEquals(PersonalityId.HOUND, r.primary?.id)
        }
    }

    @Test
    fun `n 小于 5 - 即使 100 白天 也不触发 WOLF`() {
        val r = PersonalityMatcher.classify(
            profile(ChronotypeState.Value(0.0), 0.0, 0.9, DaytimeOnsetState.TRUE, n = 4)
        )
        assertTrue(r.insufficientData)
        assertNull(r.primary)
    }

    @Test
    fun `n 小于 5 - FALSE 且 R 低 也不触发 CHAMELEON`() {
        val r = PersonalityMatcher.classify(
            profile(ChronotypeState.Value(0.0), 0.0, 0.2, DaytimeOnsetState.FALSE, n = 4)
        )
        assertTrue(r.insufficientData)
        assertNull(r.gate)
    }

    @Test
    fun `WOLF 优先于 CHAMELEON - TRUE 时 R 低也不检查 CHAMELEON`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.1, DaytimeOnsetState.TRUE))
        assertEquals(PersonalityId.WOLF, r.primary?.id)
    }

    @Test
    fun `R 边界 - 0_33 不触发 CHAMELEON（严格小于）`() {
        val r = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.33, DaytimeOnsetState.FALSE))
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertNull(r.gate)
    }

    @Test
    fun `R 边界 - 0_0 触发 CHAMELEON 1_0 进入 Base`() {
        val low = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 0.0, DaytimeOnsetState.FALSE))
        assertEquals(PersonalityId.CHAMELEON, low.primary?.id)
        val high = PersonalityMatcher.classify(profile(ChronotypeState.Value(0.0), 0.0, 1.0, DaytimeOnsetState.FALSE))
        assertEquals(PersonalityId.HOUND, high.primary?.id)
    }

    @Test
    fun `isTransitioning 边界 - 0_15 精确比较无 epsilon`() {
        assertTrue(PersonalityMatcher.isTransitioning(Math.nextDown(0.15)))
        assertFalse(PersonalityMatcher.isTransitioning(0.15))
        assertFalse(PersonalityMatcher.isTransitioning(Math.nextUp(0.15)))
        assertFalse(PersonalityMatcher.isTransitioning(null))
    }
}
