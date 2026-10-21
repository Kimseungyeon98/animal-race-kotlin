package com.example.animalrace.ui.race

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import com.example.animalrace.ui.components.CartoonButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Venue
import com.example.animalrace.race.EventKind
import com.example.animalrace.race.Race
import com.example.animalrace.race.RaceEvent
import com.example.animalrace.race.RaceView
import com.example.animalrace.race.RacerView
import com.example.animalrace.race.TRACK
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.Crown
import com.example.animalrace.ui.components.Icons2
import com.example.animalrace.ui.components.InkIcon
import com.example.animalrace.ui.components.LaneSticker
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.theme.Fredoka
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min

private const val DT = 1.0 / 60

/** 결승 후 결승선을 지나 미끄러지는 거리(연출용, m). */
private const val COAST = 22.0

/** 한 프레임: 경주 스냅샷 + 카메라(트랙의 [camX]부터 [camW]미터를 화면 너비에 보여 줌). */
private data class Frame(val view: RaceView, val camX: Double, val camW: Double, val time: Double)

private const val START_W = 130.0
private const val START_X = -0.12 * START_W

/** 아직 달리는 앞쪽 4마리가 들어오게 확대/축소하고, 선두를 화면 70% 지점에 둔다. */
private fun cameraTarget(v: RaceView): Pair<Double, Double> {
    val running = v.racers.filter { !it.finished }.sortedByDescending { it.x }
    // 모두 들어오면 결승선을 화면 가운데쯤 두어 결승선을 지난 동물과 등수 태그가 다 보이게
    if (running.isEmpty()) return (TRACK - 0.45 * 170.0) to 170.0
    val front = running.firstOrNull()?.x ?: TRACK
    val pack = running.take(4)
    val spread = front - (pack.lastOrNull()?.x ?: front)
    val w = (spread * 1.35 + 110).coerceIn(START_W, 420.0)
    val x = (front - 0.70 * w).coerceIn(-0.12 * w, TRACK + 0.2 * w - w)
    return x to w
}

/** 화면 표시용으로 두 시뮬레이션 상태 사이를 [a](0~1) 비율로 섞는다. 순위·이벤트 등은 최신 상태를 쓴다. */
private fun interpolate(prev: RaceView, cur: RaceView, a: Double): RaceView {
    val k = a.coerceIn(0.0, 1.0)
    val byLane = prev.racers.associateBy { it.lane }
    return cur.copy(
        t = prev.t + (cur.t - prev.t) * k,
        racers = cur.racers.map { r ->
            val p = byLane[r.lane] ?: return@map r
            r.copy(
                x = p.x + (r.x - p.x) * k,
                fxElapsed = if (p.fx == r.fx && r.fxElapsed >= p.fxElapsed) p.fxElapsed + (r.fxElapsed - p.fxElapsed) * k else r.fxElapsed,
            )
        },
    )
}

@Composable
fun RaceScreen(
    animals: List<Animal>,
    bet: String,
    raceKey: Int,
    onFinished: (Race, Venue) -> Unit,
    confirmExit: Boolean = false,
    onKeepRacing: () -> Unit = {},
    onQuit: () -> Unit = {},
) {
    val stadium = remember(raceKey) { Stadium.entries.random() }
    val race = remember(raceKey) { Race(animals, venue = stadium.venue) }
    var frame by remember(raceKey) { mutableStateOf(Frame(race.view(), START_X, START_W, 0.0)) }
    var countdown by remember(raceKey) { mutableStateOf<String?>(null) }
    var speed by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val finish by rememberUpdatedState(onFinished)
    // 확인창이 떠 있는 동안 카운트다운·경주 시간이 흐르지 않는다
    val paused by rememberUpdatedState(confirmExit)

    LaunchedEffect(race) {
        for (c in listOf("3", "2", "1", "출발!")) {
            while (paused) delay(50)
            countdown = c
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(if (c == "출발!") 500 else 750)
        }
        while (paused) delay(50)
        countdown = null
        var last = withFrameNanos { it }
        var acc = 0.0
        var prev = race.view()
        var cur = prev
        var post = 0.0
        var camX = START_X
        var camW = START_W
        var lastEventId = -1
        while (true) {
            val now = withFrameNanos { it }
            val step = if (paused) 0.0 else min(0.1, (now - last) / 1e9) * speed
            last = now
            if (!race.done) {
                acc += step
                while (acc >= DT && !race.done) {
                    prev = cur
                    race.step(DT)
                    cur = race.view()
                    acc -= DT
                }
            } else {
                post += step
                if (post > 2.2) break
            }
            // 시뮬레이션은 1/60초 단위로만 진행되므로, 120Hz 화면에서는 두 상태 사이를 보간해서 그린다.
            // (보간 없이 그리면 동물은 두 프레임에 한 번, 카메라는 매 프레임 움직여 겹쳐 보인다)
            val v = if (race.done) cur else interpolate(prev, cur, acc / DT)
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
        finish(race, stadium.venue)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(stadium.skyTop)) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val n = frame.view.racers.size
        val hudBottom = topInset + 126.dp
        val bottomH = 124.dp + bottomInset
        val trackSpace = maxHeight - hudBottom - 90.dp - bottomH
        val laneH = (trackSpace / n).coerceAtMost(110.dp)
        val trackTop = maxHeight - bottomH - laneH * n

        Scenery(frame, stadium, trackTop, Modifier.fillMaxSize())
        Track(frame, stadium, trackTop, laneH, maxWidth)

        Column(
            Modifier
                .padding(top = topInset)
                .padding(horizontal = 14.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(bet, frame.view.t, fast = speed > 1f) { speed = if (speed > 1f) 1f else 2f }
            Standings(frame.view)
        }

        Commentary(
            frame.view.recentEvents,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomH),
        )

        Overlays(frame, stadium, countdown)

        if (confirmExit) ExitDialog(frame.view, onKeepRacing, onQuit, topInset)
    }
}

// ---------------------------------------------------------------- 상단

@Composable
private fun TopBar(bet: String, t: Double, fast: Boolean, onToggleSpeed: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Pal.Paper)
                .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            InkIcon(Icons2.Clock, size = 18.dp)
            Text("%.1f".format(t), style = NumberStyle, fontSize = 22.sp, color = Pal.Ink, modifier = Modifier.width(52.dp))
        }
        Row(
            Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Pal.Cream)
                .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Tag("꼴찌", Pal.Tomato, fontSize = 12.sp)
            Text(shortBet(bet).ifEmpty { "자유 경기" }, fontSize = 15.sp, color = Pal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (fast) Pal.Sun else Pal.Paper)
                .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                .clickable(onClickLabel = "2배속", onClick = onToggleSpeed)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text("x2", style = NumberStyle, fontSize = 15.sp, color = Pal.Ink) }
    }
}

/** 실시간 순위 띠. 역전되면 얼굴이 미끄러지듯 자리를 바꾼다. 1등과 꼴찌는 태그로 표시. */
@Composable
private fun Standings(view: RaceView) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(52.dp)) {
        val n = view.racers.size
        val gap = 8.dp
        val avatar = minOf(48.dp, (maxWidth - gap * (n - 1)) / n)
        view.racers.forEach { r ->
            key(r.lane) {
                val x by animateDpAsState((avatar + gap) * r.rank, spring(stiffness = Spring.StiffnessMediumLow), label = "pos")
                val first = r.rank == 0
                val last = r.rank == n - 1
                Box(Modifier.offset(x = x).size(avatar, 52.dp)) {
                    Box(
                        Modifier
                            .size(avatar)
                            .clip(CircleShape)
                            .background(if (first) Pal.Sun else if (last) Pal.Pink else Pal.Paper)
                            .border(3.dp, Pal.Ink, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { AnimalSprite(r.animal, avatar * 0.92f) }
                    LaneSticker(r.lane, Modifier.offset(x = (-4).dp, y = (-6).dp), size = 20.dp)
                    Tag(
                        if (first) "1등" else if (last) "꼴찌" else "${r.rank + 1}",
                        bg = if (first) Pal.Ink else if (last) Pal.Tomato else Pal.Paper,
                        fg = if (first || last) Color.White else Pal.Ink,
                        fontSize = 10.sp, number = !(first || last),
                        modifier = Modifier.align(Alignment.BottomCenter).wrapContentWidth(unbounded = true),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 배경

/** 경기장 배경(하늘·언덕/바다·소품, 느리게)과 울타리(트랙과 같은 속도). */
@Composable
private fun Scenery(frame: Frame, stadium: Stadium, trackTop: Dp, modifier: Modifier) {
    Canvas(modifier) {
        val pxPerM = size.width / frame.camW.toFloat()
        val top = trackTop.toPx()
        stadiumBackdrop(stadium, top, (frame.camX * pxPerM).toFloat(), frame.time)
        fence(stadium, top, frame.camX, pxPerM)
    }
}

@Composable
private fun Track(frame: Frame, stadium: Stadium, trackTop: Dp, laneH: Dp, width: Dp) {
    val view = frame.view
    val n = view.racers.size
    fun sx(worldX: Double): Dp = width * ((worldX - frame.camX) / frame.camW).toFloat()
    fun visualX(r: RacerView): Double =
        if (r.finished) TRACK + min(COAST, (frame.time - (r.finishTime ?: frame.time)).coerceAtLeast(0.0) * 60) else r.x

    Canvas(Modifier.fillMaxSize()) {
        val top = trackTop.toPx()
        val lh = laneH.toPx()
        val pxPerM = size.width / frame.camW.toFloat()
        fun px(m: Double) = ((m - frame.camX) * pxPerM).toFloat()
        for (i in 0 until n) {
            drawRect(if (i % 2 == 0) stadium.dirt else stadium.dirtAlt, Offset(0f, top + i * lh), Size(size.width, lh))
            dashes(stadium.line, top + (i + 1) * lh - 4.dp.toPx(), frame.camX, frame.camW, pxPerM)
            ground(stadium, i, top + i * lh, lh, frame.camX, frame.camW, pxPerM)
        }
        drawLine(Pal.Ink, Offset(0f, top), Offset(size.width, top), 3.dp.toPx())
        // 출발 게이트
        if (px(0.0) > -40) {
            for (i in 0 until n) {
                val y = top + i * lh + 4.dp.toPx()
                drawRect(Color(0xFFB9C2CC), Offset(px(-14.0), y), Size(px(0.0) - px(-14.0), lh - 8.dp.toPx()))
                drawRect(Pal.Ink, Offset(px(-14.0), y), Size(px(0.0) - px(-14.0), lh - 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
        }
        // 체커 결승선
        val fx = px(TRACK)
        if (fx < size.width + 40) {
            val cell = 9.dp.toPx()
            var y = top
            var row = 0
            while (y < top + lh * n) {
                for (col in 0..1) {
                    drawRect(if ((row + col) % 2 == 0) Pal.Ink else Color.White,
                        Offset(fx - cell + col * cell, y), Size(cell, min(cell, top + lh * n - y)))
                }
                y += cell; row++
            }
            drawRect(Pal.Ink, Offset(fx - cell, top), Size(cell * 2, lh * n),
                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
        }
    }

    // 100m마다 거리 표지판
    var d = (floor(frame.camX / 100) * 100).coerceAtLeast(100.0)
    while (d < TRACK && sx(d) < width + 60.dp) {
        val x = sx(d)
        if (x > (-60).dp) {
            Column(Modifier.offset(x = x - 30.dp, y = trackTop - 74.dp).width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${d.toInt()}m", style = NumberStyle, fontSize = 15.sp, color = Pal.Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Pal.Paper)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
                Box(Modifier.size(6.dp, 46.dp).background(Color.White).border(2.dp, Pal.Ink))
            }
        }
        d += 100
    }

    // 레인 번호
    view.racers.forEach { r ->
        LaneSticker(r.lane, Modifier.offset(x = 8.dp, y = trackTop + laneH * r.lane + (laneH - 24.dp) / 2), size = 24.dp)
    }

    // 주자 (코끝이 트랙 위치)
    val sprite = (laneH * 1.2f).coerceAtMost(110.dp)
    val spriteH = sprite * 0.85f
    view.racers.forEach { r ->
        val nose = sx(visualX(r))
        val y = trackTop + laneH * (r.lane + 1) - spriteH - 2.dp
        if (nose < 0.dp) {
            OffscreenTag(r, view, Modifier.offset(x = 40.dp, y = trackTop + laneH * r.lane + (laneH - 30.dp) / 2))
        } else if (nose - sprite < width) {
            Box(Modifier.offset(x = nose - sprite * 0.92f, y = y)) {
                if (!r.finished && !r.stopped) Dust(stadium.dust, r.lane, frame.time, Modifier.size(sprite, spriteH))
                AnimalSprite(
                    r.animal, sprite,
                    running = !r.finished && !r.stopped,
                    fx = r.fx,
                    fxTime = r.fxElapsed.toFloat(),
                    venue = stadium.venue,
                )
                if (r.rank == 0 && !r.finished && r.fx == com.example.animalrace.model.Fx.NONE && view.t > 1) {
                    Crown(Modifier.align(Alignment.TopCenter).offset(x = sprite * 0.12f, y = (-14).dp), width = 30.dp)
                }
            }
            // 등수 태그는 코끝 앞(오른쪽)에, 레인 가운데 높이로 — 윗줄 동물을 가리지 않게
            if (r.finished) {
                val tagH = (laneH * 0.5f).coerceIn(18.dp, 28.dp)
                Tag(
                    "${r.rank + 1}등", if (r.rank == 0) Pal.Sun else if (r.rank == n - 1) Pal.Tomato else Pal.Paper,
                    if (r.rank == n - 1) Color.White else Pal.Ink,
                    fontSize = (tagH.value * 0.55f).sp,
                    modifier = Modifier.offset(x = nose + sprite * 0.1f, y = trackTop + laneH * r.lane + (laneH - tagH) / 2),
                )
            }
        }
    }
}

/** 레인 경계 점선. 바닥 무늬처럼 트랙(월드 좌표)에 붙어 있어 줌이 바뀌어도 바닥과 같은 속도로 흘러간다. */
private fun DrawScope.dashes(color: Color, y: Float, camX: Double, camW: Double, pxPerM: Float) {
    var m = floor(camX / DASH_PERIOD) * DASH_PERIOD
    while (m < camX + camW) {
        val x = ((m - camX) * pxPerM).toFloat()
        drawRect(color, Offset(x, y), Size((DASH_LEN * pxPerM).toFloat(), 4.dp.toPx()))
        m += DASH_PERIOD
    }
}

/** 점선 간격·길이(m). 기본 화면 폭(130m)에서 예전 30dp·18dp 정도 */
private const val DASH_PERIOD = 10.0
private const val DASH_LEN = 6.0

/** 달리는 동물 꼬리 쪽에서 피어올라 뒤로 흩어지는 흙먼지. 크기는 90×77 스프라이트 기준. */
@Composable
private fun Dust(color: Color, lane: Int, time: Double, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 90f
        for (k in 0 until 3) {
            val ph = (((time * 15 + k * 6 + lane * 4) % 18) / 18).toFloat()
            val d = (8 + ph * 12) * u
            val o = Offset((14 - ph * 34) * u, (58 - ph * 10) * u)
            val a = (1 - ph) * 0.9f
            drawOval(color, o, Size(d, d), alpha = a)
            drawOval(Pal.Ink, o, Size(d, d), alpha = a, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
    }
}

/** 화면 왼쪽 밖으로 밀려난 동물: 선두와의 거리. */
@Composable
private fun OffscreenTag(r: RacerView, view: RaceView, modifier: Modifier) {
    val front = view.racers.filter { !it.finished }.maxOfOrNull { it.x } ?: TRACK
    Row(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Pal.Ink.copy(alpha = 0.82f))
            .padding(start = 4.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InkIcon(Icons2.Left, size = 12.dp, color = Color.White, stroke = 3.5f)
        Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            AnimalSprite(r.animal, 26.dp)
        }
        Text(r.animal.label, fontSize = 14.sp, color = Color.White)
        Text("-${(front - r.x).toInt()}m", style = NumberStyle, fontSize = 14.sp, color = Color.White)
    }
}

// ---------------------------------------------------------------- 하단 중계

@Composable
private fun Commentary(events: List<RaceEvent>, modifier: Modifier) {
    Column(modifier.background(Pal.Grass)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(Pal.Ink))
        Row(
            Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Pal.Paper)
                    .border(3.dp, Pal.Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) { InkIcon(Icons2.Mic, size = 26.dp, stroke = 2.2f) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val latest = events.getOrNull(0)?.text ?: "곧 출발해요! 응원할 동물을 지켜보세요"
                // 새 줄은 아래에서 올라오고, 이전 줄은 바로 사라져서 두 글자가 겹쳐 보이지 않게
                AnimatedContent(
                    targetState = latest,
                    transitionSpec = { (slideInVertically(tween(180)) { it / 3 } + fadeIn(tween(180))) togetherWith fadeOut(tween(40)) },
                    label = "line1",
                ) { text ->
                    Text(
                        text, fontSize = 17.sp, color = Pal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Pal.Paper)
                            .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                // 같은 문장이 연달아 나오면 두 줄이 똑같아 보이므로 다른 문장을 둘째 줄에
                events.drop(1).firstOrNull { it.text != latest }?.let { ev ->
                    Text(
                        ev.text, fontSize = 14.sp, color = Color(0xFF3E3456), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(2.dp, Pal.Ink.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 나가기 확인

/**
 * 경주 중 뒤로 가기를 눌렀을 때의 확인창 (시안: "경기 나가기 확인창" 개선 B).
 * 떠 있는 동안 경주는 일시정지. 바깥을 누르거나 뒤로 가기를 다시 누르면 계속 본다.
 */
@Composable
private fun ExitDialog(view: RaceView, onKeep: () -> Unit, onQuit: () -> Unit, topInset: Dp) {
    // 1~3번 출전 동물이 카드 위로 깜짝 놀라 고개를 내민다
    val peek = view.racers.sortedBy { it.lane }.take(3)
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onKeep),
    ) {
        Row(
            Modifier
                .padding(start = 20.dp, top = topInset + 64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Pal.Paper)
                .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(3.dp, 12.dp).background(Pal.Ink))
            Box(Modifier.size(3.dp, 12.dp).background(Pal.Ink))
            Text("일시정지", fontSize = 14.sp, color = Pal.Ink)
        }
        Box(Modifier.align(Alignment.Center).padding(horizontal = 20.dp), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .padding(top = 56.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Pal.Ivory)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(24.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(start = 18.dp, end = 18.dp, top = 64.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("아직 경기 중이에요!", fontSize = 24.sp, color = Pal.Ink)
                Text(
                    "지금 나가면 이번 판은 결과도 기록도 남지 않아요.\n그래도 그만둘까요?",
                    fontSize = 16.sp, color = Pal.InkSoft, textAlign = TextAlign.Center, lineHeight = 22.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CartoonButton(onClick = onQuit, color = Pal.Paper, modifier = Modifier.weight(1f), height = 50.dp, depth = 4.dp) {
                        Text("그만두기", fontSize = 18.sp, color = Pal.Ink)
                    }
                    CartoonButton(onClick = onKeep, color = Pal.Sun, modifier = Modifier.weight(1.3f), height = 50.dp, depth = 4.dp) {
                        Text("계속 볼래요!", fontSize = 18.sp, color = Pal.Ink)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                peek.forEachIndexed { i, r ->
                    val side = i - (peek.size - 1) / 2f
                    AnimalSprite(
                        r.animal, 100.dp, fx = com.example.animalrace.model.Fx.SCARED,
                        modifier = Modifier.offset(y = if (side == 0f) 0.dp else 18.dp).rotate(side * 10f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 오버레이

@Composable
private fun Overlays(frame: Frame, stadium: Stadium, countdown: String?) {
    val view = frame.view
    val winner = view.racers.firstOrNull { it.finished && it.rank == 0 }
    val showWinner = winner != null && frame.time - (winner.finishTime ?: 0.0) < 2.2

    AnimatedVisibility(countdown != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        // 화면 전체를 카운트다운 영역으로 쓰고, 크게 시작한 글자도 잘라내지 않는다
        Box(Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = countdown ?: "",
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    // 숫자는 2배에서 줄어들고, 폭이 넓은 "출발!"은 시작해도 화면 폭을 넘지 않는 배율에서 줄어든다
                    val from = if (targetState.length == 1) 2f else 1.3f
                    (scaleIn(initialScale = from) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "countdown",
            ) { c ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (c.length == 1) OutlinedText(c, 180.sp, color = Pal.Sun, outlineWidth = 26f, fontFamily = Fredoka)
                    else OutlinedText(c, 96.sp, color = Pal.Sun, outlineWidth = 18f)
                }
            }
            OutlinedText(
                if (countdown == "출발!") "달려라!" else "준비…", 24.sp, outlineWidth = 6f,
                modifier = Modifier.offset(y = 130.dp),
            )
            // 이번 경기장 이름표 (카운트다운 때만)
            Text(
                stadium.venue.label, fontSize = 20.sp, color = Pal.Ink,
                modifier = Modifier
                    .offset(y = (-150).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pal.Paper)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            )
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
                    .rotate(-3f)
                    .padding(horizontal = 50.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Pal.Sun)
                    .border(4.dp, Pal.Ink, RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("1등 골인!", fontSize = 22.sp, color = Pal.Ink)
                AnimalSprite(w.animal, 150.dp, running = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LaneSticker(w.lane, size = 26.dp, border = 3.dp)
                    Text(w.animal.label, fontSize = 32.sp, color = Pal.Ink)
                }
                Text("%.2f초".format(w.finishTime ?: 0.0), style = NumberStyle, fontSize = 18.sp, color = Pal.Ink)
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(bottom = 160.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(view.done && !showWinner, enter = slideInVertically { it } + fadeIn()) {
            Text(
                "경기 종료! 결과 집계 중…", fontSize = 17.sp, color = Pal.Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Pal.Sun)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
