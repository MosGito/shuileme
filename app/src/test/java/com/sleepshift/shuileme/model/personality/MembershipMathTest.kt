package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * V2.0.7 §5.1 / §5.3 / §23（#10–#12、#26、#31、#60）Membership 数学层测试。
 */
class MembershipMathTest {

    private val math = MembershipMath
    private val registry = PersonalityRegistry
    private val sloth = registry.definition(PersonalityId.SLOTH)
    private val otter = registry.definition(PersonalityId.OTTER)
    private val hound = registry.definition(PersonalityId.HOUND)

    // ── mAxis 基础行为 ──

    @Test
    fun `mAxis - 中心为 1`() {
        assertEquals(1.0, math.mAxis(0.0, 0.0), 1e-12)
    }

    @Test
    fun `mAxis - 中心加 2_3 为 0`() {
        assertEquals(0.0, math.mAxis(2.0 / 3.0, 0.0), 1e-12)
    }

    @Test
    fun `mAxis - 中心减 2_3 为 0`() {
        assertEquals(0.0, math.mAxis(-2.0 / 3.0, 0.0), 1e-12)
    }

    @Test
    fun `mAxis - 超出边界为 0`() {
        assertEquals(0.0, math.mAxis(1.0, 0.0), 1e-12)
        assertEquals(0.0, math.mAxis(-1.0, 0.0), 1e-12)
        assertEquals(0.0, math.mAxis(0.9, 0.0), 1e-12)
    }

    @Test
    fun `mAxis - 中间点线性结果`() {
        assertEquals(0.5, math.mAxis(1.0 / 3.0, 0.0), 1e-12)
        assertEquals(0.25, math.mAxis(0.5, 0.0), 1e-12)
    }

    @Test
    fun `mAxis - 绝不返回负值`() {
        var x = -1.0
        while (x <= 1.0) {
            assertTrue("x=$x", math.mAxis(x, 0.0) >= 0.0)
            x += 0.1
        }
    }

    @Test
    fun `mAxis - 默认宽度为 2_3`() {
        assertEquals(2.0 / 3.0, PersonalityConstants.AXIS_HALF_WIDTH, 1e-12)
    }

    // ── mC / mD ──

    @Test
    fun `mC - Value 0 对 Neutral 中心为 1`() {
        assertEquals(1.0, math.mC(ChronotypeState.Value(0.0), 0.0)!!, 1e-12)
    }

    @Test
    fun `mC - 相差 1_3 为 0_5`() {
        assertEquals(0.5, math.mC(ChronotypeState.Value(1.0 / 3.0), 0.0)!!, 1e-12)
    }

    @Test
    fun `mC - 相差 2_3 为 0`() {
        assertEquals(0.0, math.mC(ChronotypeState.Value(2.0 / 3.0), 0.0)!!, 1e-12)
    }

    @Test
    fun `mD - 0 对 Neutral 中心为 1`() {
        assertEquals(1.0, math.mD(0.0, 0.0), 1e-12)
    }

    @Test
    fun `mD - 正负 2_3 对应边界为 0`() {
        assertEquals(0.0, math.mD(2.0 / 3.0, 0.0), 1e-12)
        assertEquals(0.0, math.mD(-2.0 / 3.0, 0.0), 1e-12)
    }

    // ── SLOTH / OTTER ramp ──

    @Test
    fun `OTTER ramp - R 0_5 为 0`() {
        assertEquals(0.0, math.mR(otter, 0.5)!!, 1e-12)
    }

    @Test
    fun `ramp - R 0_6 两者均为 1`() {
        assertEquals(1.0, math.mR(sloth, 0.6)!!, 1e-12)
        assertEquals(1.0, math.mR(otter, 0.6)!!, 1e-12)
    }

    @Test
    fun `ramp - R 0_7 SLOTH 为 0 OTTER 为 1`() {
        assertEquals(0.0, math.mR(sloth, 0.7)!!, 1e-12)
        assertEquals(1.0, math.mR(otter, 0.7)!!, 1e-12)
    }

    @Test
    fun `ramp - R 0_8 按 clamp 结果`() {
        assertEquals(0.0, math.mR(sloth, 0.8)!!, 1e-12)
        assertEquals(1.0, math.mR(otter, 0.8)!!, 1e-12)
    }

    @Test
    fun `ramp - R 0_4 按 clamp 结果`() {
        assertEquals(1.0, math.mR(sloth, 0.4)!!, 1e-12)
        assertEquals(0.0, math.mR(otter, 0.4)!!, 1e-12)
    }

    @Test
    fun `mR - 普通 Base 无 R 约束为 1`() {
        assertEquals(1.0, math.mR(hound, null)!!, 1e-12)
    }

    // ── Score ──

    @Test
    fun `score - 三者均为 1 则 S 为 1`() {
        val s = math.score(hound, ChronotypeState.Value(0.0), 0.0, 1.0)
        assertNotNull(s)
        assertEquals(1.0, s!!.score, 1e-12)
    }

    @Test
    fun `score - 任一 membership 为 0 则 S 为 0`() {
        val s = math.score(hound, ChronotypeState.Value(2.0 / 3.0), 0.0, 1.0) // mC=0
        assertEquals(0.0, s!!.score, 1e-12)
    }

    @Test
    fun `score - S 等于 mC 乘 mD 乘 mR`() {
        val s = math.score(hound, ChronotypeState.Value(1.0 / 3.0), 0.0, 1.0) // 0.5 × 1 × 1
        assertEquals(0.5, s!!.score, 1e-12)
    }

    @Test
    fun `score - 不做概率归一化 且恒在 0 到 1`() {
        // 固定画像：断言为 mAxis 原始乘积（归一化会改变这些精确值）
        val c = ChronotypeState.Value(0.4)
        val d = -0.3
        val scores = math.scoreAllBase(c, d, 0.7).associate { it.id to it.score }
        assertEquals(math.mAxis(0.4, 0.0) * math.mAxis(-0.3, -2.0 / 3.0), scores[PersonalityId.WORK_HORSE]!!, 1e-12)
        assertEquals(math.mAxis(0.4, 0.0) * math.mAxis(-0.3, 0.0), scores[PersonalityId.HOUND]!!, 1e-12)
        assertEquals(math.mAxis(0.4, 2.0 / 3.0) * math.mAxis(-0.3, -2.0 / 3.0), scores[PersonalityId.BAT]!!, 1e-12)
        assertEquals(math.mAxis(0.4, 2.0 / 3.0) * math.mAxis(-0.3, 0.0), scores[PersonalityId.NIGHT_OWL]!!, 1e-12)
        scores.values.forEach { assertTrue("score=$it", it in 0.0..1.0) }
        // 另选画像验证范围
        math.scoreAllBase(ChronotypeState.Value(0.0), 0.5, 0.6).forEach { assertTrue(it.score in 0.0..1.0) }
        math.scoreAllBase(ChronotypeState.Value(-0.6), 0.8, 0.5).forEach { assertTrue(it.score in 0.0..1.0) }
        math.scoreAllBase(ChronotypeState.Value(1.0), 1.0, 0.4).forEach { assertTrue(it.score in 0.0..1.0) }
    }

    @Test
    fun `scoreAllBase - 只含 10 个 Base 不含 Gate`() {
        val ids = math.scoreAllBase(ChronotypeState.Value(0.0), 0.0, 0.6).map { it.id }.toSet()
        assertTrue(ids.isNotEmpty())
        assertTrue(ids.all { it in registry.baseDefinitions.map { d -> d.id }.toSet() })
        assertFalse(ids.contains(PersonalityId.CHAMELEON))
        assertFalse(ids.contains(PersonalityId.WOLF))
    }

    // ── deterministic tie-break ──

    @Test
    fun `argmax - 更高 Score 胜过更高 priority`() {
        val a = MembershipScore(PersonalityId.ROOSTER, 0.9)   // priority 1
        val b = MembershipScore(PersonalityId.NIGHT_OWL, 0.8) // priority 9
        assertEquals(PersonalityId.ROOSTER, math.deterministicArgmax(listOf(a, b))!!.id)
    }

    @Test
    fun `argmax - Score 精确相等时 priority 高者胜`() {
        val a = MembershipScore(PersonalityId.ROOSTER, 0.5)   // priority 1
        val b = MembershipScore(PersonalityId.NIGHT_OWL, 0.5) // priority 9
        assertEquals(PersonalityId.NIGHT_OWL, math.deterministicArgmax(listOf(a, b))!!.id)
    }

    @Test
    fun `argmax - Score 与 priority 均等时 stable ID 字典序分支可执行且确定`() {
        // registry 保证不同 ID 的 priority 唯一；第三级分支通过同一定义重复条目触发（name 相等 → 结果确定）
        val x = MembershipScore(PersonalityId.HOUND, 0.5)
        val result = math.deterministicArgmax(listOf(x, x))!!
        assertEquals(PersonalityId.HOUND, result.id)
        assertEquals(0.5, result.score, 1e-12)
    }

    @Test
    fun `argmax - 相同输入重复执行结果完全一致`() {
        val scores = math.scoreAllBase(ChronotypeState.Value(0.0), 2.0 / 3.0, 0.6)
        assertEquals(math.deterministicArgmax(scores), math.deterministicArgmax(scores))
    }

    @Test
    fun `argmax - 空列表为 null`() {
        assertNull(math.deterministicArgmax(emptyList()))
    }

    @Test
    fun `argmax - 输入顺序不影响结果`() {
        val scores = math.scoreAllBase(ChronotypeState.Value(0.35), 0.0, 1.0)
        assertEquals(math.deterministicArgmax(scores), math.deterministicArgmax(scores.reversed()))
        val custom = listOf(
            MembershipScore(PersonalityId.NIGHT_OWL, 0.525),
            MembershipScore(PersonalityId.HOUND, 0.475),
            MembershipScore(PersonalityId.EARLY_BIRD, 0.1),
        )
        assertEquals(PersonalityId.NIGHT_OWL, math.deterministicArgmax(custom)!!.id)
        assertEquals(PersonalityId.NIGHT_OWL, math.deterministicArgmax(custom.reversed())!!.id)
    }

    // ── 规范关键行为 ──

    @Test
    fun `R0_6 - SLOTH 与 OTTER 精确同分 且 OTTER priority 胜出`() {
        val c = ChronotypeState.Value(0.0)
        val sSloth = math.score(sloth, c, 2.0 / 3.0, 0.6)!!
        val sOtter = math.score(otter, c, 2.0 / 3.0, 0.6)!!
        assertEquals(sSloth.score, sOtter.score, 1e-12)
        assertEquals(1.0, sSloth.score, 1e-12)
        assertEquals(PersonalityId.OTTER, math.deterministicArgmax(math.scoreAllBase(c, 2.0 / 3.0, 0.6))!!.id)
    }

    @Test
    fun `Gate - CHAMELEON 与 WOLF 不参与 Base Membership scoring`() {
        assertNull(math.score(registry.definition(PersonalityId.CHAMELEON), ChronotypeState.Value(0.0), 0.0, 0.6))
        assertNull(math.score(registry.definition(PersonalityId.WOLF), ChronotypeState.Value(0.0), 0.0, 0.6))
    }

    @Test
    fun `Boundary - 不属于 PersonalityDefinition`() {
        assertTrue(registry.definitions.none { it.name.contains("Boundary") || it.name.contains("未分类") })
    }

    @Test
    fun `CHAOS - 不存在于任何 Primary Membership`() {
        val ids = math.scoreAllBase(ChronotypeState.Value(0.0), 0.0, 0.6).map { it.id }
        assertTrue(ids.none { it.name == "CHAOS" })
    }

    @Test
    fun `C undefined - 不得自动变成 C 0`() {
        assertNull(math.mC(ChronotypeState.OutOfDomain, 0.0))
        assertNull(math.mC(ChronotypeState.MeanUndefined, 0.0))
        assertNull(math.score(hound, ChronotypeState.OutOfDomain, 0.0, 1.0))
        assertNull(math.score(hound, ChronotypeState.MeanUndefined, 0.0, 1.0))
    }

    @Test
    fun `R undefined - 约束定义不可评分 普通定义仍可评分`() {
        assertNull(math.score(sloth, ChronotypeState.Value(0.0), 2.0 / 3.0, null))
        assertNull(math.score(otter, ChronotypeState.Value(0.0), 2.0 / 3.0, null))
        assertNotNull(math.score(hound, ChronotypeState.Value(0.0), 0.0, null))
    }
}
