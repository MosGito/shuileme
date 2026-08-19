package com.sleepshift.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepshift.data.SettingsRepository
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel —— DataStore 驱动（阶段 3）。
 *
 * - 配置经 [SettingsRepository] 持久化，App 重启后自动恢复；
 * - UI 通过 [StateFlow] + `collectAsState` 订阅，修改后立即保存；
 * - 方法签名与阶段 2 保持一致，避免 UI 大量改动。
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<SleepShiftSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SleepShiftSettings(),
        )

    fun updateSettings(transform: (SleepShiftSettings) -> SleepShiftSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun setEnabled(value: Boolean) {
        if (value != settings.value.enabled) updateSettings { it.copy(enabled = value) }
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
