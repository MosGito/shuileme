package com.sleepshift

import android.app.Application
import com.sleepshift.data.SettingsRepository

/**
 * 应用级单例容器。
 * Repository 作为唯一数据访问入口，由 Application 统一持有并注入 ViewModel，
 * 保证 UI 层不直接接触 DataStore。
 */
class SleepShiftApplication : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
