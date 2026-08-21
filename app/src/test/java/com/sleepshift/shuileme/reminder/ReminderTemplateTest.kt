package com.sleepshift.shuileme.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTemplateTest {

    @Test
    fun `模板池共 45 条（3人格 × 3类型 × 5模板）`() {
        assertEquals(3 * 3 * 5, ReminderTemplate.templateCount())
    }

    @Test
    fun `每个人格每种类型都有 5 条不同模板`() {
        for (p in ReminderPersonality.entries) {
            for (t in ReminderType.entries) {
                val set = (0 until 5).map { ReminderTemplate.pick(p, t, it) }.toSet()
                assertEquals("$p/$t 应 5 条不同", 5, set.size)
            }
        }
    }

    @Test
    fun `pick 按 variant 轮换且不越界`() {
        val p = ReminderPersonality.MOON
        val t = ReminderType.SLEEP
        val a = ReminderTemplate.pick(p, t, 0)
        val b = ReminderTemplate.pick(p, t, 1)
        assertTrue("variant 0 与 1 应不同", a != b)
        // 取模轮换：variant 5 应回到与 0 相同
        assertEquals(ReminderTemplate.pick(p, t, 0), ReminderTemplate.pick(p, t, 5))
        // 负值不越界
        assertTrue(ReminderTemplate.pick(p, t, -1).isNotBlank())
    }

    @Test
    fun `三种人格文案确实不同`() {
        val a = ReminderTemplate.pick(ReminderPersonality.MOON, ReminderType.LATE, 0)
        val b = ReminderTemplate.pick(ReminderPersonality.SHARP, ReminderType.LATE, 0)
        val c = ReminderTemplate.pick(ReminderPersonality.WORK_HORSE, ReminderType.LATE, 0)
        assertTrue(a.isNotBlank() && b.isNotBlank() && c.isNotBlank())
        assertEquals(3, setOf(a, b, c).size)
    }

    @Test
    fun `熬夜文案包含对应人格风格关键词`() {
        assertTrue(ReminderTemplate.pick(ReminderPersonality.SHARP, ReminderType.LATE, 0).contains("你"))
        assertTrue(ReminderTemplate.pick(ReminderPersonality.WORK_HORSE, ReminderType.LATE, 0).contains("牛马"))
    }
}
