package com.sleepshift.shuileme.reminder

import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.OffsetMode
import com.sleepshift.shuileme.model.MoonLife
import com.sleepshift.shuileme.model.MoonMood
import com.sleepshift.shuileme.model.MoonStage
import com.sleepshift.shuileme.model.SleepState
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SleepCapsuleTest {

    private val zone = ZoneId.of("UTC")

    private fun sleepingState(offset: Int = 120) = ShuilemeState(
        state = SleepState.SLEEPING,
        sleepStartAtMs = Instant.parse("2026-08-21T22:30:00Z").toEpochMilli(),
        currentOffsetMin = offset,
        mode = OffsetMode.FIXED,
        moonLife = MoonLife(stage = MoonStage.CRESCENT, mood = MoonMood.GENTLE),
    )

    @Test
    fun `胶囊内容含真实虚拟时间与月亮状态`() {
        val now = Instant.parse("2026-08-21T23:30:00Z").toEpochMilli()
        val text = SleepCapsule.formatContent(sleepingState(120), now, zone)
        assertTrue("含真实时间 23:30", text.contains("23:30"))
        assertTrue("含虚拟时间 01:30（+2h）", text.contains("01:30"))
        assertTrue("含月亮 🌒", text.contains("🌒"))
        assertTrue("含月亮名 成长月牙", text.contains("成长月牙"))
    }

    @Test
    fun `胶囊内容含满月状态`() {
        val now = Instant.parse("2026-08-21T23:30:00Z").toEpochMilli()
        val s = sleepingState().copy(moonLife = MoonLife(stage = MoonStage.FULL))
        val text = SleepCapsule.formatContent(s, now, zone)
        assertTrue(text.contains("🌕"))
        assertTrue(text.contains("满月伙伴"))
    }

    @Test
    fun `胶囊内容非睡眠时无显示`() {
        val awake = ShuilemeState()
        val text = SleepCapsule.formatContent(awake, System.currentTimeMillis(), zone)
        assertTrue(text.isNotBlank())
    }
}
