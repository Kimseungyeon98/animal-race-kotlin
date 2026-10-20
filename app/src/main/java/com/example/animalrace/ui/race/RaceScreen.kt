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
            delay(if (c == "출발!") 500 else 750)
        }
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
            val step = min(0.1, (now - last) / 1e9) * speed
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
        finish(race)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Pal.Sky)) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val n = frame.view.racers.size
        val hudBottom = topInset + 126.dp
        val bottomH = 124.dp + bottomInset
        val trackSpace = maxHeight - hudBottom - 90.dp - bottomH
        val laneH = (trackSpace / n).coerceAtMost(110.dp)
        val trackTop = maxHeight - bottomH - laneH * n

        Scenery(frame, trackTop, Modifier.fillMaxSize())
        Track(frame, trackTop, laneH, maxWidth)

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

        Overlays(frame, countdown)
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

/** 하늘·구름·언덕(느리게)·울타리(트랙과 같은 속도). */
@Composable
private fun Scenery(frame: Frame, trackTop: Dp, modifier: Modifier) {
    Canvas(modifier) {
        val pxPerM = size.width / frame.camW.toFloat()
        val camPx = (frame.camX * pxPerM).toFloat()
        val top = trackTop.toPx()
        val ink = Pal.Ink
        val line = 3.dp.toPx()

        // 구름 (아주 느리게)
        val cloudShift = (camPx * 0.08f) % (size.width + 200f)
        listOf(0.1f to 0.35f, 0.62f to 0.5f).forEach { (fx, fy) ->
            var cx = fx * size.width - cloudShift
            if (cx < -160f) cx += size.width + 200f
            val cy = top * fy
            drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(cx, cy), Size(120.dp.toPx(), 34.dp.toPx()),
                androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
        }
        // 언덕 (중간 속도)
        val hillW = 320.dp.toPx()
        val hillShift = (camPx * 0.35f) % hillW
        var hx = -hillShift - hillW
        var i = 0
        while (hx < size.width + hillW) {
            val color = if (i % 2 == 0) Pal.HillLight else Pal.HillDark
            val hTop = top - (if (i % 2 == 0) 96.dp else 80.dp).toPx()
            drawOval(color, Offset(hx, hTop), Size(hillW, 220.dp.toPx()))
            drawOval(ink, Offset(hx, hTop), Size(hillW, 220.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(line))
            hx += hillW * 0.62f; i++
        }
        // 울타리: 10m마다 기둥 + 가로 레일
        var m = floor(frame.camX / 10) * 10
        while (true) {
            val x = ((m - frame.camX) * pxPerM).toFloat()
            if (x > size.width) break
            drawRect(Color.White, Offset(x - 4.dp.toPx(), top - 30.dp.toPx()), Size(8.dp.toPx(), 30.dp.toPx()))
            drawRect(ink, Offset(x - 4.dp.toPx(), top - 30.dp.toPx()), Size(8.dp.toPx(), 30.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            m += 10
        }
        drawRect(Color.White, Offset(0f, top - 22.dp.toPx()), Size(size.width, 10.dp.toPx()))
        drawLine(ink, Offset(0f, top - 22.dp.toPx()), Offset(size.width, top - 22.dp.toPx()), line)
        drawLine(ink, Offset(0f, top - 12.dp.toPx()), Offset(size.width, top - 12.dp.toPx()), line)
    }
}

@Composable
private fun Track(frame: Frame, trackTop: Dp, laneH: Dp, width: Dp) {
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
            drawRect(if (i % 2 == 0) Pal.Dirt else Pal.DirtAlt, Offset(0f, top + i * lh), Size(size.width, lh))
            dashes(top + (i + 1) * lh - 4.dp.toPx(), (frame.camX * pxPerM).toFloat())
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
                AnimalSprite(
                    r.animal, sprite,
                    running = !r.finished && !r.stopped,
                    fx = r.fx,
                    fxTime = r.fxElapsed.toFloat(),
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

private fun DrawScope.dashes(y: Float, scrollPx: Float) {
    val period = 30.dp.toPx()
    var x = -(scrollPx % period)
    while (x < size.width) {
        drawRect(Pal.DirtLine, Offset(x, y), Size(18.dp.toPx(), 4.dp.toPx()))
        x += period
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

// ---------------------------------------------------------------- 오버레이

@Composable
private fun Overlays(frame: Frame, countdown: String?) {
    val view = frame.view
    val winner = view.racers.firstOrNull { it.finished && it.rank == 0 }
    val showWinner = winner != null && frame.time - (winner.finishTime ?: 0.0) < 2.2

    AnimatedVisibility(countdown != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.45f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 크게 시작해서 줄어드는 숫자가 들어갈 넉넉한 영역
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = countdown ?: "",
                // 크게 시작해서 줄어드는 동안 영역 밖으로 나간 부분이 잘리지 않게 clip = false
                transitionSpec = {
                    (scaleIn(initialScale = 2f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "countdown",
            ) { c ->
                if (c.length == 1) OutlinedText(c, 180.sp, color = Pal.Sun, outlineWidth = 26f, fontFamily = Fredoka)
                else OutlinedText(c, 96.sp, color = Pal.Sun, outlineWidth = 18f)
            }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedText(if (countdown == "출발!") "달려라!" else "준비…", 24.sp, outlineWidth = 6f)
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
