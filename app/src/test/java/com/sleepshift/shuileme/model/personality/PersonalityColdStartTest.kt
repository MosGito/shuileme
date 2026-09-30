package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.0.7 §11.2 Cold Start Model 测试：
 * 自报当前作息 → 12 定义体系初步人格；不伪造 C；不触发 Special Gate；确定性。
 */
class PersonalityColdStartTest {

    @Test
    fun `早睡足眠 - 22_30 到 6_30 - EARLY_BIRD`() {
        assertEquals(PersonalityId.EARLY_BIRD, PersonalityColdStart.classify(22 * 60 + 30, 6 * 60 + 30))
    }

    @Test
    fun `偏晚足眠 - 23_30 到 7_30 - HOUND`() {
        assertEquals(PersonalityId.HOUND, PersonalityColdStart.classify(23 * 60 + 30, 7 * 60 + 30))
    }

    @Test
    fun `偏晚短眠 - 0_30 到 6_30 - WORK_HORSE`() {
        assertEquals(PersonalityId.WORK_HORSE, PersonalityColdStart.classify(30, 6 * 60 + 30))
    }

    @Test
    fun `早睡短眠 - 22_30 到 4_00 - ROOSTER`() {
        assertEquals(PersonalityId.ROOSTER, PersonalityColdStart.classify(22 * 60 + 30, 4 * 60))
    }

    @Test
    fun `跨午夜窗口 - 23_00 到 7_00 时长 480 - HOUND`() {
        assertEquals(PersonalityId.HOUND, PersonalityColdStart.classify(23 * 60, 7 * 60))
    }

    @Test
    fun `日间自报入睡 - 不伪造 C 返回 null`() {
        assertNull(PersonalityColdStart.classify(13 * 60, 21 * 60))
    }

    @Test
    fun `冷启动永不输出 Special Gate`() {
        val ids = listOf(
            PersonalityColdStart.classify(22 * 60, 6 * 60),
            PersonalityColdStart.classify(0, 8 * 60),
            PersonalityColdStart.classify(60, 7 * 60),
            PersonalityColdStart.classify(23 * 60, 5 * 60),
        ).filterNotNull()
        assertTrue(ids.none { it == PersonalityId.CHAMELEON || it == PersonalityId.WOLF })
    }

    @Test
    fun `同一输入多次运行结果确定`() {
        val a = PersonalityColdStart.classify(22 * 60 + 30, 6 * 60 + 30)
        val b = PersonalityColdStart.classify(22 * 60 + 30, 6 * 60 + 30)
        assertEquals(a, b)
    }
}
