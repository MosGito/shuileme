package com.sleepshift.shuileme.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonLifeTest {

    private fun qualified() = SleepResult(480, true, null)
    private fun lateShort() = SleepResult(300, false, SleepResultReason.STARTED_TOO_LATE)

    // ── 状态转换（五阶段） ──

    @Test
    fun `成长值映射到五阶段`() {
        assertEquals(MoonStage.BABY, MoonStage.fromGrowth(0))
        assertEquals(MoonStage.CRESCENT, MoonStage.fromGrowth(20))
        assertEquals(MoonStage.HALF, MoonStage.fromGrowth(40))
        assertEquals(MoonStage.NEAR_FULL, MoonStage.fromGrowth(60))
        assertEquals(MoonStage.NEAR_FULL, MoonStage.fromGrowth(80))
        assertEquals(MoonStage.FULL, MoonStage.fromGrowth(100))
        // 阶段 emoji
        assertEquals("🌑", MoonStage.BABY.emoji)
        assertEquals("🌕", MoonStage.FULL.emoji)
        assertEquals("满月伙伴", MoonStage.FULL.displayName)
    }

    @Test
    fun `连续合格推进阶段直至满月`() {
        var m = MoonProgress()
        // night1 growth 20 → 🌒
        m = m.applyResult(qualified())
        assertEquals(MoonStage.CRESCENT, MoonStage.fromGrowth(m.growthPercent))
        // night2 → 40 🌓
        m = m.applyResult(qualified())
        assertEquals(MoonStage.HALF, MoonStage.fromGrowth(m.growthPercent))
        // night3 → 60 🌔
        m = m.applyResult(qualified())
        assertEquals(MoonStage.NEAR_FULL, MoonStage.fromGrowth(m.growthPercent))
        // night4 → 80 🌔
        m = m.applyResult(qualified())
        assertEquals(MoonStage.NEAR_FULL, MoonStage.fromGrowth(m.growthPercent))
    }

    // ── 情绪计算 ──

    @Test
    fun `情绪 - 满月奖励为庆祝`() {
        assertEquals(MoonMood.CELEBRATING, computeMoonMood(true, 5, 0, rewardPending = true))
        assertEquals(MoonMood.CELEBRATING, computeMoonMood(false, 0, 3, rewardPending = true))
    }

    @Test
    fun `情绪 - 合格为温柔`() {
        assertEquals(MoonMood.GENTLE, computeMoonMood(true, 1, 0, rewardPending = false))
    }

    @Test
    fun `情绪 - 不合格为失望`() {
        assertEquals(MoonMood.DISAPPOINTED, computeMoonMood(false, 0, 0, rewardPending = false))
    }

    @Test
    fun `情绪 - 熬夜两次以上为担心`() {
        assertEquals(MoonMood.WORRIED, computeMoonMood(null, 0, 2, rewardPending = false))
        assertEquals(MoonMood.WORRIED, computeMoonMood(null, 0, 5, rewardPending = false))
    }

    @Test
    fun `情绪 - 连续合格或有记录为温柔 默认期待`() {
        assertEquals(MoonMood.GENTLE, computeMoonMood(null, 2, 0, rewardPending = false))
        assertEquals(MoonMood.EXPECTANT, computeMoonMood(null, 0, 1, rewardPending = false))
    }

    // ── 奖励触发 ──

    @Test
    fun `连续5晚合格触发满月奖励`() {
        var m = MoonProgress()
        repeat(4) { m = m.applyResult(qualified()) }
        assertTrue(!m.fullMoonRewardPending)
        assertEquals(80, m.growthPercent)
        m = m.applyResult(qualified())
        assertTrue("第 5 晚应触发奖励", m.fullMoonRewardPending)
        assertEquals(0, m.growthPercent) // 奖励后重置
        assertEquals(MoonStage.FULL, MoonStage.fromGrowth(100)) // 奖励展示阶段
    }

    @Test
    fun `熬夜不合格不触发奖励且情绪源为过晚`() {
        val m = MoonProgress().applyResult(lateShort())
        assertTrue(!m.fullMoonRewardPending)
        assertEquals(SleepResultReason.STARTED_TOO_LATE, m.lastSleepResult?.reason)
    }

    // ── 事件池 ──

    @Test
    fun `事件池预置且按 variant 确定性轮换`() {
        assertTrue(MoonEventPool.size() >= 5)
        val a = MoonEventPool.pick(0)
        val b = MoonEventPool.pick(1)
        assertTrue(a.isNotBlank() && b.isNotBlank())
        assertTrue(a != b)
        assertEquals(MoonEventPool.pick(0), MoonEventPool.pick(MoonEventPool.size()))
        assertEquals(MoonEventPool.pick(0), MoonEventPool.pick(-MoonEventPool.size()))
    }
}
