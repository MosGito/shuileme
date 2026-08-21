package com.sleepshift.shuileme.reminder

/**
 * 提醒人格（SL-4）：三种初始风格。
 * 未来可由 SL-6 睡眠人格系统自动推荐/切换（本阶段仅手动选择）。
 */
enum class ReminderPersonality(val emoji: String, val displayName: String) {
    /** 🌙 温柔月亮 */
    MOON("🌙", "温柔月亮"),
    /** 😈 毒舌朋友 */
    SHARP("😈", "毒舌朋友"),
    /** 🐮🐴 牛马提醒 */
    WORK_HORSE("🐮🐴", "牛马提醒"),
}

/** 提醒类型 */
enum class ReminderType { SLEEP, LATE, WAKE_FEEDBACK }

/**
 * 提醒配置（DataStore 持久化）。
 *
 * 设计调整（SL-4 编码阶段）：
 * - 睡前提醒：目标睡眠时间 - [sleepReminderAdvanceMin]；
 * - 熬夜提醒：23:00-08:00 允许触发（不受静默窗口限制），每晚最多 [lateReminderMaxPerNight] 次；
 * - 其他通知（起床反馈）遵守静默窗口。
 */
data class ReminderProfile(
    val personality: ReminderPersonality = ReminderPersonality.MOON,
    val sleepReminderAdvanceMin: Int = 30,
    val lateReminderMaxPerNight: Int = 2,
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 8,
    val enabledSleepReminder: Boolean = true,
    val enabledLateReminder: Boolean = true,
    val enabledWakeFeedback: Boolean = true,
) {
    /** 熬夜提醒检查点（每小时触发一次判定，23:00 ~ 07:00） */
    val lateCheckpointHours: List<Int> get() = listOf(23, 0, 1, 2, 3, 4, 5, 6, 7)
}
