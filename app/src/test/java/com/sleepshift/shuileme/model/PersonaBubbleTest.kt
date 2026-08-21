package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaBubbleTest {

    @Test
    fun `气泡符号直接来自人格类型`() {
        assertEquals(listOf("🌙", "🐱"), ResidentEngine.bubbleSymbols(SleepPersonalityType.NIGHT_OWL))
        assertEquals(listOf("🌞", "🐦"), ResidentEngine.bubbleSymbols(SleepPersonalityType.EARLY_BIRD))
        assertEquals(listOf("🐮", "🐴"), ResidentEngine.bubbleSymbols(SleepPersonalityType.WORK_HORSE)) // 随机 🐮 或 🐴
        assertEquals(listOf("🌊", "🦦"), ResidentEngine.bubbleSymbols(SleepPersonalityType.OTTER))
        assertEquals(listOf("🌪️"), ResidentEngine.bubbleSymbols(SleepPersonalityType.CHAOS))
    }

    @Test
    fun `气泡emoji只含该人格符号 非随机月亮`() {
        val emojis = ResidentEngine.bubbleEmojis(SleepPersonalityType.NIGHT_OWL, 8, 1)
        assertEquals(8, emojis.size)
        assertTrue(emojis.all { it == "🌙" || it == "🐱" })
        assertFalse(emojis.contains("🌑")) // 不含随机月亮
    }

    @Test
    fun `牛马气泡为牛或马`() {
        val emojis = ResidentEngine.bubbleEmojis(SleepPersonalityType.WORK_HORSE, 6, 3)
        assertTrue(emojis.all { it == "🐮" || it == "🐴" })
    }

    @Test
    fun `空数量返回空气泡`() {
        assertTrue(ResidentEngine.bubbleEmojis(SleepPersonalityType.NIGHT_OWL, 0, 0).isEmpty())
    }
}
