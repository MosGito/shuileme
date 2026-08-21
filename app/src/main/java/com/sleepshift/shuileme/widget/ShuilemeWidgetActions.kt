package com.sleepshift.shuileme.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.engine.VirtualClockEngine

/**
 * 桌面组件一键动作（SL-3）：
 * - [StartSleepAction]：一键「我要睡了」→ 直接 startSleep（无需确认页），Toast 反馈 + 刷新组件
 * - [WakeUpAction]：一键「我醒啦」→ 直接 wakeUp，Toast 反馈 + 刷新组件
 * 不依赖 Shizuku/Root；仅写本地 DataStore。
 */
class StartSleepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = ShuilemeRepository(context)
        val current = repo.current()
        if (!current.isSleeping) {
            val offset = VirtualClockEngine(current.toVirtualClockConfig()).effectiveOffsetMin(dayIndex = 0)
            repo.startSleep(System.currentTimeMillis(), offset)
            Toast.makeText(context, ShuilemeWidgetDisplay.sleepConfirmationText(), Toast.LENGTH_SHORT).show()
            ShuilemeWidgets.refreshAll(context)
        }
    }
}

class WakeUpAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = ShuilemeRepository(context)
        if (repo.current().isSleeping) {
            repo.wakeUp(System.currentTimeMillis())
            Toast.makeText(context, ShuilemeWidgetDisplay.wakeConfirmationText(), Toast.LENGTH_SHORT).show()
            ShuilemeWidgets.refreshAll(context)
        }
    }
}
