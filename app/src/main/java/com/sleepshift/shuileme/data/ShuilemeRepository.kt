package com.sleepshift.shuileme.data

import android.content.Context
import android.os.BatteryManager
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sleepshift.shuileme.engine.OffsetMode
import com.sleepshift.shuileme.engine.VirtualClockConfig
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.model.MoonEventPool
import com.sleepshift.shuileme.model.MoonLife
import com.sleepshift.shuileme.model.OnboardingState
import com.sleepshift.shuileme.model.MoonMood
import com.sleepshift.shuileme.model.MoonProgress
import com.sleepshift.shuileme.model.MoonStage
import com.sleepshift.shuileme.model.SleepDetectiveData
import com.sleepshift.shuileme.model.SleepResult
import com.sleepshift.shuileme.model.SleepResultReason
import com.sleepshift.shuileme.model.SleepSession
import com.sleepshift.shuileme.model.SleepState
import com.sleepshift.shuileme.model.computeMoonMood
import com.sleepshift.shuileme.reminder.ReminderPersonality
import com.sleepshift.shuileme.reminder.ReminderProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Context.shuilemeDataStore by preferencesDataStore(name = "shuileme")

/** 「睡了么」聚合状态（UI 唯一消费对象） */
data class ShuilemeState(
    val state: SleepState = SleepState.AWAKE,
    /** 当前会话入睡时刻（睡眠态非空） */
    val sleepStartAtMs: Long? = null,
    /** 当前偏移（分钟） */
    val currentOffsetMin: Int = VirtualClockEngine.DEFAULT_OFFSET_MIN,
    /** 偏移模式 */
    val mode: OffsetMode = OffsetMode.FIXED,
    val gradualStepMin: Int = VirtualClockEngine.DEFAULT_GRADUAL_STEP_MIN,
    val fluctuationRangeMin: Int = VirtualClockEngine.DEFAULT_FLUCTUATION_RANGE_MIN,
    /** 总睡眠次数 */
    val sleepCount: Int = 0,
    /** 连续睡眠天数 */
    val streakDays: Int = 0,
    /** 历史会话（最近 N 条，SL-6 人格分析数据源） */
    val sessions: List<SleepSession> = emptyList(),
    /** 最近一次会话 */
    val lastSession: SleepSession? = null,
    /** 月亮成长状态（SL-2-5） */
    val moonProgress: MoonProgress = MoonProgress(),
    /** 目标入睡时间（当日 00:00 起分钟，如 23:00=1380），用于合格判定 */
    val targetSleepTimeMin: Int = DEFAULT_TARGET_SLEEP_TIME_MIN,
    /** 提醒配置（SL-4） */
    val reminderProfile: ReminderProfile = ReminderProfile(),
    /** 今晚放过我（SL-4）：active + 生效日 epochDay */
    val nightOffActive: Boolean = false,
    val nightOffDate: Long? = null,
    /** 今晚熬夜提醒已发次数（SL-4） */
    val lateReminderCount: Int = 0,
    val lateReminderDate: Long? = null,
    /** 月亮生命（SL-5） */
    val moonLife: MoonLife = MoonLife(),
    /** 新用户体验（SL-4.5） */
    val onboarding: OnboardingState = OnboardingState(),
    /** 睡眠侦探（SL-7，弱信号） */
    val sleepDetectiveData: SleepDetectiveData = SleepDetectiveData(),
) {
    val isSleeping: Boolean get() = state == SleepState.SLEEPING
    /** 由配置构造引擎配置（供 UI 计算虚拟时间） */
    fun toVirtualClockConfig(): VirtualClockConfig =
        VirtualClockConfig(currentOffsetMin, mode, gradualStepMin, fluctuationRangeMin)

    companion object {
        const val DEFAULT_TARGET_SLEEP_TIME_MIN = 1380 // 23:00
    }
}

/**
 * 「睡了么」数据仓库（Preferences DataStore，独立于 Legacy SleepShift 数据文件）。
 *
 * 持久化：
 * - 睡眠状态（awake/sleeping）与当前会话时刻；
 * - 用户配置（偏移/模式）；
 * - 会话历史（最近 60 条，JSON），供 SL-6 人格分析；
 * - 派生：总次数 / 连续天数。
 */
class ShuilemeRepository(private val context: Context) {

    private val dataStore = context.applicationContext.shuilemeDataStore

    private object Keys {
        val STATE = intPreferencesKey("state") // 0=awake, 1=sleeping
        val SLEEP_START_MS = longPreferencesKey("sleep_start_ms")
        val OFFSET_MIN = intPreferencesKey("offset_min")
        val MODE = intPreferencesKey("mode")
        val GRADUAL_STEP_MIN = intPreferencesKey("gradual_step_min")
        val FLUCT_RANGE_MIN = intPreferencesKey("fluct_range_min")
        val SLEEP_COUNT = intPreferencesKey("sleep_count")
        val SESSION_HISTORY_JSON = stringPreferencesKey("session_history_json")
        val LAST_SESSION_JSON = stringPreferencesKey("last_session_json")
        // 月亮成长（SL-2-5）
        val TARGET_SLEEP_TIME_MIN = intPreferencesKey("target_sleep_time_min")
        val GROWTH_PERCENT = intPreferencesKey("growth_percent")
        val CONSECUTIVE_QUALIFIED = intPreferencesKey("consecutive_qualified")
        val TOTAL_COMPLETED_SLEEPS = intPreferencesKey("total_completed_sleeps")
        val LAST_SLEEP_RESULT_JSON = stringPreferencesKey("last_sleep_result_json")
        val FULL_MOON_REWARD_PENDING = booleanPreferencesKey("full_moon_reward_pending")
        // 提醒（SL-4）
        val PERSONALITY = intPreferencesKey("reminder_personality")
        val SLEEP_ADVANCE_MIN = intPreferencesKey("sleep_reminder_advance_min")
        val LATE_MAX_PER_NIGHT = intPreferencesKey("late_reminder_max")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        val ENABLE_SLEEP = booleanPreferencesKey("enable_sleep_reminder")
        val ENABLE_LATE = booleanPreferencesKey("enable_late_reminder")
        val ENABLE_WAKE = booleanPreferencesKey("enable_wake_feedback")
        val NIGHT_OFF_ACTIVE = booleanPreferencesKey("night_off_active")
        val NIGHT_OFF_DATE = longPreferencesKey("night_off_date")
        val LATE_COUNT = intPreferencesKey("late_reminder_count")
        val LATE_DATE = longPreferencesKey("late_reminder_date")
        // 月亮生命（SL-5）
        val MOOD = intPreferencesKey("moon_mood")
        val LAST_EVENT = stringPreferencesKey("moon_last_event")
        val LAST_EVENT_DATE = longPreferencesKey("moon_last_event_date")
        val TOTAL_REWARDS = intPreferencesKey("moon_total_rewards")
        val RECENT_LATE_NIGHTS = intPreferencesKey("recent_late_nights")
        // 碎碎念（SL-6）
        val TALK_COUNT = intPreferencesKey("talk_count")
        val TALK_DATE = longPreferencesKey("talk_date")
        // 新用户体验（SL-4.5）
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ONBOARDING_PERSONALITY = intPreferencesKey("onboarding_personality")
        val ONBOARDING_TARGET_TIME = intPreferencesKey("onboarding_target_time")
        val ONBOARDING_TUTORIAL_DONE = booleanPreferencesKey("onboarding_tutorial_done")
        // 睡眠侦探（SL-7）
        val NIGHT_HINTS = intPreferencesKey("night_hints")
        val CHARGING_DURATION = longPreferencesKey("charging_duration")
    }

    val state: Flow<ShuilemeState> = dataStore.data.map { p ->
        val sessions = decodeSessions(p[Keys.SESSION_HISTORY_JSON])
        ShuilemeState(
            state = if (p[Keys.STATE] == 1) SleepState.SLEEPING else SleepState.AWAKE,
            sleepStartAtMs = p[Keys.SLEEP_START_MS],
            currentOffsetMin = p[Keys.OFFSET_MIN] ?: VirtualClockEngine.DEFAULT_OFFSET_MIN,
            mode = OffsetMode.entries.getOrElse(p[Keys.MODE] ?: 0) { OffsetMode.FIXED },
            gradualStepMin = p[Keys.GRADUAL_STEP_MIN] ?: VirtualClockEngine.DEFAULT_GRADUAL_STEP_MIN,
            fluctuationRangeMin = p[Keys.FLUCT_RANGE_MIN] ?: VirtualClockEngine.DEFAULT_FLUCTUATION_RANGE_MIN,
            sleepCount = p[Keys.SLEEP_COUNT] ?: 0,
            streakDays = computeStreak(sessions),
            sessions = sessions,
            lastSession = decodeSession(p[Keys.LAST_SESSION_JSON]),
            moonProgress = readMoonProgress(p),
            targetSleepTimeMin = p[Keys.TARGET_SLEEP_TIME_MIN] ?: ShuilemeState.DEFAULT_TARGET_SLEEP_TIME_MIN,
            reminderProfile = ReminderProfile(
                personality = ReminderPersonality.entries.getOrElse(p[Keys.PERSONALITY] ?: 0) { ReminderPersonality.MOON },
                sleepReminderAdvanceMin = p[Keys.SLEEP_ADVANCE_MIN] ?: ReminderProfile().sleepReminderAdvanceMin,
                lateReminderMaxPerNight = p[Keys.LATE_MAX_PER_NIGHT] ?: ReminderProfile().lateReminderMaxPerNight,
                quietStartHour = p[Keys.QUIET_START_HOUR] ?: ReminderProfile().quietStartHour,
                quietEndHour = p[Keys.QUIET_END_HOUR] ?: ReminderProfile().quietEndHour,
                enabledSleepReminder = p[Keys.ENABLE_SLEEP] ?: ReminderProfile().enabledSleepReminder,
                enabledLateReminder = p[Keys.ENABLE_LATE] ?: ReminderProfile().enabledLateReminder,
                enabledWakeFeedback = p[Keys.ENABLE_WAKE] ?: ReminderProfile().enabledWakeFeedback,
            ),
            nightOffActive = p[Keys.NIGHT_OFF_ACTIVE] ?: false,
            nightOffDate = p[Keys.NIGHT_OFF_DATE],
            lateReminderCount = p[Keys.LATE_COUNT] ?: 0,
            lateReminderDate = p[Keys.LATE_DATE],
            moonLife = MoonLife(
                stage = if (p[Keys.FULL_MOON_REWARD_PENDING] ?: false) MoonStage.FULL
                else MoonStage.fromGrowth(p[Keys.GROWTH_PERCENT] ?: 0),
                mood = MoonMood.entries.getOrElse(p[Keys.MOOD] ?: 0) { MoonMood.EXPECTANT },
                lastEvent = p[Keys.LAST_EVENT],
                lastEventDate = p[Keys.LAST_EVENT_DATE],
                totalRewards = p[Keys.TOTAL_REWARDS] ?: 0,
                recentLateNights = p[Keys.RECENT_LATE_NIGHTS] ?: 0,
            ),
            onboarding = OnboardingState(
                completed = p[Keys.ONBOARDING_COMPLETED] ?: false,
                selectedPersonality = ReminderPersonality.entries.getOrElse(p[Keys.ONBOARDING_PERSONALITY] ?: 0) { ReminderPersonality.MOON },
                targetSleepTime = p[Keys.ONBOARDING_TARGET_TIME] ?: OnboardingState.DEFAULT_TARGET_SLEEP_TIME_MIN,
                virtualClockTutorialDone = p[Keys.ONBOARDING_TUTORIAL_DONE] ?: false,
            ),
            sleepDetectiveData = SleepDetectiveData(
                sleepStartTime = sessions.lastOrNull()?.sleepStartAtMs,
                wakeUpTime = sessions.lastOrNull()?.wakeAtMs,
                selfReportedSleepDuration = sessions.lastOrNull()?.durationMin,
                chargingDuration = p[Keys.CHARGING_DURATION] ?: 0,
                nightActivityHints = p[Keys.NIGHT_HINTS] ?: 0,
            ),
        )
    }

    suspend fun current(): ShuilemeState = state.first()

    /** 点「我要睡了 🌙」：进入睡眠态，记录入睡时刻与当夜偏移（顺带结算上次满月奖励） */
    suspend fun startSleep(sleepStartAtMs: Long, effectiveOffsetMin: Int) {
        dataStore.edit { p ->
            p[Keys.STATE] = 1
            p[Keys.SLEEP_START_MS] = sleepStartAtMs
            p[Keys.OFFSET_MIN] = effectiveOffsetMin
            p[Keys.FULL_MOON_REWARD_PENDING] = false
            // SL-7：重置当晚侦探线索
            p[Keys.NIGHT_HINTS] = 0
            p[Keys.CHARGING_DURATION] = 0
        }
    }

    /** SL-7：睡眠窗口内 App 打开 → 记一条活动线索（弱信号） */
    suspend fun incrementNightHint() {
        dataStore.edit { p ->
            p[Keys.NIGHT_HINTS] = (p[Keys.NIGHT_HINTS] ?: 0) + 1
        }
    }

    /** 点「我醒啦 ☀️」：结束会话，记录历史，更新次数/连续天数 + 月亮成长判定 */
    suspend fun wakeUp(wakeAtMs: Long) {
        // 事务外读当前月亮状态（dataStore.data 为 Preferences；edit 内为 MutablePreferences）
        val currentMoon = readMoonProgress(dataStore.data.first())
        dataStore.edit { p ->
            val start = p[Keys.SLEEP_START_MS] ?: return@edit
            if (wakeAtMs <= start) return@edit
            p[Keys.STATE] = 0
            p.remove(Keys.SLEEP_START_MS)
            val session = SleepSession(
                sleepStartAtMs = start,
                wakeAtMs = wakeAtMs,
                effectiveOffsetMin = p[Keys.OFFSET_MIN] ?: VirtualClockEngine.DEFAULT_OFFSET_MIN,
            )
            p[Keys.LAST_SESSION_JSON] = encodeSession(session)
            p[Keys.SLEEP_COUNT] = (p[Keys.SLEEP_COUNT] ?: 0) + 1
            val list = decodeSessions(p[Keys.SESSION_HISTORY_JSON]).toMutableList()
            list += session
            if (list.size > MAX_SESSION_HISTORY) list.removeAt(0)
            p[Keys.SESSION_HISTORY_JSON] = encodeSessions(list)
            // 月亮成长：判定本次睡眠并应用（内联写入 MutablePreferences）
            val target = p[Keys.TARGET_SLEEP_TIME_MIN] ?: ShuilemeState.DEFAULT_TARGET_SLEEP_TIME_MIN
            val next = currentMoon.applyResult(currentMoon.evaluate(start, wakeAtMs, target))
            p[Keys.GROWTH_PERCENT] = next.growthPercent
            p[Keys.CONSECUTIVE_QUALIFIED] = next.consecutiveQualified
            p[Keys.TOTAL_COMPLETED_SLEEPS] = next.totalCompletedSleeps
            p[Keys.LAST_SLEEP_RESULT_JSON] = next.lastSleepResult?.let { encodeSleepResult(it) } ?: ""
            p[Keys.FULL_MOON_REWARD_PENDING] = next.fullMoonRewardPending
            // SL-5：熬夜次数 / 情绪 / 满月奖励 / 每日事件（每日最多一次）
            val lateNights = if (next.lastSleepResult?.reason == SleepResultReason.STARTED_TOO_LATE)
                (p[Keys.RECENT_LATE_NIGHTS] ?: 0) + 1 else 0
            p[Keys.RECENT_LATE_NIGHTS] = lateNights
            val mood = computeMoonMood(
                sleepQualified = next.lastSleepResult?.isQualified,
                consecutiveQualified = next.consecutiveQualified,
                recentLateCount = lateNights,
                rewardPending = next.fullMoonRewardPending,
            )
            p[Keys.MOOD] = mood.ordinal
            if (next.fullMoonRewardPending) {
                p[Keys.TOTAL_REWARDS] = (p[Keys.TOTAL_REWARDS] ?: 0) + 1
            }
            val today = epochDay(wakeAtMs)
            if (p[Keys.LAST_EVENT_DATE] != today) {
                p[Keys.LAST_EVENT] = MoonEventPool.pick(p[Keys.SLEEP_COUNT] ?: 0)
                p[Keys.LAST_EVENT_DATE] = today
            }
            // SL-7：充电估算（弱信号：醒时在充 → 假设睡了一半时间在充）
            val charging = context.getSystemService(BatteryManager::class.java)?.isCharging == true
            p[Keys.CHARGING_DURATION] = if (charging) (wakeAtMs - start) / 120_000L else 0L
        }
    }

    /** 满月奖励已展示，清除待展示标记（也可由下次 startSleep 自动结算） */
    suspend fun dismissFullMoonReward() {
        dataStore.edit { p -> p[Keys.FULL_MOON_REWARD_PENDING] = false }
    }

    /** 设置目标入睡时间（当日 00:00 起分钟） */
    suspend fun setTargetSleepTime(targetSleepTimeMin: Int) {
        dataStore.edit { p -> p[Keys.TARGET_SLEEP_TIME_MIN] = targetSleepTimeMin }
    }

    /** 完成新用户体验（SL-4.5）：持久化引导状态 + 同步人格与目标入睡时间 */
    suspend fun completeOnboarding(selection: OnboardingState) {
        dataStore.edit { p ->
            p[Keys.ONBOARDING_COMPLETED] = true
            p[Keys.ONBOARDING_PERSONALITY] = selection.selectedPersonality.ordinal
            p[Keys.ONBOARDING_TARGET_TIME] = selection.targetSleepTime
            p[Keys.ONBOARDING_TUTORIAL_DONE] = selection.virtualClockTutorialDone
            p[Keys.PERSONALITY] = selection.selectedPersonality.ordinal
            p[Keys.TARGET_SLEEP_TIME_MIN] = selection.targetSleepTime
        }
    }

    /** 标记虚拟时间教学完成 */
    suspend fun setTutorialDone(done: Boolean) {
        dataStore.edit { p -> p[Keys.ONBOARDING_TUTORIAL_DONE] = done }
    }

    /** 更新提醒配置（SL-4） */
    suspend fun updateReminderProfile(profile: ReminderProfile) {
        dataStore.edit { p ->
            p[Keys.PERSONALITY] = profile.personality.ordinal
            p[Keys.SLEEP_ADVANCE_MIN] = profile.sleepReminderAdvanceMin
            p[Keys.LATE_MAX_PER_NIGHT] = profile.lateReminderMaxPerNight
            p[Keys.QUIET_START_HOUR] = profile.quietStartHour
            p[Keys.QUIET_END_HOUR] = profile.quietEndHour
            p[Keys.ENABLE_SLEEP] = profile.enabledSleepReminder
            p[Keys.ENABLE_LATE] = profile.enabledLateReminder
            p[Keys.ENABLE_WAKE] = profile.enabledWakeFeedback
        }
    }

    /** 今晚放过我 🌙（SL-4）：active + 生效日 epochDay */
    suspend fun setNightOff(active: Boolean, epochDay: Long) {
        dataStore.edit { p ->
            p[Keys.NIGHT_OFF_ACTIVE] = active
            p[Keys.NIGHT_OFF_DATE] = epochDay
        }
    }

    /** 今日碎碎念次数（跨日重置） */
    suspend fun getTalkCount(epochDay: Long): Int {
        val p = dataStore.data.first()
        return if (p[Keys.TALK_DATE] == epochDay) p[Keys.TALK_COUNT] ?: 0 else 0
    }

    /** 记录一条碎碎念（按日计数） */
    suspend fun incrementTalk(epochDay: Long) {
        dataStore.edit { p ->
            p[Keys.TALK_COUNT] = if (p[Keys.TALK_DATE] == epochDay) (p[Keys.TALK_COUNT] ?: 0) + 1 else 1
            p[Keys.TALK_DATE] = epochDay
        }
    }

    /** 记录一条熬夜提醒（按日计数，跨日重置）；返回当夜累计次数 */
    suspend fun incrementLateReminder(epochDay: Long): Int {
        dataStore.edit { p ->
            val count = if (p[Keys.LATE_DATE] == epochDay) (p[Keys.LATE_COUNT] ?: 0) + 1 else 1
            p[Keys.LATE_COUNT] = count
            p[Keys.LATE_DATE] = epochDay
        }
        return dataStore.data.first()[Keys.LATE_COUNT] ?: 0
    }

    /** 更新用户配置（偏移/模式） */
    suspend fun updateConfig(
        offsetMin: Int,
        mode: OffsetMode,
        gradualStepMin: Int = VirtualClockEngine.DEFAULT_GRADUAL_STEP_MIN,
        fluctuationRangeMin: Int = VirtualClockEngine.DEFAULT_FLUCTUATION_RANGE_MIN,
    ) {
        dataStore.edit { p ->
            p[Keys.OFFSET_MIN] = offsetMin.coerceIn(VirtualClockConfig.MIN_OFFSET_MIN, VirtualClockConfig.MAX_OFFSET_MIN)
            p[Keys.MODE] = mode.ordinal
            p[Keys.GRADUAL_STEP_MIN] = gradualStepMin
            p[Keys.FLUCT_RANGE_MIN] = fluctuationRangeMin
        }
    }

    /** 连续睡眠天数：从最近一次会话日起向前连续的天数 */
    private fun computeStreak(sessions: List<SleepSession>): Int {
        val days = sessions
            .mapNotNull { it.sleepStartAtMs }
            .map { epochDay(it) }
            .distinct()
            .sortedDescending()
        if (days.isEmpty()) return 0
        var streak = 1
        var expected = days[0] - 1
        for (i in 1 until days.size) {
            if (days[i] == expected) {
                streak++
                expected--
            } else break
        }
        return streak
    }

    private fun epochDay(ms: Long): Long =
        LocalDate.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).toEpochDay()

    // ── 月亮成长读写 ──

    private fun readMoonProgress(p: Preferences): MoonProgress = MoonProgress(
        growthPercent = p[Keys.GROWTH_PERCENT] ?: 0,
        consecutiveQualified = p[Keys.CONSECUTIVE_QUALIFIED] ?: 0,
        totalCompletedSleeps = p[Keys.TOTAL_COMPLETED_SLEEPS] ?: 0,
        lastSleepResult = decodeSleepResult(p[Keys.LAST_SLEEP_RESULT_JSON]),
        fullMoonRewardPending = p[Keys.FULL_MOON_REWARD_PENDING] ?: false,
    )

    private fun encodeSleepResult(r: SleepResult): String = JSONObject().apply {
        put("durationMin", r.durationMinutes)
        put("qualified", r.isQualified)
        if (r.reason != null) put("reason", r.reason.name)
    }.toString()

    private fun decodeSleepResult(json: String?): SleepResult? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(json)
            SleepResult(
                durationMinutes = o.getLong("durationMin"),
                isQualified = o.getBoolean("qualified"),
                reason = if (o.has("reason")) SleepResultReason.valueOf(o.getString("reason")) else null,
            )
        }.getOrNull()
    }

    // ── 会话历史 JSON 编解码 ──

    private fun encodeSession(s: SleepSession): String = JSONObject().apply {
        put("sleepStartAtMs", s.sleepStartAtMs)
        if (s.wakeAtMs != null) put("wakeAtMs", s.wakeAtMs)
        put("offset", s.effectiveOffsetMin)
    }.toString()

    private fun decodeSession(json: String?): SleepSession? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(json)
            SleepSession(
                sleepStartAtMs = o.getLong("sleepStartAtMs"),
                wakeAtMs = if (o.has("wakeAtMs")) o.getLong("wakeAtMs") else null,
                effectiveOffsetMin = o.optInt("offset", 0),
            )
        }.getOrNull()
    }

    private fun encodeSessions(sessions: List<SleepSession>): String {
        val arr = JSONArray()
        sessions.forEach { arr.put(JSONObject(encodeSession(it))) }
        return arr.toString()
    }

    private fun decodeSessions(json: String?): List<SleepSession> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length())
                .map { decodeSession(arr.getJSONObject(it).toString()) }
                .filterNotNull()
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val MAX_SESSION_HISTORY = 60
    }
}
