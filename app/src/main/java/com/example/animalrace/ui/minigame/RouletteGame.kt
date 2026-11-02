package com.example.animalrace.ui.minigame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.MiniGameSession
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.LaneSticker
import com.example.animalrace.ui.sound.LocalSound
import com.example.animalrace.ui.sound.Sfx
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

private val SliceColors = listOf(Color(0xFFFFC93C), Color(0xFF7FD9BE), Color(0xFF8CC4FF), Color(0xFFCFA8FF), Color(0xFFFF9FBD), Color(0xFFFFB27A))

private enum class Spin { READY, CHARGING, SPINNING, STOPPED }

/** 힘 게이지: 누르고 있는 동안 0 → 1 → 0을 [GAUGE_PERIOD]초마다 오간다 */
private const val GAUGE_PERIOD = 1.2f

internal fun gaugeAt(seconds: Float): Float {
    val t = (seconds / (GAUGE_PERIOD / 2)) % 2f
    return 1f - abs(t - 1f)
}

/** 힘에 따라 바퀴 수·도는 시간만 달라진다. 멈출 칸은 [MiniGameSession.bomb]으로 이미 정해져 있다 */
internal fun spinTurns(power: Float): Int = 2 + (power * 6).toInt()
internal fun spinMillis(power: Float): Int = (2600 + power * 3400).toInt()

/** 멈추기 직전 슬로우 모션: 마지막 [slowDegrees]도를 [SLOW_MILLIS] 동안 아주 천천히 (5.2) */
internal const val SLOW_MILLIS = 2600
internal fun slowDegrees(players: Int): Float = minOf(360f / players * 1.7f, 110f)

/**
 * 지금 [from]도에서 몇 도까지 돌릴지. 바늘(맨 위)이 [bomb]번 칸의 [inside](0~1) 지점에 오도록 맞춘다.
 * 바퀴가 시계 방향으로 θ만큼 돌면 바늘 아래는 바퀴의 -θ도 지점이다.
 */
internal fun spinTarget(from: Float, bomb: Int, players: Int, power: Float, inside: Float): Float {
    val slice = 360f / players
    val local = (bomb + inside) * slice
    val rest = ((-local - from) % 360f + 360f) % 360f
    return from + spinTurns(power) * 360f + rest
}

/** 바퀴가 [angle]도 돌았을 때 바늘이 가리키는 칸 */
internal fun sliceAt(angle: Float, players: Int): Int {
    val local = ((-angle) % 360f + 360f) % 360f
    return (local / (360f / players)).toInt().coerceAtMost(players - 1)
}

private fun powerLabel(p: Float) = when {
    p < 0.34f -> "살살"
    p < 0.67f -> "적당히"
    else -> "세게!"
}

/**
 * 룰렛 돌리기 (5.1, 시안: "룰렛 돌리기").
 * 사람 수만큼 같은 크기 칸. 버튼을 꾹 누르면 오른쪽 게이지가 찼다 줄었다 하고, 떼는 순간의 힘으로 돈다.
 * 힘은 얼마나 오래·많이 도는지만 바꾸고, 바늘이 멈출 칸은 시작할 때 고르게 뽑아 둔 몰빵 자리다 (조준해서 피할 수 없음).
 */
@Composable
internal fun RouletteGame(session: MiniGameSession, loser: Int?, onReveal: (Int, Boolean) -> Unit) {
    val n = session.players.size
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val scope = rememberCoroutineScope()
    val reveal by rememberUpdatedState(onReveal)
    var state by remember(session) { mutableStateOf(Spin.READY) }
    var power by remember(session) { mutableFloatStateOf(0f) }
    val angle = remember(session) { Animatable(0f) }
    var slow by remember(session) { mutableStateOf(false) }
    // 슬로우 모션 동안 바늘 쪽으로 살짝 다가가기
    val zoom by animateFloatAsState(if (slow && state == Spin.SPINNING) 1.14f else 1f, tween(700), label = "zoom")
    val slice = 360f / n

    // 꾹 누르는 동안 게이지 왕복
    LaunchedEffect(state) {
        if (state != Spin.CHARGING) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            power = gaugeAt((now - start) / 1e9f)
        }
    }
    // 돌아가는 동안 칸 경계를 지날 때마다 틱
    if (state == Spin.SPINNING) {
        val passed = floor(angle.value / slice).toInt()
        LaunchedEffect(passed) { sound.play(Sfx.TICK) }
    }

    fun release() {
        if (state != Spin.CHARGING) return
        state = Spin.SPINNING
        val p = power
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        sound.play(Sfx.RATCHET)
        // 바늘(맨 위)이 몰빵 칸 안쪽에 오도록 (칸 경계에 딱 걸치지 않게). 슬로우 모션 중에 칸 경계를 넘어 들어가 멈춘다
        val from = angle.value
        val target = spinTarget(from, session.bomb, n, p, 0.15f + Random.nextFloat() * 0.75f)
        val d2 = slowDegrees(n)
        val d1 = target - from - d2
        val t1 = spinMillis(p)
        // 빠르게 돌다 → 슬로우 모션으로 이어질 때 속도가 끊기지 않게: 앞 구간 끝 속도 = 뒤 구간(1-(1-u)^4) 시작 속도
        val endSlope = (4f * d2 * t1 / (d1 * SLOW_MILLIS)).coerceIn(0f, 1f)
        val fast = Easing { u -> endSlope * u + (1 - endSlope) * (1 - (1 - u) * (1 - u)) }
        val crawl = Easing { u -> 1 - (1 - u).pow(4) }
        scope.launch {
            angle.animateTo(from + d1, tween(t1, easing = fast))
            slow = true
            sound.play(Sfx.DRUM)
            angle.animateTo(target, tween(SLOW_MILLIS, easing = crawl))
            state = Spin.STOPPED
            reveal(session.bomb, true)
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val gaugeW = 50.dp
            val standH = 64.dp
            val d = minOf(maxWidth - gaugeW - 30.dp, maxHeight - standH - 40.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 누르는 동안 힘만큼 바퀴가 부르르
                    val shake = if (state == Spin.CHARGING) (if ((power * 40).toInt() % 2 == 0) 1 else -1) * power * 2.5f else 0f
                    Box(
                        Modifier.size(d + 24.dp).graphicsLayer {
                            scaleX = zoom; scaleY = zoom
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.05f)
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Stand(Modifier.align(Alignment.Center).offset(y = (d + standH) / 2).size(d * 0.62f, standH + 12.dp))
                        Wheel(session, angle.value + shake, slice, d, loser)
                        Pointer(Modifier.align(Alignment.TopCenter))
                    }
                    Spacer(Modifier.height(standH - 12.dp))
                }
                Gauge(power, state, Modifier.width(gaugeW).height(d + standH + 12.dp))
            }
        }
        Text(
            when (state) {
                Spin.READY -> "아무나 버튼을 꾹 누르고, 원하는 힘에서 떼세요"
                Spin.CHARGING -> "${powerLabel(power)} · 떼면 돌아가요!"
                Spin.SPINNING -> if (slow) "두근두근… 어디서 멈출까?" else "빙글빙글… ${powerLabel(power)} 돌렸어요"
                Spin.STOPPED -> loser?.let { "${session.players[it].name} 당첨!" } ?: ""
            },
            Modifier.fillMaxWidth(), fontSize = 16.sp, color = if (state == Spin.STOPPED) Pal.TomatoText else Pal.InkSoft, textAlign = TextAlign.Center,
        )
        HoldButton(state) { down ->
            if (down && state == Spin.READY) {
                state = Spin.CHARGING
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound.play(Sfx.CLICK)
            } else if (!down) release()
        }
    }
}

@Composable
private fun Wheel(session: MiniGameSession, rotation: Float, slice: Float, d: androidx.compose.ui.unit.Dp, loser: Int?) {
    val n = session.players.size
    Box(Modifier.size(d).graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            val tl = Offset(c.x - r, c.y - r)
            for (i in 0 until n) {
                // 0도 = 맨 위, 시계 방향 (drawArc는 오른쪽이 0도라 -90)
                val col = if (loser != null && i != loser) SliceColors[i % SliceColors.size].copy(alpha = 0.35f)
                else if (i == loser) Pal.Tomato else SliceColors[(if (n % SliceColors.size == 1 && i == n - 1) 1 else i) % SliceColors.size]
                drawArc(col, -90f + i * slice, slice, true, tl, Size(r * 2, r * 2))
            }
            for (i in 0 until n) {
                val a = Math.toRadians((-90f + i * slice).toDouble())
                drawLine(Pal.Ink, c, Offset(c.x + (r * kotlin.math.cos(a)).toFloat(), c.y + (r * kotlin.math.sin(a)).toFloat()), 3.dp.toPx())
            }
            drawCircle(Pal.Ink, r - 2.dp.toPx(), c, style = Stroke(5.dp.toPx()))
            // 테두리 전구
            val bulbs = 24
            for (k in 0 until bulbs) {
                val a = Math.toRadians(k * 360.0 / bulbs)
                drawCircle(if (k % 2 == 0) Pal.Paper else Pal.Sun, 3.5.dp.toPx(),
                    Offset(c.x + ((r - 2.dp.toPx()) * kotlin.math.cos(a)).toFloat(), c.y + ((r - 2.dp.toPx()) * kotlin.math.sin(a)).toFloat()))
            }
        }
        val sprite = (d * (if (n <= 4) 0.2f else if (n <= 6) 0.16f else 0.12f))
        session.players.forEachIndexed { i, p ->
            val mid = (i + 0.5f) * slice
            Column(
                Modifier.rotate(mid).offset(y = -d * 0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimalSprite(p.animal, sprite)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (n <= 6) LaneSticker(p.lane, size = 14.dp, border = 1.5.dp)
                    Text(p.name, fontSize = if (n <= 6) 13.sp else 11.sp, color = if (i == loser) Color.White else Pal.Ink, maxLines = 1)
                }
            }
        }
        // 가운데 축
        Box(Modifier.size(d * 0.16f).clip(CircleShape).background(Pal.Paper).border(4.dp, Pal.Ink, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(d * 0.06f).clip(CircleShape).background(Pal.Tomato))
        }
    }
}

/** 바퀴 받침대: 두 다리 + 바닥 판 */
@Composable
private fun Stand(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 3.5.dp.toPx()
        val legs = Path().apply {
            moveTo(w * 0.42f, 0f); lineTo(w * 0.58f, 0f); lineTo(w * 0.86f, h * 0.8f); lineTo(w * 0.14f, h * 0.8f); close()
        }
        drawPath(legs, Color(0xFFB983FF))
        drawPath(legs, Pal.Ink, style = Stroke(sw, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        val hole = Path().apply {
            moveTo(w * 0.5f, h * 0.3f); lineTo(w * 0.66f, h * 0.8f); lineTo(w * 0.34f, h * 0.8f); close()
        }
        drawPath(hole, Pal.Ivory)
        drawPath(hole, Pal.Ink, style = Stroke(sw, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawRoundRect(Pal.Sun, Offset(0f, h * 0.76f), Size(w, h * 0.22f), androidx.compose.ui.geometry.CornerRadius(h * 0.11f))
        drawRoundRect(Pal.Ink, Offset(0f, h * 0.76f), Size(w, h * 0.22f), androidx.compose.ui.geometry.CornerRadius(h * 0.11f), style = Stroke(sw))
    }
}

/** 맨 위 바늘 (아래를 가리킴) */
@Composable
private fun Pointer(modifier: Modifier) {
    Canvas(modifier.size(40.dp, 46.dp)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.12f, h * 0.08f); lineTo(w * 0.88f, h * 0.08f); lineTo(w * 0.5f, h * 0.95f); close()
        }
        drawPath(p, Pal.Tomato)
        drawPath(p, Pal.Ink, style = Stroke(3.5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawCircle(Pal.Paper, w * 0.13f, Offset(w * 0.5f, h * 0.32f))
        drawCircle(Pal.Ink, w * 0.13f, Offset(w * 0.5f, h * 0.32f), style = Stroke(2.5.dp.toPx()))
    }
}

/** 오른쪽 세로 힘 게이지: 아래 살살(초록) → 위 세게(빨강) */
@Composable
private fun Gauge(power: Float, state: Spin, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("세게", fontSize = 14.sp, color = Pal.TomatoText)
        Box(
            Modifier
                .weight(1f)
                .width(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Pal.Line)
                .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp)),
        ) {
            val shown = if (state == Spin.READY) 0f else power
            Canvas(Modifier.fillMaxSize().padding(5.dp)) {
                val grad = Brush.verticalGradient(listOf(Color(0xFFF2543F), Color(0xFFFFC93C), Color(0xFF6BCB77)))
                val top = size.height * (1f - shown)
                drawRoundRect(grad, Offset(0f, top), Size(size.width, size.height - top), androidx.compose.ui.geometry.CornerRadius(size.width / 2))
                // 눈금
                for (k in 1..3) {
                    val y = size.height * k / 4
                    drawLine(Pal.Ink.copy(alpha = 0.35f), Offset(size.width * 0.15f, y), Offset(size.width * 0.85f, y), 2.dp.toPx())
                }
                if (state != Spin.READY) drawLine(Pal.Ink, Offset(-4.dp.toPx(), top), Offset(size.width + 4.dp.toPx(), top), 4.dp.toPx())
            }
        }
        Text("살살", fontSize = 14.sp, color = Color(0xFF3E8E4A))
        Text(if (state == Spin.READY) "-" else "${(power * 100).toInt()}%", fontSize = 15.sp, color = Pal.Ink)
    }
}

/** 꾹 누르기 버튼: 누를 때 [onHold](true), 뗄 때 [onHold](false) */
@Composable
private fun HoldButton(state: Spin, onHold: (Boolean) -> Unit) {
    val hold by rememberUpdatedState(onHold)
    val enabled = state == Spin.READY || state == Spin.CHARGING
    val pressed = state == Spin.CHARGING
    Box(Modifier.fillMaxWidth().height(70.dp)) {
        Box(Modifier.fillMaxWidth().height(64.dp).offset(y = 6.dp).clip(RoundedCornerShape(20.dp)).background(Pal.Ink))
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset(y = if (pressed) 5.dp else 0.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (!enabled) Pal.Disabled else if (pressed) Color(0xFFD9412E) else Pal.Tomato)
                .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(onPress = {
                        hold(true)
                        tryAwaitRelease()
                        hold(false)
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (state) {
                    Spin.READY -> "꾹 눌러서 힘 모으기"
                    Spin.CHARGING -> "지금이다! 떼기"
                    Spin.SPINNING -> "돌아가는 중…"
                    Spin.STOPPED -> "멈췄어요"
                },
                fontSize = 22.sp, color = if (enabled) Color.White else Pal.DisabledInk,
            )
        }
    }
}
