package com.sleepshift.shuileme.model

import com.sleepshift.shuileme.data.ShuilemeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PersonalityCardTest {

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ── 数据生成 ──

    @Test
    fun `已解锁 - 生成夜猫子卡片`() {
        val sessions = List(5) { SleepSession(at(2, 0), at(2, 0) + 7 * 3_600_000L) }
        val personality = SleepPersonalityEngine.compute(PersonalityInput(sessions = sessions))
        val model = PersonalityCardGenerator.generate(ShuilemeState(sessions = sessions), personality)
        assertTrue(model.unlocked)
        assertEquals(SleepPersonalityType.NIGHT_OWL, model.personalityType)
        assertEquals("夜猫子型", model.title)
        assertEquals("02:00", model.averageSleepTime)
        assertTrue(model.averageDuration.isNotBlank())
        assertTrue(model.regularityScore in 0..100)
        assertTrue(model.description.isNotBlank())
    }

    // ── 未满 5 晚 ──

    @Test
    fun `未满5晚 - 未解锁显示还差几晚`() {
        val sessions = List(3) { SleepSession(at(23, 0), at(23, 0) + 8 * 3_600_000L) }
        val model = PersonalityCardGenerator.generate(ShuilemeState(sessions = sessions), SleepPersonalityState())
        assertFalse(model.unlocked)
        assertEquals(2, model.nightsNeeded)
        assertEquals("再睡几晚，解锁你的睡眠人格 🌙", PersonalityCardGenerator.lockedCopy())
    }

    @Test
    fun `刚好5晚即解锁`() {
        val sessions = List(5) { SleepSession(at(23, 0), at(23, 0) + 8 * 3_600_000L) }
        val personality = SleepPersonalityEngine.compute(PersonalityInput(sessions = sessions))
        val model = PersonalityCardGenerator.generate(ShuilemeState(sessions = sessions), personality)
        assertTrue(model.unlocked)
        assertEquals(0, model.nightsNeeded)
    }

    // ── emoji 排列 ──

    @Test
    fun `emoji装饰 - 确定性排列 数量3到5 且只含该人格符号`() {
        val type = SleepPersonalityType.NIGHT_OWL
        val a0 = PersonalityCardGenerator.decorateEmojis(type, 0)
        val a1 = PersonalityCardGenerator.decorateEmojis(type, 1)
        assertTrue(a0.size in 3..5)
        assertTrue(a1.size in 3..5)
        assertTrue(a0.all { it in type.comboEmojis })
        assertEquals(a0, PersonalityCardGenerator.decorateEmojis(type, 0)) // 确定性
        assertTrue(a0 != a1) // 不同 seed 排列不同
    }

    // ── 分享文本 ──

    @Test
    fun `分享文本为固定模板`() {
        assertEquals("我的睡眠人格是 夜猫子型 🌙\n来自睡了么", PersonalityCardGenerator.shareText("夜猫子型"))
    }

    @Test
    fun `人格双emoji表示`() {
        assertEquals("🌙🐱", PersonalityCardGenerator.personalityPairEmoji(SleepPersonalityType.NIGHT_OWL))
        assertEquals("🌞🐦", PersonalityCardGenerator.personalityPairEmoji(SleepPersonalityType.EARLY_BIRD))
    }
}
