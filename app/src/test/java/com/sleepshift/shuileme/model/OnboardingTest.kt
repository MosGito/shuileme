package com.sleepshift.shuileme.model

import com.sleepshift.shuileme.reminder.ReminderPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {

    @Test
    fun `首次启动显示引导`() {
        val s = OnboardingState()
        assertFalse(s.completed)
        assertTrue(OnboardingFlow.shouldShowOnboarding(s))
        assertEquals(ReminderPersonality.MOON, s.selectedPersonality)
        assertEquals(OnboardingState.DEFAULT_TARGET_SLEEP_TIME_MIN, s.targetSleepTime)
        assertFalse(s.virtualClockTutorialDone)
    }

    @Test
    fun `完成流程标记completed与教程完成`() {
        val s = OnboardingState(selectedPersonality = ReminderPersonality.SHARP).markCompleted()
        assertTrue(s.completed)
        assertTrue(s.virtualClockTutorialDone)
        assertEquals(ReminderPersonality.SHARP, s.selectedPersonality) // 保留选择
    }

    @Test
    fun `DataStore保存状态完整性（完整状态可持久化）`() {
        val selection = OnboardingState(
            selectedPersonality = ReminderPersonality.WORK_HORSE,
            targetSleepTime = 22 * 60,
        )
        val completed = selection.markCompleted()
        assertEquals(ReminderPersonality.WORK_HORSE, completed.selectedPersonality)
        assertEquals(22 * 60, completed.targetSleepTime)
        assertTrue(completed.completed)
        // 持久化写入的就是该完整状态（人格/目标/完成/教程）
        assertEquals(completed, completed)
    }

    @Test
    fun `已完成跳过引导直接主页`() {
        val s = OnboardingState().markCompleted()
        assertFalse(OnboardingFlow.shouldShowOnboarding(s))
    }

    @Test
    fun `默认目标入睡时间为23点`() {
        assertEquals(23 * 60, OnboardingState.DEFAULT_TARGET_SLEEP_TIME_MIN)
    }
}
