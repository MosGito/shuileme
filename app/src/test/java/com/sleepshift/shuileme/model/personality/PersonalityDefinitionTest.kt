package com.sleepshift.shuileme.model.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.0.7 §6 / §7 / §22 / §15：定义层测试。
 * 验证 12 个 PersonalityDefinition 的注册完整性、中心、priority、0.25、邻接表与 Special Gate 语义。
 */
class PersonalityDefinitionTest {

    private val registry = PersonalityRegistry

    @Test
    fun `registry - 恰好 12 个定义且 ID 唯一且与枚举一致`() {
        assertEquals(12, registry.definitions.size)
        assertEquals(12, PersonalityId.entries.size)
        assertEquals(PersonalityId.entries.toSet(), registry.definitions.map { it.id }.toSet())
    }

    @Test
    fun `registry - 10 个 Base 与 2 个 Special Gate`() {
        assertEquals(10, registry.baseDefinitions.size)
        assertEquals(2, registry.specialGateDefinitions.size)
        assertEquals(
            setOf(PersonalityId.CHAMELEON, PersonalityId.WOLF),
            registry.specialGateDefinitions.map { it.id }.toSet(),
        )
    }

    @Test
    fun `centers - 与规范 §6_1 完全一致（Special Gate 无中心）`() {
        val expected = mapOf(
            PersonalityId.ROOSTER to (-2.0 / 3.0 to -2.0 / 3.0),
            PersonalityId.EARLY_BIRD to (-2.0 / 3.0 to 0.0),
            PersonalityId.BEAR to (-2.0 / 3.0 to 2.0 / 3.0),
            PersonalityId.WORK_HORSE to (0.0 to -2.0 / 3.0),
            PersonalityId.HOUND to (0.0 to 0.0),
            PersonalityId.SLOTH to (0.0 to 2.0 / 3.0),
            PersonalityId.OTTER to (0.0 to 2.0 / 3.0),
            PersonalityId.BAT to (2.0 / 3.0 to -2.0 / 3.0),
            PersonalityId.NIGHT_OWL to (2.0 / 3.0 to 0.0),
            PersonalityId.OWL to (2.0 / 3.0 to 2.0 / 3.0),
        )
        expected.forEach { (id, center) ->
            val d = registry.definition(id)
            assertEquals("$id centerC", center.first, d.centerC!!, 1e-12)
            assertEquals("$id centerD", center.second, d.centerD!!, 1e-12)
        }
        // SLOTH / OTTER 共享同一中心（同一 Neutral×Long Region，R 分裂）
        assertEquals(registry.definition(PersonalityId.SLOTH).centerC, registry.definition(PersonalityId.OTTER).centerC)
        assertEquals(registry.definition(PersonalityId.SLOTH).centerD, registry.definition(PersonalityId.OTTER).centerD)
        // Special Gate 无中心
        assertNull(registry.definition(PersonalityId.CHAMELEON).centerC)
        assertNull(registry.definition(PersonalityId.CHAMELEON).centerD)
        assertNull(registry.definition(PersonalityId.WOLF).centerC)
        assertNull(registry.definition(PersonalityId.WOLF).centerD)
    }

    @Test
    fun `priority - 唯一且按 §5_3 目录序 1 到 12`() {
        val expected = mapOf(
            PersonalityId.ROOSTER to 1,
            PersonalityId.EARLY_BIRD to 2,
            PersonalityId.BEAR to 3,
            PersonalityId.WORK_HORSE to 4,
            PersonalityId.HOUND to 5,
            PersonalityId.SLOTH to 6,
            PersonalityId.OTTER to 7,
            PersonalityId.BAT to 8,
            PersonalityId.NIGHT_OWL to 9,
            PersonalityId.OWL to 10,
            PersonalityId.CHAMELEON to 11,
            PersonalityId.WOLF to 12,
        )
        expected.forEach { (id, priority) -> assertEquals("$id priority", priority, registry.definition(id).priority) }
        assertEquals(expected.size, registry.definitions.map { it.priority }.distinct().size)
        // OTTER priority 7 > SLOTH 6：R=0.6 同分时 OTTER 胜出（§5.3）
        assertTrue(registry.definition(PersonalityId.OTTER).priority > registry.definition(PersonalityId.SLOTH).priority)
    }

    @Test
    fun `minMembershipScore - 全部为 0_25（V2_0_7）`() {
        registry.definitions.forEach {
            assertEquals("${it.id} minMembershipScore", 0.25, it.minMembershipScore, 1e-12)
        }
        assertEquals(0.25, PersonalityConstants.MIN_MEMBERSHIP_SCORE, 1e-12)
    }

    @Test
    fun `transitionTargets - 与规范 §22 邻接表完全一致`() {
        val expected = mapOf(
            PersonalityId.ROOSTER to listOf(PersonalityId.EARLY_BIRD, PersonalityId.WORK_HORSE),
            PersonalityId.EARLY_BIRD to listOf(PersonalityId.ROOSTER, PersonalityId.BEAR, PersonalityId.HOUND),
            PersonalityId.BEAR to listOf(PersonalityId.EARLY_BIRD, PersonalityId.SLOTH, PersonalityId.OTTER),
            PersonalityId.WORK_HORSE to listOf(PersonalityId.HOUND, PersonalityId.ROOSTER, PersonalityId.BAT),
            PersonalityId.HOUND to listOf(
                PersonalityId.EARLY_BIRD, PersonalityId.NIGHT_OWL,
                PersonalityId.WORK_HORSE, PersonalityId.SLOTH, PersonalityId.OTTER,
            ),
            PersonalityId.SLOTH to listOf(PersonalityId.OTTER, PersonalityId.HOUND, PersonalityId.OWL),
            PersonalityId.OTTER to listOf(PersonalityId.SLOTH, PersonalityId.HOUND, PersonalityId.OWL),
            PersonalityId.BAT to listOf(PersonalityId.NIGHT_OWL, PersonalityId.WORK_HORSE),
            PersonalityId.NIGHT_OWL to listOf(PersonalityId.HOUND, PersonalityId.BAT, PersonalityId.OWL),
            PersonalityId.OWL to listOf(PersonalityId.NIGHT_OWL, PersonalityId.SLOTH, PersonalityId.OTTER),
        )
        expected.forEach { (id, targets) ->
            assertEquals("$id transitionTargets", targets, registry.definition(id).transitionTargets)
        }
    }

    @Test
    fun `gates - CHAMELEON targets 为全部 10 个 Base`() {
        val baseIds = registry.baseDefinitions.map { it.id }.toSet()
        assertEquals(baseIds, registry.definition(PersonalityId.CHAMELEON).transitionTargets.toSet())
    }

    @Test
    fun `gates - WOLF targets 为 NIGHT_OWL 与 OWL`() {
        assertEquals(
            listOf(PersonalityId.NIGHT_OWL, PersonalityId.OWL),
            registry.definition(PersonalityId.WOLF).transitionTargets,
        )
    }

    @Test
    fun `gates - Special Gate 不是普通 C×D Region`() {
        registry.specialGateDefinitions.forEach { d ->
            assertTrue("${d.id} 应为 Special Gate", d.isSpecialGate)
            assertNull("${d.id} 不得有 centerC", d.centerC)
            assertNull("${d.id} 不得有 centerD", d.centerD)
            assertNull("${d.id} 不得有 chronotypeRegion", d.chronotypeRegion)
            assertNull("${d.id} 不得有 durationRegion", d.durationRegion)
        }
        registry.baseDefinitions.forEach { d ->
            assertFalse("${d.id} 不应是 Special Gate", d.isSpecialGate)
            assertNotNull("${d.id} 必须有 centerC", d.centerC)
            assertNotNull("${d.id} 必须有 centerD", d.centerD)
        }
    }

    @Test
    fun `旧 CHAOS 不存在于 Primary Registry`() {
        assertFalse(PersonalityId.entries.any { it.name == "CHAOS" })
        assertTrue(registry.definitions.none { it.name.contains("混沌") })
    }

    @Test
    fun `Boundary 不是 PersonalityDefinition`() {
        assertFalse(PersonalityId.entries.any { it.name == "BOUNDARY" })
        assertTrue(registry.definitions.none { it.name.contains("Boundary") || it.name.contains("未分类") })
    }

    @Test
    fun `CHAMELEON 文案 - 不声称每天入睡时间随机`() {
        val d = registry.definition(PersonalityId.CHAMELEON)
        assertFalse(d.description.contains("每天"))
        assertFalse(d.description.contains("随机"))
        assertFalse(d.culturalMeaning.contains("每天"))
    }
}
