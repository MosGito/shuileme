package com.sleepshift.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sleepshift.DebugActivity
import com.sleepshift.ui.config.ConfigScreen
import com.sleepshift.ui.home.HomeScreen
import com.sleepshift.ui.mode.ModeScreen
import com.sleepshift.ui.onboarding.OnboardingScreen

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("今日", Icons.Filled.Home),
    CONFIG("配置", Icons.Filled.Settings),
    MODE("模式", Icons.Filled.DateRange),
}

/** 应用壳：首次启动引导 + 底部导航三页（阶段 2 用状态式导航） */
@Composable
fun SleepShiftApp(viewModel: SettingsViewModel) {
    val onboardingDone by viewModel.onboardingDone.collectAsState()
    if (!onboardingDone) {
        OnboardingScreen(onFinish = { viewModel.setOnboardingDone(true) })
        return
    }
    MainTabs(viewModel)
}

@Composable
private fun MainTabs(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.HOME -> HomeScreen(viewModel)
                AppTab.CONFIG -> ConfigScreen(viewModel)
                AppTab.MODE -> ModeScreen(viewModel)
            }
            // 临时开发测试入口（非正式 UI，跳转 DebugActivity；真机验证用）
            TextButton(
                onClick = { context.startActivity(Intent(context, DebugActivity::class.java)) },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Text(
                    "开发测试",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
