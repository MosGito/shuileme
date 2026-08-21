package com.sleepshift.shuileme.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.DebugActivity
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.model.MoonGlow
import com.sleepshift.shuileme.model.MoonRipple
import com.sleepshift.shuileme.model.PersonaBubblePhysics
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import com.sleepshift.shuileme.ui.components.NightMoon
import com.sleepshift.shuileme.ui.components.SleepGoalEditor
import com.sleepshift.shuileme.model.ResidentEngine
import com.sleepshift.shuileme.model.ShuilemeTypography
import com.sleepshift.shuileme.model.SleepGestureState
import com.sleepshift.shuileme.model.SleepGestureTrigger
import com.sleepshift.shuileme.reminder.ReminderPersonality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** 长按蓄力完成一圈的时间（毫秒） */
private const val CHARGE_MS = 1100L // SL-9.7：长按 1.1s 完成蓄力

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
    var showCase by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var devOpen by remember { mutableStateOf(false) } // SL-9.7：最小化开发控制台开关
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

    // SL-9.6：人格气泡改为失重漂浮（不依赖重力/加速度计），物理循环见下方 LaunchedEffect

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

    Box(
        Modifier.fillMaxSize(),
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

            // 装饰人格气泡物理状态（SL-9.7：失重漂浮 + 窗框级稳定坐标系拖拽）
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
                    physics = physics.step(dt = 0.016f)
                }
            }

            // 玻璃窗框（半透明 + 柔和描边 + 顶部光泽）；窗框级拖拽（稳定坐标系）
            val density = LocalDensity.current
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(windowMargin.dp)
                    .pointerInput(Unit) {
                        // SL-9.9：命中 = emoji 渲染中心（marginPx + b*density + emojiHalf），半径 90px（≈3x emoji 半径）
                        // 拖拽：delta = currentPointer - pointerDown（帧局部稳定）；bubblePosition = bubbleStart + delta/density
                        val marginPx = with(density) { windowMargin.dp.toPx() }
                        val emojiHalfPx = 30f // 22sp emoji 半宽 ≈ 30px
                        var dragIdx = -1
                        var pointerDown = Offset.Zero
                        var pointerDownOffset = Offset.Zero
                        var lastAmount = Offset.Zero
                        detectDragGestures(
                            onDragStart = { pointer ->
                                val hit = physics.bubbles.indexOfFirst { b ->
                                    val cx = marginPx + b.x * density.density + emojiHalfPx
                                    val cy = marginPx + b.y * density.density + emojiHalfPx
                                    kotlin.math.abs(pointer.x - cx) < 90f &&
                                        kotlin.math.abs(pointer.y - cy) < 90f
                                }
                                if (hit >= 0) {
                                    dragIdx = hit
                                    val bb = physics.bubbles[hit]
                                    val bubbleCenterPx = Offset(
                                        marginPx + bb.x * density.density + emojiHalfPx,
                                        marginPx + bb.y * density.density + emojiHalfPx,
                                    )
                                    pointerDown = pointer
                                    pointerDownOffset = pointer - bubbleCenterPx
                                    lastAmount = Offset.Zero
                                    physics = physics.moveBubble(hit, bb.x, bb.y, drag = true)
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                if (dragIdx >= 0) {
                                    lastAmount = amount
                                    val centerPx = change.position - pointerDownOffset
                                    physics = physics.moveBubble(
                                        dragIdx,
                                        (centerPx.x - marginPx - emojiHalfPx) / density.density,
                                        (centerPx.y - marginPx - emojiHalfPx) / density.density,
                                        drag = true,
                                    )
                                }
                            },
                            onDragEnd = {
                                if (dragIdx >= 0) {
                                    val vx = (lastAmount.x / 0.25f).coerceIn(-160f, 160f)
                                    val vy = (lastAmount.y / 0.25f).coerceIn(-160f, 160f)
                                    val bb = physics.bubbles[dragIdx]
                                    physics = physics.moveBubble(
                                        dragIdx,
                                        bb.x,
                                        bb.y,
                                        vx = vx,
                                        vy = vy,
                                        drag = false,
                                    )
                                    dragIdx = -1
                                }
                            },
                            onDragCancel = { dragIdx = -1 },
                        )
                    },
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

                // 装饰人格气泡（纯渲染；拖拽由窗框级 pointerInput 处理，SL-9.9 命中已扩大并对齐 emoji 中心）
                physics.bubbles.forEachIndexed { _, b ->
                    Text(
                        b.emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .offset(x = b.x.dp, y = b.y.dp),
                    )
                }

                // ── 月亮中心弹出提示（SL-9.3：alpha 0→1 / scale 0.8→1，停留，alpha→0 / scale→0.8）──
            AnimatedVisibility(
                visible = hintVisible,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400)),
                exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.8f, animationSpec = tween(400)),
                modifier = Modifier.align(Alignment.Center).offset(y = -130.dp), // SL-9.7：提示在月亮正上方 ~80dp，不被遮挡
            ) {
                Box(
                    Modifier
                        .background(ShuilemeNight.CardStrong, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (state.isSleeping) "长按我迎接新一天 ☀️" else "长按我进入梦乡 🌙",
                        fontSize = 22.sp,
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

            // SL-9.10：恢复「昨晚月亮观察」卡片（醒后自动生成，点击查看详情；设置入口保持移除）
            val report = detectiveReport
            if (report != null) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(ShuilemeNight.CardStrong, RoundedCornerShape(18.dp))
                        .clickable { showCase = true }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🌙 昨晚月亮观察 · ${report.caseLevel.emoji}", fontSize = 22.sp, color = ShuilemeNight.TextPrimary)
                    Text(
                        report.observation,
                        fontSize = 16.sp,
                        color = ShuilemeNight.TextSecondary,
                        textAlign = TextAlign.Center,
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
                onOpen = { settingsOpen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )

            // ── 左上：最小化开发控制台（SL-9.7：小点按钮，默认低可见度，点击展开/收起）──
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ShuilemeNight.TextSecondary.copy(alpha = if (devOpen) 0.8f else 0.25f))
                            .clickable { devOpen = !devOpen },
                    )
                    AnimatedVisibility(
                        visible = devOpen,
                        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150)),
                    ) {
                        Column(
                            Modifier
                                .padding(top = 8.dp)
                                .background(ShuilemeNight.CardStrong, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            TextButton(onClick = { context.startActivity(Intent(context, DebugActivity::class.java)) }) {
                                Text("调试控制台", color = ShuilemeNight.TextPrimary)
                            }
                        }
                    }
                }
            }
            }
        }

        // SL-9.5：设置面板从上方展开（齿轮下拉打开 → 面板自顶部滑下 + 半透明遮罩）
        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(250)),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(250)),
        ) {
            Box(Modifier.fillMaxSize()) {
                // 遮罩：点击任意空白关闭
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { settingsOpen = false },
                )
                SettingsDrawer(
                    viewModel = viewModel,
                    state = state,
                    context = context,
                    onClose = { settingsOpen = false },
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

    // 蓄力完成：月亮轻微放大
    val moonScale by animateFloatAsState(
        targetValue = if (gesture.state == SleepGestureState.COMPLETED) 1.2f else 1f,
        animationSpec = tween(300),
        label = "moonScale",
    )

    // SL-9.6：复用共享 NightMoon（光晕 + 呼吸波纹 + 月亮本体），与引导页月亮视觉一致
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        NightMoon(
            emoji = moonEmoji(state, nowMs, virtualMs),
            glowAlpha = glowAlpha,
            glowScale = glowScale,
            tapRippleTick = rippleTick,
            moonTextModifier = Modifier
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
    }
}

/** SL-9.6：夜灯式设置开关 —— Canvas 二次贝塞尔柔性绳 + 齿轮跟随手指，禁止字符绳、禁止点击打开。 */
@Composable
private fun NightLightButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val swing = rememberInfiniteTransition(label = "nightLightSwing")
    val angle by swing.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "swingAngle",
    )
    val gearBaseY = 58.dp
    val boxWidth = 46.dp
    val boxHeight = 90.dp
    val density = LocalDensity.current

    // SL-9.7：齿轮即 draggable object（parent 稳定坐标系）。
    // 按下：记录 startGearPosition + startPointer；移动：gearPosition = startGear + delta；
    // 绳按 gearPosition 动态绘制（Canvas bezier）；松手超阈值开设置，否则 spring 回弹。
    var dragging by remember { mutableStateOf(false) }
    var gearPos by remember { mutableStateOf(Offset.Zero) }
    var startGear by remember { mutableStateOf(Offset.Zero) }
    var startPointer by remember { mutableStateOf(Offset.Zero) }
    // 松手回弹动画（非拖拽时目标为 0）
    val animX by animateFloatAsState(
        targetValue = if (dragging) gearPos.x else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "gearAnimX",
    )
    val animY by animateFloatAsState(
        targetValue = if (dragging) gearPos.y else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "gearAnimY",
    )

    Box(
        modifier = modifier
            .width(boxWidth)
            .height(boxHeight)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pointer ->
                        // 命中齿轮：齿轮中心在 parent 局部坐标
                        val gearCenterX = with(density) { boxWidth.toPx() } / 2f
                        val gearCenterY = with(density) { gearBaseY.toPx() } + gearPos.y + 31f // 齿轮 24sp 中心
                        if (kotlin.math.abs(pointer.x - gearCenterX) < 80f &&
                            kotlin.math.abs(pointer.y - gearCenterY) < 80f
                        ) {
                            dragging = true
                            startGear = gearPos
                            startPointer = pointer
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (dragging) {
                            val delta = change.position - startPointer // parent 局部稳定 delta
                            gearPos = startGear + delta
                        }
                    },
                    onDragEnd = {
                        if (dragging) {
                            val exceeded = gearPos.y > 60f // 向下拖超阈值 → 打开设置
                            dragging = false
                            gearPos = Offset.Zero // 弹簧回弹
                            if (exceeded) onOpen()
                        }
                    },
                    onDragCancel = { dragging = false; gearPos = Offset.Zero },
                )
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        // 柔性绳：Canvas 二次贝塞尔，上端接屏幕外，下端接齿轮（随 animX/animY 动态）
        Canvas(Modifier.fillMaxSize()) {
            val startX = size.width / 2f
            val endX = size.width / 2f + animX
            val startY = -24f // 上端在屏幕外（隐藏连接点）
            val endY = gearBaseY.toPx() + animY
            val bendX = (animY * 0.20f + animX * 0.5f).coerceIn(-36f, 36f)
            val ctrl = Offset(startX + bendX, (startY + endY) / 2f)
            val path = Path().apply {
                moveTo(startX, startY)
                quadraticTo(ctrl.x, ctrl.y, endX, endY)
            }
            drawPath(
                path = path,
                color = ShuilemeNight.TextSecondary.copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        // 齿轮（跟随 animX/animY + 摆动）
        Text(
            "⚙️",
            fontSize = 24.sp,
            modifier = Modifier
                .offset(x = animX.dp, y = gearBaseY + animY.dp)
                .graphicsLayer { rotationZ = angle },
        )
    }
}

/** 设置面板（SL-8 + SL-9.5：从上方展开的顶部面板；SL-9.10 加「睡眠目标」二级菜单） */
@Composable
private fun SettingsDrawer(
    viewModel: ShuilemeViewModel,
    state: ShuilemeState,
    context: android.content.Context,
    onClose: () -> Unit,
) {
    var showGoalEditor by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                ShuilemeNight.Sky,
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (showGoalEditor) "睡眠目标" else "设置", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
            Row {
                if (showGoalEditor) {
                    TextButton(onClick = { showGoalEditor = false }) { Text("← 返回", color = ShuilemeNight.TextSecondary) }
                }
                TextButton(onClick = onClose) { Text("✕", color = ShuilemeNight.TextSecondary) }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (showGoalEditor) {
            // SL-9.10：共享 SleepGoalEditor（与初始页同一组件）
            SleepGoalEditor(
                targetSleepTimeMin = state.targetSleepTimeMin,
                targetWakeTimeMin = state.targetWakeTimeMin,
                currentSleepTimeMin = state.currentSleepTimeMin,
                currentWakeTimeMin = state.currentWakeTimeMin,
                onTargetSleepChange = { viewModel.setSleepGoal(it, state.targetWakeTimeMin, state.currentSleepTimeMin, state.currentWakeTimeMin) },
                onTargetWakeChange = { viewModel.setSleepGoal(state.targetSleepTimeMin, it, state.currentSleepTimeMin, state.currentWakeTimeMin) },
                onCurrentSleepChange = { viewModel.setSleepGoal(state.targetSleepTimeMin, state.targetWakeTimeMin, it, state.currentWakeTimeMin) },
                onCurrentWakeChange = { viewModel.setSleepGoal(state.targetSleepTimeMin, state.targetWakeTimeMin, state.currentSleepTimeMin, it) },
                showInitialInclination = false,
            )
            return
        }

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
        // SL-9.10：睡眠目标二级菜单入口
        TextButton(
            onClick = { showGoalEditor = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("睡眠目标", fontSize = 22.sp, color = ShuilemeNight.TextPrimary)
        }

        Spacer(Modifier.height(12.dp))
        Text("提醒人格", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ReminderPersonality.entries.forEach { p ->
                FilterChip(
                    selected = state.reminderProfile.personality == p,
                    onClick = { viewModel.setReminderPersonality(p) },
                    label = { Text("${p.displayEmoji()} ${p.displayName}") },
                )
            }
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
