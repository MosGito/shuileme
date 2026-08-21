package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SleepDetectiveTest {

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ── 案件等级计算 ──

    @Test
    fun `案件等级 - 0线索安静 1线索小动作 2线索偷偷回来`() {
        assertEquals(DetectiveCaseLevel.QUIET, SleepDetective.computeLevel(0))
        assertEquals(DetectiveCaseLevel.LITTLE_ACTION, SleepDetective.computeLevel(1))
        assertEquals(DetectiveCaseLevel.SNEAK_BACK, SleepDetective.computeLevel(2))
        assertEquals(DetectiveCaseLevel.SNEAK_BACK, SleepDetective.computeLevel(5))
    }

    // ── 报告生成 ──

    @Test
    fun `报告生成 - 含标题 观察 等级 结论`() {
        val start = at(23, 30)
        val data = SleepDetectiveData(
            sleepStartTime = start,
            wakeUpTime = start + 7 * 3_600_000L,
            selfReportedSleepDuration = 420,
            nightActivityHints = 2,
        )
        val report = SleepDetective.generateReport(data, 0)
        assertEquals("昨晚睡眠案件", report.title)
        assertEquals(DetectiveCaseLevel.SNEAK_BACK, report.caseLevel)
        assertTrue(report.observation.contains("23:30"))
        assertTrue(report.conclusion.isNotBlank())
    }

    @Test
    fun `报告观察按等级不同`() {
        val start = at(23, 30)
        val quiet = SleepDetective.generateReport(SleepDetectiveData(sleepStartTime = start, nightActivityHints = 0), 0)
        val sneak = SleepDetective.generateReport(SleepDetectiveData(sleepStartTime = start, nightActivityHints = 3), 0)
        assertTrue(quiet.observation != sneak.observation)
        assertEquals(DetectiveCaseLevel.QUIET, quiet.caseLevel)
        assertEquals(DetectiveCaseLevel.SNEAK_BACK, sneak.caseLevel)
    }

    // ── 文案轮换 ──

    @Test
    fun `文案轮换 - 按variant轮换且确定性`() {
        val c0 = DetectiveTemplatePool.pickConclusion(DetectiveCaseLevel.QUIET, 0)
        val c1 = DetectiveTemplatePool.pickConclusion(DetectiveCaseLevel.QUIET, 1)
        assertTrue(c0 != c1)
        assertEquals(c0, DetectiveTemplatePool.pickConclusion(DetectiveCaseLevel.QUIET, 3))
        assertEquals(c0, DetectiveTemplatePool.pickConclusion(DetectiveCaseLevel.QUIET, -3))
    }

    // ── 数据持久化（稳定报告 + 降级） ──

    @Test
    fun `完整数据稳定生成报告`() {
        val start = at(23, 30)
        val full = SleepDetectiveData(
            sleepStartTime = start,
            wakeUpTime = start + 7 * 3_600_000L,
            selfReportedSleepDuration = 420,
            chargingDuration = 180,
            nightActivityHints = 1,
        )
        val r1 = SleepDetective.generateReport(full, 1)
        val r2 = SleepDetective.generateReport(full, 1)
        assertEquals(r1, r2) // 稳定
        assertEquals(DetectiveCaseLevel.LITTLE_ACTION, r1.caseLevel)
        // 缺省（未醒）→ 时间降级
        assertEquals("？", SleepDetective.formatTime(null))
    }

    // ── 禁用词 ──

    @Test
    fun `报告不含医疗断言禁用词`() {
        val start = at(23, 30)
        val data = SleepDetectiveData(sleepStartTime = start, nightActivityHints = 3)
        val report = SleepDetective.generateReport(data, 0)
        val text = report.observation + report.conclusion
        assertFalse(text.contains("你睡醒了"))
        assertFalse(text.contains("你深睡了"))
        assertFalse(text.contains("睡眠质量"))
    }
}
