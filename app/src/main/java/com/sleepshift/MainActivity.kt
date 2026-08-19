package com.sleepshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.sleepshift.ui.SettingsViewModel
import com.sleepshift.ui.SettingsViewModelFactory
import com.sleepshift.ui.SleepShiftApp
import com.sleepshift.ui.theme.SleepShiftTheme

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as SleepShiftApplication
        SettingsViewModelFactory(app.settingsRepository, app.timezoneScheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepShiftTheme {
                SleepShiftApp(viewModel = settingsViewModel)
            }
        }
    }
}
