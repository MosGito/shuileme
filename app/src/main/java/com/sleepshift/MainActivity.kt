package com.sleepshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.model.OnboardingFlow
import com.sleepshift.shuileme.reminder.ShuilemeReminderScheduler
import com.sleepshift.shuileme.ui.ShuilemeHomeScreen
import com.sleepshift.shuileme.ui.ShuilemeOnboardingScreen
import com.sleepshift.shuileme.ui.ShuilemeViewModel
import com.sleepshift.shuileme.ui.ShuilemeViewModelFactory
import com.sleepshift.shuileme.widget.ShuilemeWidgetRefreshScheduler
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
        // SL-3：桌面组件 15 分钟周期刷新（BOOT 由 RefreshReceiver 重排）
        ShuilemeWidgetRefreshScheduler.schedule(applicationContext)
        // SL-4：睡前提醒 + 熬夜检查点调度
        lifecycleScope.launch { ShuilemeReminderScheduler.scheduleAll(applicationContext) }
        setContent {
            SleepShiftTheme {
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
