package com.sleepshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sleepshift.ui.SettingsViewModel
import com.sleepshift.ui.SleepShiftApp
import com.sleepshift.ui.theme.SleepShiftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepShiftTheme {
                SleepShiftApp(viewModel = viewModel())
            }
        }
    }
}
