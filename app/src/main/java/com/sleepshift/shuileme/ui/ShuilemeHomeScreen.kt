package com.sleepshift.shuileme.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.DebugActivity
import com.sleepshift.PersonalityCardActivity
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.model.MoonGlow
import com.sleepshift.shuileme.model.PersonaBubblePhysics
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import com.sleepshift.shuileme.model.SleepGestureState
import com.sleepshift.shuileme.model.SleepGestureTrigger
import com.sleepshift.shuileme.model.SleepResultReason
import com.sleepshift.shuileme.reminder.ReminderPersonality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 「睡了么」主界面（SL-8 沉浸式重构）。
 *
 * 结构：人格头部（顶）→ 月亮宠物（中央，光效）→ 虚拟时间（月亮下）→ 装饰人格气泡 → 设置抽屉（齿轮）。
 * 首页只保留：睡眠人格 / 月亮 / 虚拟时间 / 人格气泡 / 设置入口 五项。
 * 交互：「我要睡了/我醒啦」= 长按月亮 → 出现提示 → 点击确认（防误触）。
 */
@Composable
fun ShuilemeHomeScreen(viewModel: ShuilemeViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val personality by viewModel.personalityState.collectAsState()
    val talk by viewModel.talkText.collectAsState()
    val detectiveReport by viewModel.detectiveReport.collectAsState()
    val scope = rememberCoroutineScope()
    var showCase by remember { mutableStateOf(false) }
    var gesture by remember { mutableStateOf(SleepGestureTrigger()) }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(1000) } }
    LaunchedEffect(Unit) { viewModel.refreshTalk() }

    // SL-7 案件报告页
    if (showCase && detectiveReport != null) {
        SleepCaseReportScreen(report = detectiveReport!!, onBack = { showCase = false })
        return
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawer(
                viewModel = viewModel,
                state = state,
                context = context,
                onOpenDetective = {
                    showCase = true
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF14142B)),
        ) {
            val bw = maxWidth.value
            val bh = maxHeight.value

            // ── 装饰人格气泡（SL-8，物理下落 + 弹跳）──
            val bubbleEmojis = remember(personality.residents) {
                personality.residents.flatMap { r -> List(r.amount) { r.emoji } }
            }
            var physics by remember { mutableStateOf(PersonaBubblePhysics()) }
            LaunchedEffect(bubbleEmojis, bw, bh) {
                physics = PersonaBubblePhysics(
                    width = bw,
                    height = bh,
                    bubbles = PersonaBubblePhysics.scatter(bubbleEmojis, bw, bh, state.sleepCount),
                )
            }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(16)
                    physics = physics.step(gravityY = 500f, dt = 0.016f)
                }
            }
            physics.bubbles.forEach { b ->
                Text(
                    b.emoji,
                    fontSize = 20.sp,
                    modifier = Modifier.offset(x = b.x.dp, y = b.y.dp),
                )
            }

            // ── 中央：月亮宠物 + 光效 + 长按手势 ──
            MoonWithGlow(
                state = state,
                nowMs = nowMs,
                gesture = gesture,
                onLongPress = {
                    gesture = SleepGestureTrigger()
                        .onDown(System.currentTimeMillis())
                        .onTick(System.currentTimeMillis() + 600L, 500L)
                },
                onTap = {
                    if (gesture.state == SleepGestureState.READY) {
                        val now = System.currentTimeMillis()
                        if (state.isSleeping) viewModel.wakeUp(now) else viewModel.startSleep(now)
                        gesture = SleepGestureTrigger()
                    }
                },
            )

            // ── 月亮下方：虚拟时间 + 心理暗示 ──
            val engine = remember(state.currentOffsetMin, state.mode, state.gradualStepMin, state.fluctuationRangeMin) {
                VirtualClockEngine(state.toVirtualClockConfig())
            }
            val virtualMs = engine.virtualTimeMs(nowMs)
            val fmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
            val zone = remember { ZoneId.systemDefault() }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    fmt.format(Instant.ofEpochMilli(virtualMs).atZone(zone)),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    statusCopy(state, nowMs, virtualMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                talk?.let { t ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── 长按确认提示 ──
            if (gesture.state == SleepGestureState.READY) {
                Text(
                    if (state.isSleeping) "点击确认 ☀️ 我醒啦" else "点击确认 🌙 我要睡了",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 90.dp),
                )
            }

            // ── 顶部：人格头部（MBTI 式）──
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "你的睡眠人格类型是",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val p = personality.primaryType
                if (p != null) {
                    Text(
                        "${PersonalityCardGenerator.personalityPairEmoji(p)} ${p.displayName}型",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(context, PersonalityCardActivity::class.java))
                        },
                    )
                } else {
                    Text("🌙 月亮正在认识你", style = MaterialTheme.typography.titleMedium)
                }
            }

            // ── 右上：设置齿轮 ──
            IconButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Text("⚙️", fontSize = 20.sp)
            }
        }
    }
}

/** 月亮宠物 + 月光（光效随 MoonLife）+ 长按手势 */
@Composable
private fun MoonWithGlow(
    state: ShuilemeState,
    nowMs: Long,
    gesture: SleepGestureTrigger,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
) {
    val intensity = MoonGlow.glowIntensity(state.moonLife)
    val glowAlpha by animateFloatAsState(MoonGlow.glowAlpha(intensity), tween(600), label = "glowAlpha")
    val glowScale by animateFloatAsState(MoonGlow.glowScale(intensity), tween(600), label = "glowScale")
    val engine = remember(state.currentOffsetMin, state.mode) { VirtualClockEngine(state.toVirtualClockConfig()) }
    val virtualMs = engine.virtualTimeMs(nowMs)
    val moonScale by animateFloatAsState(
        targetValue = if (gesture.state == SleepGestureState.READY) 1.12f else 1f,
        animationSpec = tween(300),
        label = "moonScale",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 淡黄月光（径向渐变，非图片）
        Box(
            Modifier
                .size(190.dp * glowScale)
                .alpha(glowAlpha)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFE082), Color(0x00FFE082)),
                            center = center,
                            radius = size.minDimension / 2f,
                        ),
                        radius = size.minDimension / 2f,
                    )
                },
        )
        // 月亮本体（长按 → READY → 点击确认）
        Text(
            moonEmoji(state, nowMs, virtualMs),
            fontSize = 96.sp,
            modifier = Modifier
                .scale(moonScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                        onTap = { onTap() },
                    )
                },
        )
    }
}

/** 设置抽屉（SL-8：收纳偏移/目标/人格/入口） */
@Composable
private fun SettingsDrawer(
    viewModel: ShuilemeViewModel,
    state: ShuilemeState,
    context: android.content.Context,
    onOpenDetective: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("虚拟时间偏移", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = state.currentOffsetMin.toFloat(),
                onValueChange = { viewModel.updateOffset((it / 15f).roundToInt() * 15) },
                valueRange = 0f..240f,
                steps = 15,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("+${state.currentOffsetMin} 分钟", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))
            Text("目标入睡时间", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(22 * 60, 23 * 60, 0).forEach { h ->
                    TextButton(onClick = { viewModel.setTargetSleepTime(h) }) {
                        Text(if (h == 0) "00:00" else "%02d:%02d".format(h / 60, h % 60))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("提醒人格", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReminderPersonality.entries.forEach { p ->
                    FilterChip(
                        selected = state.reminderProfile.personality == p,
                        onClick = { viewModel.setReminderPersonality(p) },
                        label = { Text("${p.emoji} ${p.displayName}") },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onOpenDetective) { Text("🌙 昨晚月亮观察") }
            TextButton(onClick = { context.startActivity(Intent(context, PersonalityCardActivity::class.java)) }) {
                Text("🫧 我的睡眠人格卡片")
            }
            TextButton(onClick = { context.startActivity(Intent(context, DebugActivity::class.java)) }) {
                Text("旧版控制台")
            }
        }
    }
}

/** 月亮 emoji：睡眠态=进度相；清醒态=月亮生命阶段（SL-5，满月奖励=🌕） */
private fun moonEmoji(state: ShuilemeState, nowMs: Long, virtualMs: Long): String =
    if (state.isSleeping) {
        val start = state.sleepStartAtMs
        if (start == null) "🌙"
        else sleepProgressMoon(((nowMs - start) / 60_000.0) / VirtualClockEngine.DEFAULT_TARGET_SLEEP_MIN)
    } else {
        state.moonLife.stage.emoji
    }

/** 睡眠进度月亮（8 相盈亏，过半=满月 🌕） */
private val MOON_WAX = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")

private fun sleepProgressMoon(progress: Double): String {
    val idx = (progress.coerceIn(0.0, 1.0) * MOON_WAX.size).toInt().coerceIn(0, MOON_WAX.size - 1)
    return MOON_WAX[idx]
}

/** 状态文案 */
private fun statusCopy(state: ShuilemeState, nowMs: Long, virtualMs: Long): String {
    if (state.isSleeping) {
        val start = state.sleepStartAtMs ?: return "嘘…月亮在数羊"
        val virtualElapsedMin = (nowMs - start) / 60_000L + state.currentOffsetMin
        return "嘘，你已经睡了 ${formatElapsed(virtualElapsedMin)} 啦"
    }
    return when {
        state.moonProgress.fullMoonRewardPending -> "🌝 月亮很满意！连续 5 晚达成满月"
        isVirtualVeryLate(virtualMs) -> "🌚 这么晚还不睡？今晚让月亮长大一点"
        else -> "🌙 今晚让月亮长大一点"
    }
}

/** 虚拟时间是否已过午夜（很晚） */
private fun isVirtualVeryLate(virtualMs: Long): Boolean {
    val hour = Instant.ofEpochMilli(virtualMs).atZone(ZoneId.systemDefault()).hour
    return hour < 6
}

/** 时长文案 */
private fun formatElapsed(totalMin: Long): String {
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h <= 0L -> "$m 分钟"
        m == 0L -> "$h 小时"
        else -> "$h 小时 $m 分钟"
    }
}
