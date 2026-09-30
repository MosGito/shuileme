package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class SleepPersonalityTest {

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 8, 21, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun session(startHour: Int, durationHours: Long): SleepSession {
        val start = at(startHour, 0)
        return SleepSession(start, start + durationHours * 3_600_000L)
    }

    private fun sessionAt(startHour: Int, startMin: Int, durMin: Long): SleepSession {
        val start = at(startHour, startMin)
        return SleepSession(start, start + durMin * 60_000L)
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
    fun `初始人格倾向由自报作息推算`() {
        assertEquals(SleepPersonalityType.NIGHT_OWL, SleepPersonalityEngine.initialInclination(2 * 60, 9 * 60, 7 * 60))
        assertEquals(SleepPersonalityType.EARLY_BIRD, SleepPersonalityEngine.initialInclination(22 * 60, 6 * 60, 8 * 60))
        assertEquals(SleepPersonalityType.OTTER, SleepPersonalityEngine.initialInclination(23 * 60, 8 * 60, 8 * 60))
    }

    // ── STEP 3：冷启动人格基线 = current，而不是 target ──

    @Test
    fun `冷启动 - current 0200-1200 target 2300-0700 时人格来自 current`() {
        // current 02:00→12:00 → 夜猫子方向
        val fromCurrent = SleepPersonalityEngine.initialInclination(2 * 60, 12 * 60, 8 * 60)
        // target 23:00→07:00 → 海獭方向
        val fromTarget = SleepPersonalityEngine.initialInclination(23 * 60, 7 * 60, 8 * 60)
        assertEquals(SleepPersonalityType.NIGHT_OWL, fromCurrent)
        assertEquals(SleepPersonalityType.OTTER, fromTarget)
        assertTrue("current 与 target 应产生不同冷启动人格", fromCurrent != fromTarget)
    }

    @Test
    fun `冷启动 - current 2300-0800 target 0200-1200 时人格来自 current`() {
        val fromCurrent = SleepPersonalityEngine.initialInclination(23 * 60, 8 * 60, 10 * 60)
        val fromTarget = SleepPersonalityEngine.initialInclination(2 * 60, 12 * 60, 10 * 60)
        assertEquals(SleepPersonalityType.OTTER, fromCurrent)
        assertEquals(SleepPersonalityType.NIGHT_OWL, fromTarget)
        assertTrue(fromCurrent != fromTarget)
    }

    @Test
    fun `冷启动 - 4 晚仍为冷启动规则 5 晚切换真实会话`() {
        // 4 晚：正式人格仍未解锁，冷启动人格由 current 决定
        val four = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(4) { session(2, 8) }))
        assertNull(four.primaryType)
        val cold = SleepPersonalityEngine.initialInclination(2 * 60, 12 * 60, 8 * 60)
        assertEquals(SleepPersonalityType.NIGHT_OWL, cold)

        // 5 晚：正式人格由真实会话决定（02:00→10:00 → NIGHT_OWL），与 current/target 无关
        val five = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(5) { session(2, 8) }))
        assertEquals(SleepPersonalityType.NIGHT_OWL, five.primaryType)
    }

    @Test
    fun `正式人格 - target 改变不改变 primaryType 只影响置信度`() {
        val sessions = List(5) { session(2, 8) } // 真实 02:00→10:00
        val aligned = SleepPersonalityEngine.compute(
            PersonalityInput(sessions = sessions, sleepTargetDeviationMin = 0L)
        )
        val misaligned = SleepPersonalityEngine.compute(
            PersonalityInput(sessions = sessions, sleepTargetDeviationMin = 240L)
        )
        assertEquals(aligned.primaryType, misaligned.primaryType)
        assertTrue("target 偏离应降低置信度", misaligned.confidence < aligned.confidence)
    }

    @Test
    fun `正式人格 - current 不参与 10 晚真实数据稳定`() {
        // PersonalityInput 不含 current 字段：current 只能作为冷启动基线，无法影响正式人格
        val sessions = List(10) { session(2, 8) }
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = sessions))
        assertEquals(SleepPersonalityType.NIGHT_OWL, state.primaryType)
    }

    @Test
    fun `目标偏差降低置信度`() {
        val noDev = SleepPersonalityEngine.computeConfidence(10, 0.8)
        val bigDev = SleepPersonalityEngine.computeConfidence(10, 0.8, 240L)
        assertTrue(bigDev < noDev)
        assertTrue(bigDev >= 0.0)
        // 与默认兼容
        assertEquals(SleepPersonalityEngine.computeConfidence(10, 0.8), noDev, 0.0001)
    }

    @Test
    fun `数据不足返回空人格且居民为月亮宝宝`() {
        val state = SleepPersonalityEngine.compute(PersonalityInput(sessions = List(3) { session(23, 8) }))
        assertNull(state.primaryType)
        assertEquals(0.0, state.confidence, 0.0)
        assertEquals(5, ResidentEngine.totalResidents(state.residents))
    }

    // ── 环形统计（STEP 2：跨午夜修复） ──

    @Test
    fun `环形平均 - 2350与0010平均接近午夜而非中午`() {
        val mean = SleepPersonalityEngine.circularMeanMinutes(listOf(1430, 10))
        assertNotNull(mean)
        // 允许 1 分钟以内误差
        assertTrue(SleepPersonalityEngine.circularDistance(mean!!, 0) <= 1)
        // 绝不能接近 720（12:00）
        assertTrue(SleepPersonalityEngine.circularDistance(mean, 720) > 700)
    }

    @Test
    fun `环形平均 - 多组午夜附近数据平均接近午夜`() {
        val mean = SleepPersonalityEngine.circularMeanMinutes(listOf(1435, 5, 1438, 2))
        assertNotNull(mean)
        assertTrue(SleepPersonalityEngine.circularDistance(mean!!, 0) <= 2)
    }

    @Test
    fun `环形距离 - 2350与0010距离为20分钟`() {
        assertEquals(20, SleepPersonalityEngine.circularDistance(1430, 10))
        assertEquals(20, SleepPersonalityEngine.circularDistance(10, 1430))
        assertEquals(720, SleepPersonalityEngine.circularDistance(0, 720)) // 最大距离
    }

    @Test
    fun `环形平均 - 空列表与均匀退化返回null 单样本返回原值`() {
        assertNull(SleepPersonalityEngine.circularMeanMinutes(emptyList()))
        // 0 与 720 在圆周上相对 → 合成向量≈0 → 无统计意义
        assertNull(SleepPersonalityEngine.circularMeanMinutes(listOf(0, 720)))
        val single = SleepPersonalityEngine.circularMeanMinutes(listOf(120))
        assertNotNull(single)
        assertEquals(120, single)
    }

    @Test
    fun `规律性 - 午夜两侧数据保持高规律度`() {
        // 旧普通标准差会把 23:50/00:10/23:55/00:05 判成极不规律（stddev≈712 → 0）
        val sessions = listOf(
            sessionAt(23, 50, 460), sessionAt(0, 10, 460),
            sessionAt(23, 55, 460), sessionAt(0, 5, 460),
        )
        val reg = SleepPersonalityEngine.computeMetrics(sessions).regularityScore
        assertTrue("regularity=$reg 应保持高值", reg >= 0.9)
    }

    @Test
    fun `规律性 - 非跨午夜数据与修复前语义一致`() {
        val sessions = listOf(
            sessionAt(22, 0, 480), sessionAt(22, 10, 480),
            sessionAt(21, 55, 480), sessionAt(22, 5, 480),
        )
        val reg = SleepPersonalityEngine.computeMetrics(sessions).regularityScore
        // 旧公式结果：普通 stddev(1315,1320,1325,1330)=5.59 → 1 - 5.59/120 ≈ 0.953
        val legacyMean = listOf(1320, 1330, 1315, 1325).average()
        val legacyStddev = sqrt(listOf(1320, 1330, 1315, 1325).map { (it - legacyMean) * (it - legacyMean) }.average())
        val legacyReg = (1.0 - min(1.0, legacyStddev / 120.0)).coerceIn(0.0, 1.0)
        assertTrue("reg=$reg legacy=$legacyReg", abs(reg - legacyReg) < 0.01)
    }

    @Test
    fun `时长平均 - 仍为普通算术平均`() {
        val sessions = listOf(
            sessionAt(22, 0, 480), sessionAt(22, 0, 490),
            sessionAt(22, 0, 475), sessionAt(22, 0, 485),
        )
        val metrics = SleepPersonalityEngine.computeMetrics(sessions)
        // (480+490+475+485)/4 = 482.5 → toLong() 截断为 482
        assertEquals(482L, metrics.avgDurationMin)
    }

    @Test
    fun `computeMetrics - 跨午夜真实会话 入睡接近午夜 起床接近0738 规律度高`() {
        val sessions = listOf(
            sessionAt(23, 50, 460), // 07:30 醒
            sessionAt(0, 10, 460),  // 07:50 醒
            sessionAt(23, 55, 460), // 07:35 醒
            sessionAt(0, 5, 460),   // 07:45 醒
            sessionAt(23, 50, 460), // 07:30 醒
        )
        val metrics = SleepPersonalityEngine.computeMetrics(sessions)
        val start = metrics.avgSleepTimeMin
        assertNotNull(start)
        assertTrue("avgStart=$start 应接近午夜", SleepPersonalityEngine.circularDistance(start!!, 0) <= 6)
        assertTrue("avgStart 不能变成 12:00", SleepPersonalityEngine.circularDistance(start, 720) > 700)
        val wake = metrics.avgWakeTimeMin
        assertNotNull(wake)
        assertTrue("avgWake=$wake 应接近 07:38(458)", SleepPersonalityEngine.circularDistance(wake!!, 458) <= 3)
        assertTrue("regularity=${metrics.regularityScore} 应较高", metrics.regularityScore >= 0.9)
        assertEquals(460L, metrics.avgDurationMin)
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
