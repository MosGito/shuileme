package com.sleepshift.shuileme.ui

import com.sleepshift.shuileme.model.SleepPersonalityType
import com.sleepshift.shuileme.model.personality.ChronotypeState
import com.sleepshift.shuileme.model.personality.DaytimeOnsetState
import com.sleepshift.shuileme.model.personality.PersonalityId
import com.sleepshift.shuileme.model.personality.PersonalityMatcher
import com.sleepshift.shuileme.model.personality.SleepBehaviorProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PersonalityResultAdapter 测试：新判定结果 → 旧展示结构的最小兼容投影。
 * 判定本身来自 PersonalityMatcher（V2.0.7 §20.1），适配层不重新实现算法。
 */
class PersonalityResultAdapterTest {

    private fun profile(
        chronotype: ChronotypeState = ChronotypeState.Value(0.0),
        d: Double? = 0.0,
        regularity: Double? = 1.0,
        state: DaytimeOnsetState = DaytimeOnsetState.FALSE,
        n: Int = 5,
        meanOnsetMin: Int = 120,
        meanWakeMin: Int = 480,
    ): SleepBehaviorProfile = SleepBehaviorProfile(
        effectiveSessionCount = n,
        meanOnsetMin = meanOnsetMin,
        meanWakeMin = meanWakeMin,
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
    fun `其余 7 个 PersonalityId 投影到旧枚举`() {
        assertEquals(SleepPersonalityType.EARLY_BIRD, PersonalityResultAdapter.legacyType(PersonalityId.ROOSTER))
        assertEquals(SleepPersonalityType.OTTER, PersonalityResultAdapter.legacyType(PersonalityId.BEAR))
        assertEquals(SleepPersonalityType.OTTER, PersonalityResultAdapter.legacyType(PersonalityId.HOUND))
        assertEquals(SleepPersonalityType.CHAOS, PersonalityResultAdapter.legacyType(PersonalityId.SLOTH))
        assertEquals(SleepPersonalityType.NIGHT_OWL, PersonalityResultAdapter.legacyType(PersonalityId.BAT))
        assertEquals(SleepPersonalityType.NIGHT_GHOST, PersonalityResultAdapter.legacyType(PersonalityId.OWL))
        assertEquals(SleepPersonalityType.WORK_HORSE, PersonalityResultAdapter.legacyType(PersonalityId.WORK_HORSE))
    }

    @Test
    fun `Gate 与关键类型投影到旧枚举`() {
        assertEquals(SleepPersonalityType.CHAOS, PersonalityResultAdapter.legacyType(PersonalityId.CHAMELEON))
        assertEquals(SleepPersonalityType.NIGHT_GHOST, PersonalityResultAdapter.legacyType(PersonalityId.WOLF))
        assertEquals(SleepPersonalityType.NIGHT_OWL, PersonalityResultAdapter.legacyType(PersonalityId.NIGHT_OWL))
        assertEquals(SleepPersonalityType.OTTER, PersonalityResultAdapter.legacyType(PersonalityId.OTTER))
        assertEquals(SleepPersonalityType.EARLY_BIRD, PersonalityResultAdapter.legacyType(PersonalityId.EARLY_BIRD))
    }

    @Test
    fun `正式 Base 判定 - 投影 primaryType 且透传 metrics 与 confidence`() {
        val p = profile()
        val result = PersonalityMatcher.classify(p)
        assertTrue(result.isClassified)
        val state = PersonalityResultAdapter.legacyState(result, p)
        assertEquals(SleepPersonalityType.OTTER, state.primaryType) // HOUND → 海獭投影
        assertEquals(120, state.metrics.avgSleepTimeMin)
        assertEquals(480, state.metrics.avgWakeTimeMin)
        assertEquals(480L, state.metrics.avgDurationMin)
        assertEquals(1.0, state.metrics.regularityScore, 1e-9)
        assertEquals(result.confidence!!, state.confidence, 1e-9)
    }

    @Test
    fun `数据不足 - primaryType null 且 confidence 兼容为 0`() {
        val p = profile(n = 4, regularity = null)
        val result = PersonalityMatcher.classify(p)
        assertTrue(result.insufficientData)
        val state = PersonalityResultAdapter.legacyState(result, p)
        assertNull(state.primaryType)
        assertEquals(0.0, state.confidence, 1e-9)
    }

    @Test
    fun `Boundary - primaryType null`() {
        val p = profile(chronotype = ChronotypeState.MeanUndefined, regularity = 0.9)
        val result = PersonalityMatcher.classify(p)
        assertTrue(result.boundary)
        assertNull(PersonalityResultAdapter.legacyState(result, p).primaryType)
    }
}
