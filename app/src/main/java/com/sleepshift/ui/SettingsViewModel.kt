package com.sleepshift.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepshift.data.SettingsRepository
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings
import com.sleepshift.time.TimezoneScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel —— DataStore 驱动（阶段 3）+ 调度联动（阶段 5）。
 *
 * - 配置经 [SettingsRepository] 持久化，App 重启后自动恢复；
 * - UI 通过 [StateFlow] + `collectAsState` 订阅，修改后立即保存；
 * - **配置变更后重新武装**（[TimezoneScheduler.arm]），保证旧闹钟不会按旧配置错误执行；
 * - **启用→arm()；停用→cancel() + 恢复原始时区**。
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val scheduler: TimezoneScheduler,
) : ViewModel() {

    val settings: StateFlow<SleepShiftSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SleepShiftSettings(),
        )

    /** 首次启动引导是否已完成 */
    val onboardingDone: StateFlow<Boolean> = repository.onboardingDone
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun setOnboardingDone(done: Boolean) {
        viewModelScope.launch { repository.setOnboardingDone(done) }
    }

    /** 配置变更：写入 DataStore 并重新武装（未启用时 arm() 内部会 cancel） */
    fun updateSettings(transform: (SleepShiftSettings) -> SleepShiftSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
            scheduler.arm()
        }
    }

    /** 启用 → arm()；停用 → cancel() + 恢复原始时区 */
    fun setEnabled(value: Boolean) {
        if (value == settings.value.enabled) return
        viewModelScope.launch {
            repository.updateSettings { it.copy(enabled = value) }
            if (value) {
                scheduler.arm()
            } else {
                scheduler.cancel()
                scheduler.applyRestore()
            }
        }
    }

    fun setStartTimeMin(value: Int) {
        if (value != settings.value.startTimeMin) updateSettings { it.copy(startTimeMin = value) }
    }

    fun setRestoreTimeMin(value: Int) {
        if (value != settings.value.restoreTimeMin) updateSettings { it.copy(restoreTimeMin = value) }
    }

    fun setOffsetMin(value: Int) {
        if (value != settings.value.offsetMin) updateSettings { it.copy(offsetMin = value) }
    }

    fun setMode(value: SleepShiftMode) {
        if (value != settings.value.mode) updateSettings { it.copy(mode = value) }
    }

    fun setGradualStepMin(value: Int) {
        if (value != settings.value.gradualStepMin) updateSettings { it.copy(gradualStepMin = value) }
    }

    fun setFluctuationRangeMin(value: Int) {
        if (value != settings.value.fluctuationRangeMin) updateSettings { it.copy(fluctuationRangeMin = value) }
    }
}
