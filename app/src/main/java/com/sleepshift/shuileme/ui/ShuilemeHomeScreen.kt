package com.sleepshift.shuileme.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.sleepshift.DebugActivity
import com.sleepshift.PersonalityCardActivity
import com.sleepshift.shuileme.GravitySensor
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.model.MoonGlow
import com.sleepshift.shuileme.model.MoonRipple
import com.sleepshift.shuileme.model.PersonaBubblePhysics
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import com.sleepshift.shuileme.model.ResidentEngine
import com.sleepshift.shuileme.model.ShuilemeTypography
import com.sleepshift.shuileme.model.SleepGestureState
import com.sleepshift.shuileme.model.SleepGestureTrigger
import com.sleepshift.shuileme.model.resolveGravity
import com.sleepshift.shuileme.reminder.ReminderPersonality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** 长按蓄力完成一圈的时间（毫秒） */
private const val CHARGE_MS = 1500L

/**
 * 「睡了么」主界面（SL-9 体验修正）。
 * - 星空背景 + 呼吸闪烁；
 * - 月亮中央光效（点击波纹 + 长按蓄力环完成触发）；
 * - 睡眠/清醒状态提示 + 虚拟时间（睡眠窗口内偏移）；
 * - 装饰人格气泡（四壁物理边界）；
 * - 齿轮设置抽屉。
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
    var hintVisible by remember { mutableStateOf(false) }
    var hintTick by remember { mutableIntStateOf(0) }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(1000) } }
    LaunchedEffect(Unit) { viewModel.refreshTalk() }
    LaunchedEffect(hintTick) {
        if (hintTick > 0) {
            hintVisible = true
            delay(3000)
            hintVisible = false
        }
    }

    // SL-9.2.2：真实重力（加速度计；无传感器/关闭 → 降级模拟）
    val gravitySensor = remember { GravitySensor(context) }
    val sensorTilt by gravitySensor.tilt.collectAsState()
    var gravityPromptShown by remember { mutableStateOf(false) }
    DisposableEffect(state.gravityEnabled) {
        if (state.gravityEnabled) {
            gravitySensor.start()
            if (gravitySensor.hasSensor && !gravityPromptShown) {
                gravityPromptShown = true
                Toast.makeText(context, "让月亮感受你的方向 🌙", Toast.LENGTH_SHORT).show()
            }
        } else {
            gravitySensor.stop()
        }
        onDispose { gravitySensor.stop() }
    }
    val gravity = resolveGravity(if (state.gravityEnabled) sensorTilt else null)

    // SL-7 案件报告页
    if (showCase && detectiveReport != null) {
        SleepCaseReportScreen(report = detectiveReport!!, onBack = { showCase = false })
        return
    }

    // 长按蓄力环：CHARGING → 进度 0→1 → 完成触发
    LaunchedEffect(gesture.state) {
        if (gesture.state == SleepGestureState.CHARGING) {
            val start = System.currentTimeMillis()
            while (gesture.state == SleepGestureState.CHARGING) {
                val progress = ((System.currentTimeMillis() - start) / CHARGE_MS.toFloat()).coerceAtMost(1f)
                gesture = gesture.onCharge(progress)
                if (progress >= 1f) {
                    gesture = gesture.onChargeComplete()
                    val now = System.currentTimeMillis()
                    if (state.isSleeping) viewModel.wakeUp(now) else viewModel.startSleep(now)
                    break
                }
                delay(16)
            }
        }
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
                .background(ShuilemeNight.Sky)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val bw = maxWidth.value
            val bh = maxHeight.value

            // 星空背景
            NightStarField(Modifier.fillMaxSize())

            // ── 夜晚窗框区域（SL-9.2.2：气泡只在窗框内运动）──
            val windowMargin = 16f
            val windowW = (bw - windowMargin * 2).coerceAtLeast(120f)
            val windowH = (bh - windowMargin * 2).coerceAtLeast(240f)

            // 玻璃窗框（半透明 + 柔和描边 + 顶部光泽）
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(windowMargin.dp),
            ) {
                // SL-9.3：极细半透明窗框线（低透明度，不改变视觉面积）
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(0.5.dp, ShuilemeNight.Accent.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                        .background(ShuilemeNight.Card),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .width(70.dp)
                            .height(2.dp)
                            .background(ShuilemeNight.Accent.copy(alpha = 0.18f)),
                    )
                }

                // 装饰人格气泡（SL-9.2.1 人格符号；可拖动，松手惯性漂移；不出窗框）
                val bubbleSource = personality.primaryType ?: state.onboarding.initialPersonality
                val bubbleEmojis = remember(bubbleSource) {
                    if (bubbleSource == null) emptyList()
                    else ResidentEngine.bubbleEmojis(bubbleSource, count = 8, seed = state.sleepCount)
                }
                var physics by remember { mutableStateOf(PersonaBubblePhysics()) }
                LaunchedEffect(bubbleEmojis, windowW, windowH) {
                    physics = PersonaBubblePhysics(
                        width = windowW,
                        height = windowH,
                        bubbles = PersonaBubblePhysics.scatter(bubbleEmojis, windowW, windowH, state.sleepCount),
                    )
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(16)
                        physics = physics.step(gravityX = gravity.first, gravityY = gravity.second, dt = 0.016f)
                    }
                }
                physics.bubbles.forEachIndexed { idx, b ->
                    Text(
                        b.emoji,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .offset(x = (windowMargin + b.x).dp, y = (windowMargin + b.y).dp)
                            .pointerInput(idx) {
                                // SL-9.3：拖动用 pointer offset（position = pointer - 初始偏移）
                                var offsetX = 0f
                                var offsetY = 0f
                                var lastAmount = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val b = physics.bubbles[idx]
                                        offsetX = start.x - b.x
                                        offsetY = start.y - b.y
                                        physics = physics.moveBubble(idx, b.x, b.y, drag = true)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        lastAmount = amount
                                        physics = physics.moveBubble(
                                            idx,
                                            change.position.x - offsetX,
                                            change.position.y - offsetY,
                                            drag = true,
                                        )
                                    },
                                    onDragEnd = {
                                        val vx = (lastAmount.x / 0.12f).coerceIn(-260f, 260f)
                                        val vy = (lastAmount.y / 0.12f).coerceIn(-260f, 260f)
                                        physics = physics.moveBubble(
                                            idx,
                                            physics.bubbles[idx].x,
                                            physics.bubbles[idx].y,
                                            vx = vx,
                                            vy = vy,
                                            drag = false,
                                        )
                                    },
                                )
                            },
                    )
                }

                // ── 月亮中心弹出提示（SL-9.3：alpha 0→1 / scale 0.8→1，停留，alpha→0 / scale→0.8）──
            AnimatedVisibility(
                visible = hintVisible,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400)),
                exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.8f, animationSpec = tween(400)),
                modifier = Modifier.align(Alignment.Center).offset(y = -70.dp),
            ) {
                Box(
                    Modifier
                        .background(ShuilemeNight.CardStrong, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (state.isSleeping) "长按我迎接新一天 ☀️" else "长按我进入梦乡 🌙",
                        fontSize = 18.sp,
                        color = ShuilemeNight.TextPrimary,
                    )
                }
            }

            // ── 中央：月亮（光效 + 波纹 + 蓄力环 + 手势）──
            MoonWithGlow(
                state = state,
                nowMs = nowMs,
                gesture = gesture,
                rippleTick = hintTick,
                onTap = {
                    hintTick++
                    hintVisible = true
                },
                onLongPress = { gesture = SleepGestureTrigger().onPress().onLongPress() },
                onPressCancel = { gesture = gesture.onCancel() },
            )

            // ── 月亮下方：状态 + 虚拟时间 + 提示 ──
            val engine = remember(state.currentOffsetMin, state.mode, state.gradualStepMin, state.fluctuationRangeMin) {
                VirtualClockEngine(state.toVirtualClockConfig())
            }
            val zone = remember { ZoneId.systemDefault() }
            val virtualMs = engine.virtualTimeMs(
                realTimeMs = nowMs,
                sleepStartMin = state.targetSleepTimeMin,
                wakeMin = state.targetWakeTimeMin,
                zone = zone,
            )
            val inSleepWindow = engine.isInSleepWindow(nowMs, state.targetSleepTimeMin, state.targetWakeTimeMin, zone)
            val fmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 175.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (inSleepWindow) "现在是睡眠时间" else "现在是清醒时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShuilemeNight.TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    fmt.format(Instant.ofEpochMilli(virtualMs).atZone(zone)),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShuilemeNight.Accent,
                )
                Text(
                    statusCopy(state, nowMs, virtualMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShuilemeNight.TextSecondary,
                )
                talk?.let { t ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t,
                        style = MaterialTheme.typography.bodySmall,
                        color = ShuilemeNight.Accent,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // SL-9.3：昨晚月亮观察入口（点击进入报告）
            val report = detectiveReport
            if (report != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(ShuilemeNight.CardStrong, RoundedCornerShape(18.dp))
                        .clickable { showCase = true }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(
                        "🌙 昨晚月亮观察 · ${report.caseLevel.emoji}",
                        fontSize = 16.sp,
                        color = ShuilemeNight.TextPrimary,
                    )
                }
            }

            // ── 顶部：人格头部（MBTI 式）──
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // SL-9.3：MBTI 风格两行标题（标题 30sp / 人格名 40sp 视觉焦点；禁止 emoji）
                Text(
                    "你的睡眠人格是",
                    fontSize = ShuilemeTypography.TITLE_SP.sp,
                    color = ShuilemeNight.TextSecondary,
                )
                val p = personality.primaryType
                if (p != null) {
                    Text(
                        p.displayName,
                        fontSize = ShuilemeTypography.PERSONA_SP.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShuilemeNight.TextPrimary,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(context, PersonalityCardActivity::class.java))
                        },
                    )
                } else {
                    val initial = state.onboarding.initialPersonality
                    Text(
                        initial?.displayName ?: "🌙 月亮正在认识你",
                        fontSize = ShuilemeTypography.PERSONA_SP.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShuilemeNight.TextPrimary,
                    )
                }
            }

            // ── 右上：夜灯式设置开关（挂绳齿轮 → 向下拖动打开设置）──
            NightLightButton(
                onOpen = { scope.launch { drawerState.open() } },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
            }
        }
    }
}

/** 月亮宠物：光效 + 点击波纹 + 长按蓄力环 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoonWithGlow(
    state: ShuilemeState,
    nowMs: Long,
    gesture: SleepGestureTrigger,
    rippleTick: Int,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onPressCancel: () -> Unit,
) {
    val intensity = MoonGlow.glowIntensity(state.moonLife)
    val glowAlpha by animateFloatAsState(MoonGlow.glowAlpha(intensity), tween(600), label = "glowAlpha")
    val glowScale by animateFloatAsState(MoonGlow.glowScale(intensity), tween(600), label = "glowScale")
    val engine = remember(state.currentOffsetMin, state.mode) { VirtualClockEngine(state.toVirtualClockConfig()) }
    val virtualMs = engine.virtualTimeMs(nowMs)
    val chargeProgress = gesture.chargeProgress

    // 点击波纹：一次扩散 + 淡出
    val ripple by animateFloatAsState(
        targetValue = if (rippleTick > 0) 1f else 0f,
        animationSpec = tween(900),
        label = "ripple",
    )
    // 蓄力完成：月亮轻微放大
    val moonScale by animateFloatAsState(
        targetValue = if (gesture.state == SleepGestureState.COMPLETED) 1.2f else 1f,
        animationSpec = tween(300),
        label = "moonScale",
    )
    // SL-9.2.2：常驻呼吸波纹（低透明度、缓慢扩散，像水面/夜灯光晕）
    val breathTransition = rememberInfiniteTransition(label = "moonBreath")
    val breathPhase by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "breathPhase",
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 常驻呼吸波纹
        Canvas(Modifier.size(180.dp)) {
            drawCircle(
                color = ShuilemeNight.Accent.copy(alpha = MoonRipple.alpha(breathPhase)),
                radius = size.minDimension / 2f * (0.4f + MoonRipple.radius(breathPhase) * 0.6f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // 点击波纹
        if (ripple > 0.01f) {
            Canvas(Modifier.size(160.dp)) {
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = (1f - ripple) * 0.4f),
                    radius = size.minDimension / 2f * (0.5f + ripple),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        // 淡黄月光
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
        // 长按蓄力环
        val chargeColor = ShuilemeNight.Accent
        if (gesture.state == SleepGestureState.CHARGING || chargeProgress > 0f) {
            Canvas(Modifier.size(150.dp)) {
                drawArc(
                    color = chargeColor,
                    startAngle = -90f,
                    sweepAngle = chargeProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        // 月亮本体（点击提示 / 长按蓄力）
        Text(
            moonEmoji(state, nowMs, virtualMs),
            fontSize = 96.sp,
            modifier = Modifier
                .scale(moonScale)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val longPressed = awaitLongPressOrCancellation(down.id)
                        if (longPressed != null) {
                            // 长按确认 → 开始蓄力；蓄力动画由父级 LaunchedEffect 驱动
                            onLongPress()
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                            }
                            onPressCancel()
                        } else {
                            // 长按前抬起 = 短按 → 提示
                            onTap()
                        }
                    }
                },
        )
    }
}

/** SL-9.3：夜灯式设置开关（挂绳 + 月亮摆动；向下拖动超过阈值 → 打开设置） */
@Composable
private fun NightLightButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val swing = rememberInfiniteTransition(label = "nightLightSwing")
    val angle by swing.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "swingAngle",
    )
    var dragY by remember { mutableStateOf(0f) }
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragY += amount.y
                    },
                    onDragEnd = {
                        if (dragY > 60f) onOpen() // 向下拖动超过阈值 → 打开设置
                    },
                )
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(22.dp)
                .background(ShuilemeNight.TextSecondary.copy(alpha = 0.6f)),
        )
        Text(
            "⚙️",
            fontSize = 22.sp,
            modifier = Modifier.graphicsLayer { rotationZ = angle },
        )
    }
}

/** 设置抽屉（SL-8） */
@Composable
private fun SettingsDrawer(
    viewModel: ShuilemeViewModel,
    state: ShuilemeState,
    context: android.content.Context,
    onOpenDetective: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = ShuilemeNight.Sky,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
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
            TextButton(onClick = onOpenDetective, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("🌙 昨晚月亮观察")
            }
            TextButton(
                onClick = { context.startActivity(Intent(context, PersonalityCardActivity::class.java)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("🫧 我的睡眠人格卡片") }
            TextButton(
                onClick = { context.startActivity(Intent(context, DebugActivity::class.java)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("旧版控制台") }
        }
    }
}

/** 月亮 emoji：睡眠态=进度相；清醒态=月亮生命阶段 */
private fun moonEmoji(state: ShuilemeState, nowMs: Long, virtualMs: Long): String =
    if (state.isSleeping) {
        val start = state.sleepStartAtMs
        if (start == null) "🌙"
        else sleepProgressMoon(((nowMs - start) / 60_000.0) / VirtualClockEngine.DEFAULT_TARGET_SLEEP_MIN)
    } else {
        state.moonLife.stage.emoji
    }

private val MOON_WAX = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")

private fun sleepProgressMoon(progress: Double): String {
    val idx = (progress.coerceIn(0.0, 1.0) * MOON_WAX.size).toInt().coerceIn(0, MOON_WAX.size - 1)
    return MOON_WAX[idx]
}

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

private fun isVirtualVeryLate(virtualMs: Long): Boolean {
    val hour = Instant.ofEpochMilli(virtualMs).atZone(ZoneId.systemDefault()).hour
    return hour < 6
}

private fun formatElapsed(totalMin: Long): String {
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h <= 0L -> "$m 分钟"
        m == 0L -> "$h 小时"
        else -> "$h 小时 $m 分钟"
    }
}
