package com.sleepshift.shuileme.model

import com.sleepshift.shuileme.reminder.ReminderPersonality

/**
 * 新用户体验（SL-4.5）状态。
 * 首次打开进入引导；完成后进入主页；已完成直接主页。
 */
data class OnboardingState(
    val completed: Boolean = false,
    val selectedPersonality: ReminderPersonality = ReminderPersonality.MOON,
    val targetSleepTime: Int = DEFAULT_TARGET_SLEEP_TIME_MIN,
    val targetWakeTime: Int = DEFAULT_TARGET_WAKE_TIME_MIN,
    /** SL-9.10：当前真实作息（第二组时间，睡眠人格分析核心数据） */
    val currentSleepTime: Int = DEFAULT_CURRENT_SLEEP_TIME_MIN,
    val currentWakeTime: Int = DEFAULT_CURRENT_WAKE_TIME_MIN,
    val virtualClockTutorialDone: Boolean = false,
    /** SL-9：由自报作息推算的初始人格倾向（低置信度） */
    val initialPersonality: SleepPersonalityType? = null,
) {
    /** 完成引导（保留选择，供持久化） */
    fun markCompleted(): OnboardingState =
        copy(completed = true, virtualClockTutorialDone = true)

    companion object {
        const val DEFAULT_TARGET_SLEEP_TIME_MIN = 23 * 60 // 23:00
        const val DEFAULT_TARGET_WAKE_TIME_MIN = 7 * 60   // 07:00
        const val DEFAULT_CURRENT_SLEEP_TIME_MIN = 2 * 60 // 02:00（当前现实作息）
        const val DEFAULT_CURRENT_WAKE_TIME_MIN = 12 * 60 // 12:00
    }
}

object OnboardingFlow {
    /** 是否展示引导（首次启动判定；已完成跳过） */
    fun shouldShowOnboarding(state: OnboardingState): Boolean = !state.completed
}
