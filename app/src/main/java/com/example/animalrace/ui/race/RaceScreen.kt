package com.example.animalrace.ui.race

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.Animal
import com.example.animalrace.race.EventKind
import com.example.animalrace.race.Race
import com.example.animalrace.race.RaceEvent
import com.example.animalrace.race.RaceView
import com.example.animalrace.race.RacerView
import com.example.animalrace.race.TRACK
import com.example.animalrace.ui.components.LaneBadge
import com.example.animalrace.ui.theme.DisplayFont
import com.example.animalrace.ui.theme.LocalRaceColors
import com.example.animalrace.ui.theme.MonoFont
import com.example.animalrace.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

private const val DT = 1.0 / 60

/** 결승 후 결승선을 지나 미끄러지는 거리(시각 효과용). */
private const val COAST = 22.0

/**
 * 한 프레임에 그릴 것: 경주 스냅샷 + 카메라.
 * 카메라는 트랙의 [camX]부터 [camW]만큼(미터)을 화면 너비에 보여 준다.
 */
private data class Frame(val view: RaceView, val camX: Double, val camW: Double, val time: Double)

private const val START_W = 130.0
private const val START_X = -0.12 * START_W

/**
 * 카메라 목표: 아직 달리는 동물 중 앞쪽 4마리가 들어오도록 확대/축소하고,
 * 선두를 화면 70% 지점에 둔다. 출발선과 결승선 밖으로는 나가지 않는다.
 */
private fun cameraTarget(v: RaceView): Pair<Double, Double> {
    val running = v.racers.filter { !it.finished }.sortedByDescending { it.x }
    val front = running.firstOrNull()?.x ?: TRACK
    val pack = running.take(4)
    val spread = front - (pack.lastOrNull()?.x ?: front)
    val w = (spread * 1.35 + 110).coerceIn(START_W, 420.0)
    val x = (front - 0.70 * w).coerceIn(-0.12 * w, TRACK + 0.2 * w - w)
    return x to w
}

@Composable
fun RaceScreen(animals: List<Animal>, bet: String, raceKey: Int, onFinished: (Race) -> Unit) {
    val race = remember(raceKey) { Race(animals) }
    var frame by remember(raceKey) { mutableStateOf(Frame(race.view(), START_X, START_W, 0.0)) }
    var countdown by remember(raceKey) { mutableStateOf<String?>(null) }
    var speed by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(race) {
        for (c in listOf("3", "2", "1", "출발!")) {
            countdown = c
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(if (c == "출발!") 450 else 750)
        }
        countdown = null
        var last = withFrameNanos { it }
        var acc = 0.0
        var post = 0.0
        var camX = START_X
        var camW = START_W
        var lastEventId = -1
        while (true) {
            val now = withFrameNanos { it }
            val step = min(0.1, (now - last) / 1e9) * speed
            last = now
            if (!race.done) {
                acc += step
                while (acc >= DT && !race.done) { race.step(DT); acc -= DT }
            } else {
                post += step
                if (post > 2.0) break
            }
            val v = race.view()
            val (tx, tw) = cameraTarget(v)
            camX += (tx - camX) * (1 - exp(-step * 3.5))
            camW += (tw - camW) * (1 - exp(-step * 1.8))
            v.recentEvents.firstOrNull()?.let { ev ->
                if (ev.id != lastEventId) {
                    lastEventId = ev.id
                    if (ev.kind != EventKind.SKILL) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            frame = Frame(v, camX, camW, v.t + post)
        }
        finish(race)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF16231B))) {
        Column(Modifier.fillMaxSize()) {
            Hud(bet, frame.view, fast = speed > 1f) { speed = if (speed > 1f) 1f else 2f }
            Stage(frame, Modifier.weight(1f))
        }
        Overlays(frame, countdown)
    }
}

// ---------------------------------------------------------------- 상단 HUD

@Composable
private fun Hud(bet: String, view: RaceView, fast: Boolean, onToggleSpeed: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "live")
    val dot by pulse.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).graphicsLayer { alpha = dot }.clip(CircleShape).background(LocalRaceColors.current.penalty))
                Spacer(Modifier.width(6.dp))
                Text(
                    bet.ifEmpty { "자유 경기" }, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text("%.1f".format(view.t), style = NumberStyle, fontSize = 24.sp)
                Text("초", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 2.dp, top = 6.dp))
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = fast, onClick = onToggleSpeed, label = { Text("2배속") },
                    leadingIcon = if (fast) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                )
            }
            Standings(view)
        }
    }
}

/** 실시간 순위 띠. 역전이 일어나면 칩이 미끄러지듯 자리를 바꾼다. */
@Composable
private fun Standings(view: RaceView) {
    val race = LocalRaceColors.current
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth().height(42.dp)) {
        val n = view.racers.size
        val gap = 4.dp
        val chipW = minOf((maxWidth - gap * (n - 1)) / n, 58.dp)
        view.racers.forEach { r ->
            key(r.lane) {
                val x by animateDpAsState((chipW + gap) * r.rank, spring(stiffness = Spring.StiffnessMediumLow), label = "pos")
                val bg = when {
                    r.rank == 0 -> race.gold
                    r.finished -> scheme.primaryContainer
                    else -> scheme.surfaceContainerHigh
                }
                Column(
                    Modifier.offset(x = x).width(chipW).height(42.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("${r.rank + 1}", style = NumberStyle, fontSize = 9.sp,
                        color = if (r.rank == 0) Color(0xFF2A1F00) else scheme.onSurfaceVariant)
                    Text(r.animal.emoji, fontSize = 16.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 경기장

@Composable
private fun Stage(frame: Frame, modifier: Modifier) {
    val colors = LocalRaceColors.current
    val view = frame.view
    val n = view.racers.size
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val screenW = maxWidth
        val apronH = 150.dp
        val minStand = 56.dp
        val laneH = ((maxHeight - apronH - minStand) / n).coerceAtMost(120.dp)
        val standH = maxHeight - apronH - laneH * n
        val trackTop = standH
        val apronTop = standH + laneH * n
        val runner = (laneH * 0.8f).coerceAtMost(76.dp)

        /** 트랙 좌표(m) → 화면 x(dp) */
        fun sx(worldX: Double): Dp = screenW * ((worldX - frame.camX) / frame.camW).toFloat()
        fun visualX(r: RacerView): Double =
            if (r.finished) TRACK + min(COAST, (frame.time - (r.finishTime ?: frame.time)) * 60) else r.x

        Canvas(Modifier.fillMaxSize()) {
            val pxPerM = size.width / frame.camW.toFloat()
            fun px(worldX: Double) = ((worldX - frame.camX) * pxPerM).toFloat()
            val standPx = standH.toPx()
            val laneHpx = laneH.toPx()
            val trackTopPx = trackTop.toPx()
            val apronTopPx = apronTop.toPx()

            drawGrandstand(standPx, frame.camX, pxPerM)
            // 잔디 레인 + 깎은 줄무늬(트랙과 함께 흘러감)
            for (i in 0 until n) {
                drawRect(if (i % 2 == 0) colors.turfLight else colors.turfDark,
                    Offset(0f, trackTopPx + i * laneHpx), Size(size.width, laneHpx))
            }
            val band = 40.0
            var m = floor(frame.camX / band) * band
            while (px(m) < size.width) {
                if ((m / band).toInt() % 2 == 0) {
                    drawRect(Color.White.copy(alpha = 0.05f), Offset(px(m), trackTopPx),
                        Size((band * pxPerM).toFloat(), laneHpx * n))
                }
                m += band
            }
            for (i in 1 until n) {
                drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, trackTopPx + i * laneHpx),
                    Offset(size.width, trackTopPx + i * laneHpx), 1.dp.toPx())
            }
            // 100m 마다 거리선
            var d = (floor(frame.camX / 100) * 100).coerceAtLeast(100.0)
            while (px(d) < size.width && d < TRACK) {
                drawLine(Color.White.copy(alpha = 0.22f), Offset(px(d), trackTopPx), Offset(px(d), apronTopPx), 2.dp.toPx())
                d += 100
            }
            // 출발 게이트
            if (px(0.0) > -40) {
                for (i in 0 until n) {
                    val top = trackTopPx + i * laneHpx + 3.dp.toPx()
                    drawRect(Color(0xFF8E9AA6), Offset(px(-14.0), top), Size(px(0.0) - px(-14.0), laneHpx - 6.dp.toPx()))
                    drawRect(Color(0xFF5E6872), Offset(px(-2.0), top), Size(px(0.0) - px(-2.0), laneHpx - 6.dp.toPx()))
                }
            }
            // 결승선 (트랙 체커 + 관중석 위 아치)
            val fx = px(TRACK)
            if (fx < size.width + 40) {
                val cell = (laneHpx / 6).coerceIn(6.dp.toPx(), 12.dp.toPx())
                var y = trackTopPx
                var row = 0
                while (y < apronTopPx) {
                    for (col in 0..1) {
                        drawRect(if ((row + col) % 2 == 0) Color(0xFF111111) else Color.White,
                            Offset(fx - cell + col * cell, y), Size(cell, min(cell, apronTopPx - y)))
                    }
                    y += cell; row++
                }
                drawFinishArch(measurer, fx, standPx, colors.gold)
            }
            drawRail(trackTopPx, frame.camX, pxPerM, colors.rail, measurer)
            drawRail(apronTopPx, frame.camX, pxPerM, colors.rail, null)
            drawApron(apronTopPx, frame.camX, pxPerM)

            // 먼지 / 가속 불꽃
            val runnerPx = runner.toPx()
            view.racers.forEach { r ->
                if (r.finished || r.stopped || r.x <= 0) return@forEach
                val nose = px(r.x)
                val tail = nose - runnerPx * 0.75f
                val cy = trackTopPx + r.lane * laneHpx + laneHpx / 2 + runnerPx * 0.32f
                if (r.boosted) {
                    for (j in 0..2) {
                        val ly = cy - runnerPx * 0.35f + j * runnerPx * 0.18f
                        drawLine(Color(0xFFFFC14D).copy(alpha = 0.6f - j * 0.15f), Offset(tail - runnerPx * 1.2f, ly),
                            Offset(tail - 4.dp.toPx(), ly), 2.5.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                for (j in 1..4) {
                    val wobble = sin(frame.time * 22 + j * 1.7 + r.lane).toFloat() * 2.dp.toPx()
                    drawCircle(
                        color = if (r.boosted) Color(0xFFFFB13B).copy(alpha = 0.5f - j * 0.11f)
                        else Color(0xFFEDE6D2).copy(alpha = 0.45f - j * 0.1f),
                        radius = (5 - j) * runnerPx / 22,
                        center = Offset(tail - j * runnerPx * 0.18f, cy + wobble),
                    )
                }
            }
        }

        // 각 레인 왼쪽 위 출전 번호
        view.racers.forEach { r ->
            LaneBadge(r.lane, size = 18.dp, modifier = Modifier.offset(x = 6.dp, y = trackTop + laneH * r.lane + 4.dp))
        }

        // 주자 (코끝이 트랙 위치에 오도록)
        view.racers.forEach { r ->
            val nose = sx(visualX(r))
            val y = trackTop + laneH * r.lane + (laneH - runner) / 2
            when {
                nose < 0.dp -> OffscreenTag(r, view, Modifier.offset(x = 28.dp, y = trackTop + laneH * r.lane + (laneH - 26.dp) / 2))
                nose - runner > screenW -> Unit
                else -> Runner(r, frame.time, runner, Modifier.offset(x = nose - runner, y = y))
            }
        }

        Commentary(
            view.recentEvents,
            Modifier
                .offset(y = apronTop)
                .height(apronH)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun Runner(r: RacerView, time: Double, size: Dp, modifier: Modifier) {
    val race = LocalRaceColors.current
    val moving = !r.finished && !r.stopped && time > 0
    val phase = time * (if (r.boosted) 26 else 17) + r.lane * 1.3
    Box(modifier.size(size)) {
        Text(
            r.animal.emoji,
            fontSize = (size.value * 0.78f).sp,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = if (r.boosted) -1.12f else -1f
                    scaleY = if (r.boosted) 1.12f else 1f
                    translationY = if (moving) (sin(phase).toFloat() * size.value * 0.07f).dp.toPx() else 0f
                    rotationZ = if (moving) sin(phase + 1).toFloat() * 7f else 0f
                    alpha = if (r.stopped) 0.7f else 1f
                },
        )
        val badgeMod = Modifier.align(Alignment.TopCenter).offset(y = -(size * 0.32f))
        when {
            r.fx != null -> Box(
                badgeMod
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 5.dp, vertical = 3.dp),
            ) { Text(r.fx, fontSize = (size.value * 0.26f).coerceIn(12f, 20f).sp) }
            r.rank == 0 && !r.finished && time > 1 ->
                Text("👑", fontSize = (size.value * 0.3f).coerceIn(12f, 22f).sp, modifier = badgeMod)
        }
        if (r.finished) {
            Text(
                "${r.rank + 1}착", style = NumberStyle, fontSize = 11.sp, color = Color(0xFF2A1F00),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-4).dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(race.gold)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

/** 화면 왼쪽 밖으로 밀려난 동물: 선두와의 거리 표시. */
@Composable
private fun OffscreenTag(r: RacerView, view: RaceView, modifier: Modifier) {
    val front = view.racers.filter { !it.finished }.maxOfOrNull { it.x } ?: TRACK
    Row(
        modifier
            .height(26.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("◀", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(r.animal.emoji, fontSize = 15.sp)
        Spacer(Modifier.width(4.dp))
        Text("-${(front - r.x).toInt()}m", style = NumberStyle, fontSize = 11.sp, color = Color.White)
        if (r.fx != null) Text(" ${r.fx}", fontSize = 12.sp)
    }
}

/** 트랙 아래 흙바닥 위에 깔리는 반투명 중계 채팅. 최신이 아래, 오래될수록 흐려진다. */
@Composable
private fun Commentary(events: List<RaceEvent>, modifier: Modifier) {
    val race = LocalRaceColors.current
    Box(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            reverseLayout = true,
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
        ) {
            itemsIndexed(events.take(4), key = { _, ev -> ev.id }) { i, ev ->
                val accent = when (ev.kind) {
                    EventKind.FINISH -> race.gold
                    EventKind.LEAD -> Color(0xFF7FE0A3)
                    EventKind.SKILL -> Color.White
                }
                Row(
                    Modifier
                        .animateItem()
                        .graphicsLayer { alpha = 1f - i * 0.2f }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("%.1f".format(ev.t), style = NumberStyle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ev.text, color = accent, fontSize = 14.sp,
                        fontWeight = if (ev.kind == EventKind.SKILL) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 전체 화면 오버레이

@Composable
private fun Overlays(frame: Frame, countdown: String?) {
    val race = LocalRaceColors.current
    val view = frame.view
    val winner = view.racers.firstOrNull { it.finished && it.rank == 0 }
    val showWinner = winner != null && frame.time - (winner.finishTime ?: 0.0) < 2.2

    AnimatedVisibility(countdown != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = countdown ?: "",
                transitionSpec = { (scaleIn(initialScale = 2f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
                label = "countdown",
            ) { c ->
                Text(c, color = Color.White, fontFamily = DisplayFont, fontSize = 96.sp,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 8f), 0f)))
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            showWinner,
            enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 300f), initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 1.2f) + fadeOut(),
        ) {
            val w = winner ?: return@AnimatedVisibility
            Column(
                Modifier
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(race.gold, Color(0xFFD08A00))))
                    .padding(horizontal = 36.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🏁 1착", fontFamily = DisplayFont, fontSize = 22.sp, color = Color(0xFF2A1F00))
                Text(w.animal.emoji, fontSize = 64.sp, modifier = Modifier.graphicsLayer { scaleX = -1f })
                Text(w.animal.label, fontFamily = DisplayFont, fontSize = 30.sp, color = Color(0xFF2A1F00))
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(bottom = 170.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(view.done && !showWinner, enter = slideInVertically { it } + fadeIn()) {
            Text(
                "경기 종료! 결과 집계 중…", fontWeight = FontWeight.Bold, color = Color(0xFF2A1F00),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(race.gold).padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- 배경 그리기

private val CrowdColors = listOf(
    Color(0xFFE8505B), Color(0xFFF9D56E), Color(0xFF14B1AB), Color(0xFFF3ECC2),
    Color(0xFF6C8EF5), Color(0xFFFF9F68), Color(0xFFB983FF), Color(0xFFFFFFFF),
)

/** 관중석: 트랙보다 천천히 흘러가는(패럴랙스) 관중 머리들. */
private fun DrawScope.drawGrandstand(height: Float, camX: Double, pxPerM: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2731), Color(0xFF2E3D48)), endY = height), size = Size(size.width, height))
    val spacing = 13.dp.toPx()
    val rowH = 11.dp.toPx()
    val shift = (camX * pxPerM * 0.35).toFloat()
    val rows = ((height - 10.dp.toPx()) / rowH).toInt().coerceIn(1, 6)
    for (row in 0 until rows) {
        val y = height - 8.dp.toPx() - row * rowH
        val rowShift = shift + row * spacing * 0.5f
        val first = floor(rowShift / spacing).toInt()
        var i = first
        while (true) {
            val x = i * spacing - rowShift
            if (x > size.width + spacing) break
            val h = (i * 73856093 xor row * 19349663) and 0x7fffffff
            val c = CrowdColors[h % CrowdColors.size]
            drawCircle(c.copy(alpha = 0.55f + (h % 5) * 0.08f), radius = 3.6.dp.toPx(), center = Offset(x, y - (h % 3).toFloat()))
            i++
        }
    }
    drawRect(Color(0xFF0F171D), Offset(0f, 0f), Size(size.width, 6.dp.toPx()))
}

/** 레일: 10m마다 기둥, 100m마다 거리 표지판. 빠르게 지나가며 속도감을 준다. */
private fun DrawScope.drawRail(y: Float, camX: Double, pxPerM: Float, rail: Color, measurer: TextMeasurer?) {
    drawLine(rail, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
    var m = floor(camX / 10) * 10
    while (true) {
        val x = ((m - camX) * pxPerM).toFloat()
        if (x > size.width) break
        drawLine(rail.copy(alpha = 0.85f), Offset(x, y - 7.dp.toPx()), Offset(x, y), 2.dp.toPx())
        if (measurer != null && m > 0 && m < TRACK && m.toInt() % 100 == 0) {
            val boardW = 44.dp.toPx()
            val boardH = 18.dp.toPx()
            drawRect(Color.White, Offset(x - boardW / 2, y - 7.dp.toPx() - boardH), Size(boardW, boardH))
            drawText(
                measurer, "${m.toInt()}m",
                topLeft = Offset(x - boardW / 2 + 5.dp.toPx(), y - 7.dp.toPx() - boardH + 2.dp.toPx()),
                style = TextStyle(color = Color(0xFF1B2731), fontSize = 11.sp, fontFamily = MonoFont),
            )
        }
        m += 10
    }
}

private fun DrawScope.drawApron(top: Float, camX: Double, pxPerM: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFB98E5C), Color(0xFF8C6640)), startY = top, endY = size.height),
        Offset(0f, top), Size(size.width, size.height - top))
    // 흙바닥 결 (트랙과 같은 속도로 흐름)
    var m = floor(camX / 7) * 7
    while (true) {
        val x = ((m - camX) * pxPerM).toFloat()
        if (x > size.width) break
        val h = ((m * 131).toInt() and 0x7fffffff)
        val y = top + 10.dp.toPx() + (h % 9) * ((size.height - top - 20.dp.toPx()) / 9)
        drawLine(Color(0xFF6E4E30).copy(alpha = 0.35f), Offset(x, y), Offset(x + 9.dp.toPx(), y), 2.dp.toPx(), cap = StrokeCap.Round)
        m += 7
    }
}

private fun DrawScope.drawFinishArch(measurer: TextMeasurer, x: Float, standH: Float, gold: Color) {
    val postW = 5.dp.toPx()
    drawRect(gold, Offset(x - postW / 2, 6.dp.toPx()), Size(postW, standH - 6.dp.toPx()))
    val boardW = 64.dp.toPx()
    val boardH = 22.dp.toPx()
    val top = (standH / 2 - boardH / 2).coerceAtLeast(8.dp.toPx())
    drawRect(gold, Offset(x - boardW / 2, top), Size(boardW, boardH))
    drawText(
        measurer, "FINISH", topLeft = Offset(x - boardW / 2 + 9.dp.toPx(), top + 3.dp.toPx()),
        style = TextStyle(color = Color(0xFF2A1F00), fontSize = 12.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold),
    )
}
