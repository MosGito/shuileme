package com.sleepshift.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sleepshift.ui.config.ConfigScreen
import com.sleepshift.ui.home.HomeScreen
import com.sleepshift.ui.mode.ModeScreen

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("今日", Icons.Filled.Home),
    CONFIG("配置", Icons.Filled.Settings),
    MODE("模式", Icons.Filled.DateRange),
}

/** 应用壳：底部导航 + 三页切换（阶段 2 用状态式导航，后续如需深链/回退栈再加 navigation-compose） */
@Composable
fun SleepShiftApp(viewModel: SettingsViewModel) {
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
        }
    }
}
