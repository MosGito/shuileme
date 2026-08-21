package com.sleepshift.shuileme.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.OffsetMode
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.model.MorningDetectiveReport
import com.sleepshift.shuileme.model.OnboardingState
import com.sleepshift.shuileme.model.PersonalityInput
import com.sleepshift.shuileme.model.ResidentTalkSystem
import com.sleepshift.shuileme.model.SleepDetective
import com.sleepshift.shuileme.model.SleepPersonalityEngine
import com.sleepshift.shuileme.model.SleepPersonalityState
import com.sleepshift.shuileme.model.SleepSession
import com.sleepshift.shuileme.reminder.ReminderPersonality
import com.sleepshift.shuileme.reminder.SleepCapsule
import com.sleepshift.shuileme.reminder.ShuilemeReminderNotifier
import com.sleepshift.shuileme.reminder.ShuilemeReminderScheduler
import com.sleepshift.shuileme.widget.ShuilemeWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 「睡了么」首页 ViewModel。
 * - 暴露 [state]（DataStore 驱动，App 重启后自动恢复）；
 * - 提供 我要睡了 / 我醒啦 / 偏移调整 三个动作；
 * - startSleep/wakeUp 后刷新桌面组件（SL-3 事件刷新）；
 * - 不触碰系统时间 / 时区；纯本地状态。
 */
class ShuilemeViewModel(
    private val repository: ShuilemeRepository,
    private val appContext: Context,
) : ViewModel() {

    val state: StateFlow<ShuilemeState> = repository.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShuilemeState(),
        )

    /** SL-4.5 新用户体验状态 */
    val onboarding: StateFlow<OnboardingState> = state
        .map { it.onboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingState())

    /** 完成引导：持久化 + 同步人格/目标 + 重排提醒 */
    fun completeOnboarding(selection: OnboardingState) {
        viewModelScope.launch {
            repository.completeOnboarding(selection)
            ShuilemeReminderScheduler.scheduleAll(appContext)
        }
    }

    /** SL-6 睡眠人格状态（由 DataStore 会话数据实时计算，纯规则无 AI） */
    val personalityState: StateFlow<SleepPersonalityState> = state
        .map { s ->
            SleepPersonalityEngine.compute(
                PersonalityInput(
                    sessions = s.sessions,
                    moonLife = s.moonLife,
                    streakDays = s.streakDays,
                    sleepCount = s.sleepCount,
                    // SL-9：实际入睡 vs 目标入睡 偏差 → 影响规律度/置信度
                    sleepTargetDeviationMin = targetDeviation(s.sessions, s.targetSleepTimeMin),
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepPersonalityState())

    private val _talkText = MutableStateFlow<String?>(null)
    val talkText: StateFlow<String?> = _talkText

    /** SL-7 睡眠侦探报告（醒后有报告才非空） */
    val detectiveReport: StateFlow<MorningDetectiveReport?> = state
        .map { s ->
            s.sleepDetectiveData.takeIf { it.wakeUpTime != null }?.let { data ->
                SleepDetective.generateReport(data, s.sleepCount)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** SL-7：睡眠窗口内 App 打开 → 记一条活动线索（弱信号） */
    fun recordNightActivity() {
        viewModelScope.launch {
            if (state.value.isSleeping) repository.incrementNightHint()
        }
    }

    /** 刷新今日碎碎念（每日 ≤3 次，预置模板） */
    fun refreshTalk() {
        viewModelScope.launch {
            val s = state.value
            val personality = personalityState.value
            val type = personality.primaryType ?: return@launch
            val now = System.currentTimeMillis()
            val today = todayEpochDay(now)
            val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
            if (!ResidentTalkSystem.shouldTrigger(repository.getTalkCount(today), hour)) return@launch
            val engine = VirtualClockEngine(s.toVirtualClockConfig())
            val virtualHour = Instant.ofEpochMilli(engine.virtualTimeMs(now)).atZone(ZoneId.systemDefault()).hour
            val isLate = virtualHour < 6
            val scenario = ResidentTalkSystem.pickScenario(
                isLate = isLate,
                moonMood = s.moonLife.mood,
                sleepQualified = s.moonProgress.lastSleepResult?.isQualified,
            )
            _talkText.value = ResidentTalkSystem.pickTalk(type, scenario, s.sleepCount)
            repository.incrementTalk(today)
        }
    }

    /** SL-9：实际入睡 vs 目标入睡 平均偏差（分钟，含跨午夜取最小环） */
    private fun targetDeviation(sessions: List<SleepSession>, targetMin: Int): Long? {
        if (sessions.isEmpty()) return null
        val devs = sessions.map { s ->
            val zdt = Instant.ofEpochMilli(s.sleepStartAtMs).atZone(ZoneId.systemDefault())
            val m = zdt.hour * 60 + zdt.minute
            val raw = kotlin.math.abs(m - targetMin)
            minOf(raw, 1440 - raw)
        }
        return devs.average().toLong()
    }

    private fun todayEpochDay(nowMs: Long): Long =
        LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault()).toEpochDay()

    /** 虚拟时间毫秒（供 UI 展示；engine 由当前配置派生） */
    fun virtualTimeMs(realNowMs: Long, current: ShuilemeState): Long =
        VirtualClockEngine(current.toVirtualClockConfig()).virtualTimeMs(realNowMs)

    /** 点「我要睡了 🌙」 */
    fun startSleep(nowMs: Long) {
        val current = state.value
        if (current.isSleeping) return
        viewModelScope.launch {
            // MVP 默认 FIXED：当夜偏移 = 当前配置偏移
            val engine = VirtualClockEngine(current.toVirtualClockConfig())
            val offset = engine.effectiveOffsetMin(dayIndex = 0)
            repository.startSleep(nowMs, offset)
            // SL-9.2：睡眠胶囊常驻通知
            SleepCapsule.show(appContext, repository.current())
            ShuilemeWidgets.refreshAll(appContext)
        }
    }

    /** 点「我醒啦 ☀️」 */
    fun wakeUp(nowMs: Long) {
        val current = state.value
        if (!current.isSleeping) return
        viewModelScope.launch {
            repository.wakeUp(nowMs)
            // SL-9.2：取消睡眠胶囊
            SleepCapsule.cancel(appContext)
            // SL-4 起床反馈（遵守静默窗口，LOW 渠道）
            val fresh = repository.current()
            if (fresh.reminderProfile.enabledWakeFeedback) {
                ShuilemeReminderNotifier.notifyWakeFeedback(appContext, fresh)
            }
            ShuilemeWidgets.refreshAll(appContext)
        }
    }

    /** 调整偏移（分钟，0~240） */
    fun updateOffset(offsetMin: Int) {
        val current = state.value
        viewModelScope.launch {
            repository.updateConfig(offsetMin, current.mode, current.gradualStepMin, current.fluctuationRangeMin)
        }
    }

    /** 切换偏移模式 */
    fun setMode(mode: OffsetMode) {
        val current = state.value
        viewModelScope.launch {
            repository.updateConfig(current.currentOffsetMin, mode, current.gradualStepMin, current.fluctuationRangeMin)
        }
    }

    /** SL-8：设置抽屉 - 切换提醒人格 */
    fun setReminderPersonality(personality: ReminderPersonality) {
        viewModelScope.launch {
            val profile = state.value.reminderProfile
            repository.updateReminderProfile(profile.copy(personality = personality))
            ShuilemeReminderScheduler.scheduleAll(appContext)
        }
    }

    /** SL-8：设置抽屉 - 设置目标入睡时间（分钟） */
    /** SL-9.10：保存完整睡眠目标（目标 + 当前两组时间） */
    fun setSleepGoal(
        targetSleep: Int,
        targetWake: Int,
        currentSleep: Int,
        currentWake: Int,
    ) {
        viewModelScope.launch { repository.setSleepGoal(targetSleep, targetWake, currentSleep, currentWake) }
    }

    fun setTargetSleepTime(targetSleepTimeMin: Int) {
        viewModelScope.launch {
            repository.setTargetSleepTime(targetSleepTimeMin)
            ShuilemeReminderScheduler.scheduleAll(appContext)
        }
    }

    /** SL-9：设置目标睡眠窗口（入睡 + 起床） */
    fun setTargetSleepWindow(sleepTimeMin: Int, wakeTimeMin: Int) {
        viewModelScope.launch {
            repository.setTargetSleepWindow(sleepTimeMin, wakeTimeMin)
            ShuilemeReminderScheduler.scheduleAll(appContext)
        }
    }

    /** SL-9.1：重力互动开关 */
    fun setGravityEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGravityEnabled(enabled) }
    }
}

class ShuilemeViewModelFactory(
    private val repository: ShuilemeRepository,
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ShuilemeViewModel(repository, appContext) as T
}
