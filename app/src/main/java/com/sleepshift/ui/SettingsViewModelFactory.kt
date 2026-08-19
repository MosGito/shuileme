package com.sleepshift.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sleepshift.data.SettingsRepository
import com.sleepshift.time.TimezoneScheduler

/** ViewModel 工厂：将 Repository 单例与 Scheduler 注入 SettingsViewModel */
class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val scheduler: TimezoneScheduler,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
