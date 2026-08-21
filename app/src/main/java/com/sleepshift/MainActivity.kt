package com.sleepshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.model.OnboardingFlow
import com.sleepshift.shuileme.reminder.ShuilemeReminderScheduler
import com.sleepshift.shuileme.ui.ShuilemeHomeScreen
import com.sleepshift.shuileme.ui.ShuilemeOnboardingScreen
import com.sleepshift.shuileme.ui.ShuilemeViewModel
import com.sleepshift.shuileme.ui.ShuilemeViewModelFactory
import com.sleepshift.shuileme.widget.ShuilemeWidgetRefreshScheduler
import com.sleepshift.ui.WelcomeDialog
import com.sleepshift.ui.theme.SleepShiftTheme
import kotlinx.coroutines.launch

/**
 * 应用入口。
 *
 * SL-2：主界面为「睡了么」首页 MVP。
 * SL-3：启动时调度桌面组件 15 分钟周期刷新。
 * SleepShift Legacy 保留于 DebugActivity（「旧版控制台」入口）。
 */
class MainActivity : ComponentActivity() {

    private val shuilemeViewModel: ShuilemeViewModel by viewModels {
        ShuilemeViewModelFactory(ShuilemeRepository(application), application)
    }

    override fun onResume() {
        super.onResume()
        // SL-7：睡眠窗口内打开 App → 记一条侦探线索（弱信号）
        shuilemeViewModel.recordNightActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // SL-9.4：深色系统栏（透明夜空 + 浅色图标），消除白底状态/导航栏
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        // SL-3：桌面组件 15 分钟周期刷新（BOOT 由 RefreshReceiver 重排）
        ShuilemeWidgetRefreshScheduler.schedule(applicationContext)
        // SL-4：睡前提醒 + 熬夜检查点调度
        lifecycleScope.launch { ShuilemeReminderScheduler.scheduleAll(applicationContext) }
        setContent {
            SleepShiftTheme {
                // SL-9.10 内测包装：首次启动欢迎弹窗作为 onboarding 的前置介绍层。
                // 未点「开始探索」前仅渲染 WelcomeDialog（全屏），完成后才进入原导航流程，
                // 避免与 onboarding 同时出现（此前为全局 overlay）。
                var showWelcome by remember {
                    mutableStateOf(
                        getSharedPreferences("welcome", MODE_PRIVATE)
                            .getBoolean("hasShownWelcomeDialog", false)
                            .not()
                    )
                }
                if (showWelcome) {
                    WelcomeDialog(
                        onDismiss = {
                            getSharedPreferences("welcome", MODE_PRIVATE)
                                .edit().putBoolean("hasShownWelcomeDialog", true).apply()
                            showWelcome = false
                        }
                    )
                } else {
                    val onboarding by shuilemeViewModel.onboarding.collectAsState()
                    if (OnboardingFlow.shouldShowOnboarding(onboarding)) {
                        ShuilemeOnboardingScreen(viewModel = shuilemeViewModel)
                    } else {
                        ShuilemeHomeScreen(viewModel = shuilemeViewModel)
                    }
                }
            }
        }
    }
}
