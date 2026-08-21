package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SleepPersonalityTest {

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun session(startHour: Int, durationHours: Long): SleepSession {
        val start = at(startHour, 0)
        return SleepSession(start, start + durationHours * 3_600_000L)
    }

    // ── 人格判定 ──

    @Test
    fun `夜猫子 - 凌晨2点入睡`() {
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(5) { session(2, 7) }))
        assertEquals(SleepPersonalityType.NIGHT_OWL, state.primaryType)
    }

    @Test
    fun `牛马型 - 平均不足6小时（短睡优先于早起鸟）`() {
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(5) { session(23, 5) }))
        assertEquals(SleepPersonalityType.WORK_HORSE, state.primaryType)
    }

    @Test
    fun `早起鸟 - 22点入睡 6点醒`() {
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(5) { session(22, 8) }))
        assertEquals(SleepPersonalityType.EARLY_BIRD, state.primaryType)
    }

    @Test
    fun `海獭型 - 平均7小时以上`() {
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(5) { session(23, 8) }))
        assertEquals(SleepPersonalityType.OTTER, state.primaryType)
    }

    @Test
    fun `数据不足返回空人格且居民为月亮宝宝`() {
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(3) { session(23, 8) }))
        assertNull(state.primaryType)
        assertEquals(0.0, state.confidence, 0.0)
        assertEquals(5, ResidentEngine.totalResidents(state.residents))
    }

    // ── 居民数量限制 5~12 ──

    @Test
    fun `居民数量限制在5到12之间`() {
        val primary = SleepPersonalityType.NIGHT_OWL
        val health = SleepPersonalityType.OTTER
        for (i in 0..10) {
            val residents = ResidentEngine.generateResidents(primary, i / 10.0, health)
            val total = ResidentEngine.totalResidents(residents)
            assertTrue("total=$total 应在 [5,12]", total in ResidentEngine.MIN_RESIDENTS..ResidentEngine.MAX_RESIDENTS)
        }
    }

    // ── 人格迁移 ──

    @Test
    fun `进化0时只有主人格居民`() {
        val residents = ResidentEngine.generateResidents(SleepPersonalityType.NIGHT_OWL, 0.0, SleepPersonalityType.OTTER)
        assertTrue(residents.all { it.personalityType == SleepPersonalityType.NIGHT_OWL })
        assertEquals(5, ResidentEngine.totalResidents(residents))
    }

    @Test
    fun `进化提升时健康人格居民增加 旧人格减少`() {
        val at0 = ResidentEngine.generateResidents(SleepPersonalityType.NIGHT_OWL, 0.0, SleepPersonalityType.OTTER)
        val atFull = ResidentEngine.generateResidents(SleepPersonalityType.NIGHT_OWL, 1.0, SleepPersonalityType.OTTER)
        val owlAt0 = at0.filter { it.personalityType == SleepPersonalityType.NIGHT_OWL }.sumOf { it.amount }
        val owlAtFull = atFull.filter { it.personalityType == SleepPersonalityType.NIGHT_OWL }.sumOf { it.amount }
        val otterAtFull = atFull.filter { it.personalityType == SleepPersonalityType.OTTER }.sumOf { it.amount }
        assertTrue("主人格应减少（$owlAtFull < $owlAt0）", owlAtFull < owlAt0)
        assertTrue("健康人格应出现", otterAtFull > 0)
    }

    // ── 碎碎念触发频率 ──

    @Test
    fun `碎碎念每日最多3次且深夜才触发`() {
        assertTrue(ResidentTalkSystem.shouldTrigger(0, 23))
        assertTrue(ResidentTalkSystem.shouldTrigger(2, 23))
        assertFalse(ResidentTalkSystem.shouldTrigger(3, 23))   // 达上限
        assertFalse(ResidentTalkSystem.shouldTrigger(0, 12))   // 白天不触发
        assertTrue(ResidentTalkSystem.shouldTrigger(0, 2))     // 凌晨也触发
    }

    @Test
    fun `碎碎念场景选择`() {
        assertEquals(ResidentTalkSystem.TalkScenario.LATE, ResidentTalkSystem.pickScenario(true, MoonMood.EXPECTANT, null))
        assertEquals(ResidentTalkSystem.TalkScenario.MOON, ResidentTalkSystem.pickScenario(false, MoonMood.CELEBRATING, null))
        assertEquals(ResidentTalkSystem.TalkScenario.GOOD_SLEEP, ResidentTalkSystem.pickScenario(false, MoonMood.GENTLE, true))
        assertEquals(ResidentTalkSystem.TalkScenario.GENERAL, ResidentTalkSystem.pickScenario(false, MoonMood.EXPECTANT, null))
    }

    @Test
    fun `碎碎念文案按人格取预置模板`() {
        val owl = ResidentTalkSystem.pickTalk(SleepPersonalityType.NIGHT_OWL, ResidentTalkSystem.TalkScenario.LATE, 0)
        val owl2 = ResidentTalkSystem.pickTalk(SleepPersonalityType.NIGHT_OWL, ResidentTalkSystem.TalkScenario.LATE, 1)
        assertTrue(owl.isNotBlank())
        assertTrue(owl != owl2) // 轮换
        // 夜猫子 LATE 模板含"熬夜"
        assertTrue(owl.contains("熬夜") || owl2.contains("熬夜"))
    }

    // ── 隐藏人格解锁 ──

    @Test
    fun `隐藏人格 - 满月守护者 由满月奖励或连续7天解锁`() {
        val byReward = SleepPersonalityEngine.unlockedHidden(PersonalityInput(moonLife = MoonLife(totalRewards = 1)))
        assertTrue(SleepPersonalityType.MOON_GUARDIAN in byReward)
        val byStreak = SleepPersonalityEngine.unlockedHidden(PersonalityInput(streakDays = 7))
        assertTrue(SleepPersonalityType.MOON_GUARDIAN in byStreak)
    }

    @Test
    fun `隐藏人格 - 夜行幽灵 由连续熬夜解锁`() {
        val ghost = SleepPersonalityEngine.unlockedHidden(PersonalityInput(moonLife = MoonLife(recentLateNights = 3)))
        assertTrue(SleepPersonalityType.NIGHT_GHOST in ghost)
    }

    @Test
    fun `隐藏人格 - 摆烂大师 由多次短睡解锁`() {
        val shortSessions = List(3) { SleepSession(at(23, 0), at(23, 0) + 3 * 3_600_000L) }
        val lazy = SleepPersonalityEngine.unlockedHidden(PersonalityInput(sessions = shortSessions))
        assertTrue(SleepPersonalityType.PROCASTINATOR in lazy)
        val none = SleepPersonalityEngine.unlockedHidden(PersonalityInput())
        assertTrue(none.isEmpty())
    }
}
