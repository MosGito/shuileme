package com.sleepshift.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sleepshift.AppCapabilities
import com.sleepshift.permission.ShizukuManager
import com.sleepshift.permission.ShizukuPermission
import rikka.shizuku.Shizuku

/**
 * 首次启动引导（三步）：
 * 1. 理念介绍（普通语言）
 * 2. 能力检查（系统时间控制权限 / 自动时间设置 / 精确闹钟；含 Shizuku 授权引导）
 * 3. 就绪 + 通知授权 + 开始使用
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(1) }
    var status by remember { mutableStateOf<AppCapabilities.Status?>(null) }
    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Shizuku 授权结果监听（Phase 11-D）：授权/拒绝后立即重查能力状态
    DisposableEffect(Unit) {
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == ShizukuPermission.REQUEST_CODE) {
                    status = AppCapabilities.check(context)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    fun refreshStatus() {
        status = AppCapabilities.check(context)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    LaunchedEffect(step) {
        if (step == 2) refreshStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                1 -> IntroContent()
                2 -> StatusContent(
                    status = status,
                    onGrantShizuku = { ShizukuPermission.requestPermission() },
                    onOpenShizuku = { openShizukuApp(context) },
                )
                3 -> ReadyContent(status, notificationGranted, notifLauncher::launch)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 步骤指示点
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(3) { i ->
                val active = i + 1 == step
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (active) 24.dp else 8.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            CircleShape,
                        )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 底部动作按钮
        Button(
            onClick = {
                when (step) {
                    1 -> step = 2
                    2 -> step = 3
                    3 -> onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (step == 3) "开始使用" else "下一步")
        }
        if (step == 2) {
            OutlinedButton(
                onClick = { refreshStatus() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("重新检查")
            }
        }
    }
}

/** 打开 Shizuku 应用；未安装时打开官方站点 */
private fun openShizukuApp(context: Context) {
    val intent = context.packageManager
        .getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
private fun IntroContent() {
    Text(
        text = "欢迎使用 SleepShift",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "让手机时间稍微快一点，\n帮助你建立睡眠暗示。",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "到了设定时间，手机时间会自动提前，让你自然地意识到\"该睡觉了\"；天亮后恢复原样，不影响白天使用。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatusContent(
    status: AppCapabilities.Status?,
    onGrantShizuku: () -> Unit,
    onOpenShizuku: () -> Unit,
) {
    Text(
        text = "检查手机状态",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    if (status == null) {
        Text("正在检查…", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    } else {
        StatusRow(
            title = "系统时间控制权限",
            ok = status.timezoneControlReady,
            okText = status.timezoneControlDesc,
            failText = "未授权",
        )
        if (!status.timezoneControlReady) {
            ShizukuGuideCard(status, onGrantShizuku, onOpenShizuku)
        }
        StatusRow(
            title = "自动时间设置",
            ok = !status.autoTimeZoneEnabled,
            okText = "已关闭",
            failText = "开启中（应用会自动关闭）",
        )
        StatusRow(
            title = "精确闹钟权限",
            ok = status.exactAlarmGranted,
            okText = "已开启",
            failText = "未开启（请在系统设置中允许）",
        )
    }
}

/**
 * Shizuku 授权引导卡（仅当时区控制未就绪时显示）。
 * 按「未安装 / 未运行 / 未授权」三态给出对应操作。
 */
@Composable
private fun ShizukuGuideCard(
    status: AppCapabilities.Status,
    onGrantShizuku: () -> Unit,
    onOpenShizuku: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text("通过 Shizuku 授权", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        when {
            !status.shizukuInstalled -> {
                Text(
                    text = "未检测到 Shizuku。请先安装（shizuku.rikka.app），然后回来重新检查。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenShizuku, modifier = Modifier.fillMaxWidth()) {
                    Text("前往 Shizuku")
                }
            }
            !status.shizukuRunning -> {
                Text(
                    text = "Shizuku 已安装但服务未运行。请打开 Shizuku 并启动服务（无 root 设备需先用 adb 激活，见下方说明）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenShizuku, modifier = Modifier.fillMaxWidth()) {
                    Text("打开 Shizuku")
                }
            }
            else -> {
                Text(
                    text = "Shizuku 运行中，但尚未授权给 SleepShift。点击下方按钮完成授权。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGrantShizuku, modifier = Modifier.fillMaxWidth()) {
                    Text("授权 Shizuku")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "备选方式：以设备管理员安装本应用（需 adb），同样可获得时间控制权限。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, failText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (ok) "✓" else "✗",
            color = if (ok) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (ok) okText else failText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    status: AppCapabilities.Status?,
    notificationGranted: Boolean,
    requestNotif: (String) -> Unit,
) {
    val ready = status?.allReady == true
    Text(
        text = if (ready) "一切就绪" else "还差一点点",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = if (ready) {
            "你现在可以开始使用 SleepShift 了。\n到\"配置\"页设置开始时间和提前量即可。"
        } else {
            "部分状态未就绪，请先完成上方提示的设置，然后重新检查。"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    if (!notificationGranted) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { requestNotif(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("允许睡眠模式通知")
        }
    }
}
