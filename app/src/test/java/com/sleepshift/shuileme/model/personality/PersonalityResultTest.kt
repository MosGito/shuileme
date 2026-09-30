package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PersonalityResult 契约测试：Base / Gate / Boundary / Insufficient 四种形态。
 */
class PersonalityResultTest {

    private fun baseProfile(
        chronotype: ChronotypeState = ChronotypeState.Value(0.0),
        d: Double? = 0.0,
        regularity: Double? = 1.0,
        state: DaytimeOnsetState = DaytimeOnsetState.FALSE,
        n: Int = 5,
    ): SleepBehaviorProfile = SleepBehaviorProfile(
        effectiveSessionCount = n,
        meanOnsetMin = 120,
        meanWakeMin = 480,
        meanDurationMin = d?.let { (480.0 + it * 120.0).toLong() },
        onsetStdDevMin = 10.0,
        wakeStdDevMin = 10.0,
        durationStdDevMin = 5.0,
        regularity = regularity,
        completeness = 1.0,
        chronotype = chronotype,
        daytimeOnsetRatio = if (state == DaytimeOnsetState.TRUE) 1.0 else 0.5,
        daytimeOnset = state,
    )

    @Test
    fun `Base 形态 - primary score confidence 正确且无 gate`() {
        val r = PersonalityMatcher.classify(baseProfile())
        assertEquals(PersonalityId.HOUND, r.primary?.id)
        assertEquals(1.0, r.primaryScore!!, 1e-9)
        assertNull(r.gate)
        assertFalse(r.boundary)
        assertFalse(r.insufficientData)
        assertTrue(r.isClassified)
        assertTrue(r.confidence != null)
    }

    @Test
    fun `Gate 形态 - WOLF margin null 且 isTransitioning false`() {
        val r = PersonalityMatcher.classify(
            baseProfile(chronotype = ChronotypeState.Value(-1.0), state = DaytimeOnsetState.TRUE)
        )
        assertEquals(PersonalityId.WOLF, r.primary?.id)
        assertEquals(PersonalityId.WOLF, r.gate)
        assertNull(r.primaryScore)
        assertNull(r.margin)
        assertFalse(r.isTransitioning)
        assertFalse(r.boundary)
        assertTrue(r.isClassified)
    }

    @Test
    fun `Boundary 形态 - 全 null 输出确定性`() {
        val r = PersonalityMatcher.classify(
            baseProfile(chronotype = ChronotypeState.MeanUndefined, regularity = 0.9)
        )
        assertTrue(r.boundary)
        assertFalse(r.isClassified)
        assertNull(r.primary)
        assertNull(r.secondary)
        assertNull(r.margin)
        assertNull(r.gate)
        assertFalse(r.isTransitioning)
        assertFalse(r.insufficientData)
    }

    @Test
    fun `Insufficient 形态 - n 小于 5 不判定且不等于 Boundary`() {
        val r = PersonalityMatcher.classify(baseProfile(n = 4))
        assertTrue(r.insufficientData)
        assertFalse(r.boundary)
        assertNull(r.primary)
        assertNull(r.confidence)
    }
}
