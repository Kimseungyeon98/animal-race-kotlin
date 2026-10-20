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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.animalrace.race.TRACK
import com.example.animalrace.ui.components.LaneBadge
import com.example.animalrace.ui.theme.DisplayFont
import com.example.animalrace.ui.theme.LocalRaceColors
import com.example.animalrace.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

private const val DT = 1.0 / 60

@Composable
fun RaceScreen(animals: List<Animal>, bet: String, raceKey: Int, onFinished: (Race) -> Unit) {
    val race = remember(raceKey) { Race(animals) }
    var view by remember(raceKey) { mutableStateOf(race.view()) }
    var countdown by remember(raceKey) { mutableStateOf<String?>(null) }
    var speed by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val finish by rememberUpdatedState(onFinished)

    // 카운트다운 → 고정 간격(1/60초)으로 시뮬레이션을 돌리고 매 프레임 스냅샷을 갱신
    LaunchedEffect(race) {
        for (c in listOf("3", "2", "1", "출발!")) {
            countdown = c
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(if (c == "출발!") 450 else 750)
        }
        countdown = null
        var last = withFrameNanos { it }
        var acc = 0.0
        var lastEventId = -1
        while (!race.done) {
            val now = withFrameNanos { it }
            acc += min(0.1, (now - last) / 1e9) * speed
            last = now
            while (acc >= DT && !race.done) { race.step(DT); acc -= DT }
            val next = race.view()
            val newest = next.recentEvents.firstOrNull()
            if (newest != null && newest.id != lastEventId) {
                lastEventId = newest.id
                if (newest.kind != EventKind.SKILL) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            view = next
        }
        view = race.view()
        delay(1800)
        finish(race)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RaceHeader(bet, view.t, fast = speed > 1f) { speed = if (speed > 1f) 1f else 2f }
            Standings(view)
            Track(view, countdown)
            Commentary(view.recentEvents, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RaceHeader(bet: String, t: Double, fast: Boolean, onToggleSpeed: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "live")
    val dot by pulse.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).graphicsLayer { alpha = dot }.clip(CircleShape).background(LocalRaceColors.current.penalty))
                Spacer(Modifier.width(6.dp))
                Text("LIVE", style = NumberStyle, fontSize = 11.sp, letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(bet.ifEmpty { "자유 경기" }, style = MaterialTheme.typography.headlineSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("%.1f".format(t), style = NumberStyle, fontSize = 30.sp)
        Text("초", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 2.dp, top = 8.dp))
        Spacer(Modifier.width(10.dp))
        FilterChip(
            selected = fast, onClick = onToggleSpeed, label = { Text("2배속") },
            leadingIcon = if (fast) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
        )
    }
}

/** 실시간 순위 띠. 순위가 바뀌면 칩이 미끄러지듯 자리를 바꾼다. */
@Composable
private fun Standings(view: RaceView) {
    val race = LocalRaceColors.current
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth().height(46.dp)) {
        val n = view.racers.size
        val gap = 5.dp
        val chipW = minOf((maxWidth - gap * (n - 1)) / n, 60.dp)
        view.racers.forEach { r ->
            key(r.lane) {
                val x by animateDpAsState((chipW + gap) * r.rank, spring(stiffness = Spring.StiffnessMediumLow), label = "pos")
                val bg = when {
                    r.rank == 0 -> race.gold
                    r.finished -> scheme.primaryContainer
                    else -> scheme.surfaceContainerHigh
                }
                Column(
                    Modifier
                        .offset(x = x)
                        .width(chipW)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("${r.rank + 1}", style = NumberStyle, fontSize = 10.sp,
                        color = if (r.rank == 0) Color(0xFF2A1F00) else scheme.onSurfaceVariant)
                    Text(r.animal.emoji, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun Track(view: RaceView, countdown: String?) {
    val colors = LocalRaceColors.current
    val n = view.racers.size
    val laneH = if (n <= 6) 56.dp else 44.dp
    val runner = if (n <= 6) 38.dp else 32.dp
    val labelW = 40.dp
    val finishInset = 22.dp
    val startPad = 4.dp
    val measurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(laneH * n)
            .clip(shape)
            .border(3.dp, colors.turfEdge, shape),
    ) {
        val travel = maxWidth - finishInset - runner / 2 - labelW - startPad
        fun leftOf(x: Double): Dp = labelW + startPad + travel * (x / TRACK).toFloat()

        // 잔디, 레인, 거리 표지, 결승선, 먼지
        Canvas(Modifier.matchParentSize()) {
            val lh = laneH.toPx()
            val lw = labelW.toPx()
            for (i in 0 until n) {
                drawRect(if (i % 2 == 0) colors.turfLight else colors.turfDark, Offset(0f, i * lh), Size(size.width, lh))
            }
            val stripe = 34.dp.toPx()
            var sx = lw
            var k = 0
            while (sx < size.width) {
                if (k % 2 == 0) drawRect(Color.White.copy(alpha = 0.045f), Offset(sx, 0f), Size(stripe, size.height))
                sx += stripe; k++
            }
            drawRect(Color.Black.copy(alpha = 0.22f), size = Size(lw, size.height))
            val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            for (i in 1 until n) {
                drawLine(Color.White.copy(alpha = 0.32f), Offset(lw, i * lh), Offset(size.width, i * lh), 1.5.dp.toPx(), pathEffect = dash)
            }
            drawLine(colors.rail, Offset(lw + 1.5.dp.toPx(), 0f), Offset(lw + 1.5.dp.toPx(), size.height), 3.dp.toPx())

            // 250m 단위 거리 표지 (트랙 1000m 기준)
            val runnerHalf = runner.toPx() / 2
            for (q in 1..3) {
                val px = leftOf(TRACK * q / 4).toPx() + runnerHalf
                drawLine(Color.White.copy(alpha = 0.22f), Offset(px, 0f), Offset(px, size.height), 1.dp.toPx())
                drawText(
                    measurer, "${q * 250}m", topLeft = Offset(px + 3.dp.toPx(), 2.dp.toPx()),
                    style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontFamily = NumberStyle.fontFamily),
                )
            }

            // 체커 결승선
            val fx = size.width - finishInset.toPx()
            val cell = 6.dp.toPx()
            var y = 0f
            var row = 0
            while (y < size.height) {
                for (col in 0..1) {
                    drawRect(
                        if ((row + col) % 2 == 0) Color(0xFF111111) else Color.White,
                        Offset(fx - cell + col * cell, y), Size(cell, cell),
                    )
                }
                y += cell; row++
            }

            // 달리는 동물 뒤로 흩날리는 먼지 / 가속 시 불꽃
            view.racers.forEach { r ->
                if (r.finished || r.stopped || r.x <= 0) return@forEach
                val cx = leftOf(r.x).toPx() + runnerHalf * 0.4f
                val cy = r.lane * lh + lh / 2 + runnerHalf * 0.55f
                val spread = if (r.boosted) 9.dp.toPx() else 6.dp.toPx()
                for (j in 1..4) {
                    val wobble = sin(view.t * 22 + j * 1.7 + r.lane).toFloat() * 2.dp.toPx()
                    drawCircle(
                        color = if (r.boosted) Color(0xFFFFB13B).copy(alpha = 0.55f - j * 0.12f)
                        else colors.rail.copy(alpha = 0.42f - j * 0.09f),
                        radius = (5 - j) * 1.5.dp.toPx(),
                        center = Offset(cx - j * spread, cy + wobble),
                    )
                }
            }
        }

        // 왼쪽 출전 번호 + 현재 순위
        view.racers.forEach { r ->
            Column(
                Modifier
                    .offset(y = laneH * r.lane)
                    .width(labelW)
                    .height(laneH),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LaneBadge(r.lane, size = 20.dp)
                Text(
                    "${r.rank + 1}위", style = NumberStyle, fontSize = 9.sp,
                    color = if (r.rank == 0) colors.gold else Color.White.copy(alpha = 0.85f),
                )
            }
        }

        // 주자
        view.racers.forEach { r ->
            val moving = !r.finished && !r.stopped && view.t > 0
            val phase = view.t * (if (r.boosted) 26 else 17) + r.lane * 1.3
            Box(
                Modifier
                    .offset(x = leftOf(r.x), y = laneH * r.lane + (laneH - runner) / 2)
                    .size(runner),
            ) {
                Text(
                    r.animal.emoji,
                    fontSize = (runner.value * 0.74f).sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            scaleX = if (r.boosted) -1.12f else -1f
                            scaleY = if (r.boosted) 1.12f else 1f
                            translationY = if (moving) (sin(phase).toFloat() * 2.5f).dp.toPx() else 0f
                            rotationZ = if (moving) sin(phase + 1).toFloat() * 6f else 0f
                            alpha = if (r.stopped) 0.75f else 1f
                        },
                )
                if (r.fx != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-12).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) { Text(r.fx, fontSize = 11.sp) }
                } else if (r.rank == 0 && !r.finished && view.t > 1) {
                    Text("👑", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-11).dp))
                }
                if (r.finished) {
                    Text(
                        "${r.rank + 1}착", style = NumberStyle, fontSize = 9.sp, color = Color(0xFF2A1F00),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-8).dp, y = (-6).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.gold)
                            .padding(horizontal = 3.dp),
                    )
                }
            }
        }

        // 카운트다운
        AnimatedVisibility(countdown != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = countdown ?: "",
                    transitionSpec = { (scaleIn(initialScale = 1.8f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
                    label = "countdown",
                ) { c ->
                    Text(
                        c, color = Color.White, fontFamily = DisplayFont, fontSize = 76.sp,
                        style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 6f), 0f)),
                    )
                }
            }
        }

        // 경기 종료 배너
        AnimatedVisibility(
            view.done,
            enter = slideInVertically { it } + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        ) {
            Text(
                "🏁 경기 종료! 결과 집계 중…",
                fontWeight = FontWeight.Bold, color = Color(0xFF2A1F00),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.gold).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Commentary(events: List<RaceEvent>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val race = LocalRaceColors.current
    if (events.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text("중계 대기 중… 스킬이 터지면 여기에 떠요.", style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }
    LazyColumn(modifier.fillMaxWidth(), userScrollEnabled = false, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(events, key = { it.id }) { ev ->
            val bg = when (ev.kind) {
                EventKind.FINISH -> race.gold.copy(alpha = 0.25f)
                EventKind.LEAD -> scheme.primaryContainer
                EventKind.SKILL -> scheme.surfaceContainerHigh
            }
            Row(
                Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("%.1f".format(ev.t), style = NumberStyle, fontSize = 12.sp,
                    color = scheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                Text(
                    ev.text, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (ev.kind == EventKind.SKILL) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
