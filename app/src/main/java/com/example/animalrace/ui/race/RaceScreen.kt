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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.animalrace.ui.sound.LocalSound
import com.example.animalrace.ui.sound.Music
import com.example.animalrace.ui.sound.Sfx
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Fx
import com.example.animalrace.model.RandomRank
import com.example.animalrace.model.Venue
import com.example.animalrace.race.EventKind
import com.example.animalrace.race.Race
import com.example.animalrace.race.RaceEvent
import com.example.animalrace.race.RaceView
import com.example.animalrace.race.RacerView
import com.example.animalrace.race.TRACK
import com.example.animalrace.race.withSubject
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.Crown
import com.example.animalrace.ui.art.headTop
import com.example.animalrace.ui.components.Icons2
import com.example.animalrace.ui.components.InkIcon
import com.example.animalrace.ui.components.LaneSticker
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.theme.Fredoka
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal
import com.example.animalrace.ui.theme.LaneCloth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min

private const val DT = 1.0 / 60

/** 결승 후 결승선을 지나 미끄러지는 거리(연출용, m). */
private const val COAST = 22.0

/** 한 프레임: 경주 스냅샷 + 카메라(트랙의 [camX]부터 [camW]미터를 화면 너비에 보여 줌). */
private data class Frame(val view: RaceView, val camX: Double, val camW: Double, val time: Double)

private const val START_W = 130.0
/** 출발선을 화면 왼쪽에서 이만큼 띄운다: 출발 전 동물(그림 폭 ≈ 화면의 28%)이 잘리지 않게 */
private const val START_LEFT = 0.32
private const val START_X = -START_LEFT * START_W

/** 아직 달리는 앞쪽 4마리가 들어오게 확대/축소하고, 선두를 화면 70% 지점에 둔다. */
private fun cameraTarget(v: RaceView): Pair<Double, Double> {
    val running = v.racers.filter { !it.finished }.sortedByDescending { it.x }
    // 모두 들어오면 결승선을 화면 가운데쯤 두어 결승선을 지난 동물과 등수 태그가 다 보이게
    if (running.isEmpty()) return (TRACK - 0.45 * 170.0) to 170.0
    val front = running.firstOrNull()?.x ?: TRACK
    val pack = running.take(4)
    val spread = front - (pack.lastOrNull()?.x ?: front)
    val w = (spread * 1.35 + 110).coerceIn(START_W, 420.0)
    val x = (front - 0.70 * w).coerceIn(-START_LEFT * w, TRACK + 0.2 * w - w)
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
    /** 랜덤 등수 모드의 당첨 등수. 있으면 카운트다운 전에 룰렛으로 보여 준다 */
    picks: List<Int>? = null,
    /** 당첨 등수를 미리 직접 정한 판: 룰렛 대신 발표 카드만 잠깐 */
    picksFixed: Boolean = false,
    confirmExit: Boolean = false,
    onKeepRacing: () -> Unit = {},
    onQuit: () -> Unit = {},
) {
    val stadium = remember(raceKey) { Stadium.entries.random() }
    val race = remember(raceKey) { Race(animals, venue = stadium.venue) }
    var frame by remember(raceKey) { mutableStateOf(Frame(race.view(), START_X, START_W, 0.0)) }
    var countdown by remember(raceKey) { mutableStateOf<String?>(null) }
    var roulette by remember(raceKey) { mutableStateOf<Roulette?>(null) }
    // 룰렛을 탭하면 남은 칸을 바로 멈추고(발표 중이면 바로 출발로) 넘어간다
    var skip by remember(raceKey) { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val finish by rememberUpdatedState(onFinished)
    // 확인창이 떠 있는 동안 카운트다운·경주 시간이 흐르지 않는다
    val paused by rememberUpdatedState(confirmExit)

    LaunchedEffect(race) {
        // 잠든 오디오 출력을 먼저 깨워 둬야 첫 "3"(또는 룰렛 틱)이 제때 나온다
        sound.warmUp()
        delay(300)
        if (picks != null) {
            // 확인창이 떠 있는 동안은 멈추고, 탭하면 바로 끝나는 기다림
            suspend fun hold(ms: Long) {
                var left = ms
                while (left > 0 && !skip) { delay(30); if (!paused) left -= 30 }
                while (paused) delay(50)
            }
            if (picksFixed) {
                roulette = Roulette(stopped = picks.size, revealed = true, fixed = true)
                sound.play(Sfx.CLACK)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                hold(1600)
                roulette = null
            } else {
                roulette = Roulette(stopped = 0, revealed = false)
                picks.indices.forEach { i ->
                    // 칸이 도는 동안 틱틱틱(점점 느리게) → 멈출 때 딱!
                    val ticking = launch {
                        var gap = 55L
                        while (!skip) { if (!paused) sound.play(Sfx.TICK); delay(gap); gap = (gap * 1.1).toLong().coerceAtMost(190) }
                    }
                    hold(if (i == 0) 1300 else 850)
                    ticking.cancel()
                    roulette = Roulette(stopped = i + 1, revealed = false)
                    if (!skip) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sound.play(Sfx.CLACK) }
                }
                skip = false
                // 두구두구… 쾅! 에 맞춰 발표 (쾅은 소리 시작 1.2초 뒤)
                sound.play(Sfx.DRUM)
                hold(1200)
                roulette = Roulette(stopped = picks.size, revealed = true)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                skip = false
                hold(1800)
                roulette = null
            }
        }
        for (c in listOf("3", "2", "1", "출발!")) {
            while (paused) delay(50)
            countdown = c
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            sound.play(if (c == "출발!") Sfx.GO else Sfx.BEEP)
            if (c == "출발!") sound.music(Music.RACE)
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
                // 다 들어오면 경기 곡은 스르르
                if (post == 0.0) sound.music(null)
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

        // 발표 전까지는 등수를 숨긴다 (빈 목록 = 랜덤 판이지만 아직 모름)
        val shown = picks?.let { if (roulette == null || roulette?.revealed == true) it else emptyList() }

        Scenery(frame, stadium, trackTop, Modifier.fillMaxSize())
        Track(frame, stadium, trackTop, laneH, maxWidth, picks)

        Column(
            Modifier
                .padding(top = topInset)
                .padding(horizontal = 14.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(bet, shown, frame.view.t, fast = speed > 1f) { speed = if (speed > 1f) 1f else 2f }
            Standings(frame.view, shown)
        }

        Commentary(
            withPickLines(frame.view, picks),
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomH),
        )

        Overlays(frame, stadium, countdown)

        roulette?.let { RouletteOverlay(it, picks.orEmpty(), n, bet) { skip = true } }

        if (confirmExit) ExitDialog(frame.view, onKeepRacing, onQuit, topInset)
    }
}

// ---------------------------------------------------------------- 상단

@Composable
private fun TopBar(bet: String, picks: List<Int>?, t: Double, fast: Boolean, onToggleSpeed: () -> Unit) {
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
            when {
                picks == null -> Tag("꼴찌", Pal.Tomato, fontSize = 12.sp)
                picks.isEmpty() -> Tag("🎯 ?등", Pal.Paper, Pal.Ink, fontSize = 12.sp)
                else -> Tag("🎯 ${RandomRank.label(picks)}", Pal.Sun, Pal.Ink, fontSize = 12.sp)
            }
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

/**
 * 실시간 순위 띠. 역전되면 얼굴이 미끄러지듯 자리를 바꾼다. 1등과 꼴찌는 태그로 표시.
 * 랜덤 등수 판([picks]가 null이 아님)은 꼴찌 대신 지금 당첨 등수에 있는 동물을 빨간 태그로.
 */
@Composable
private fun Standings(view: RaceView, picks: List<Int>?) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(52.dp)) {
        val n = view.racers.size
        val gap = 8.dp
        val avatar = minOf(48.dp, (maxWidth - gap * (n - 1)) / n)
        view.racers.forEach { r ->
            key(r.lane) {
                val x by animateDpAsState((avatar + gap) * r.rank, spring(stiffness = Spring.StiffnessMediumLow), label = "pos")
                // 출발 전엔 출전 순서일 뿐이라 1등·꼴찌·당첨 표시를 하지 않는다
                val started = view.t > 0
                val first = started && r.rank == 0
                val hit = started && picks?.contains(r.rank + 1) == true
                val last = hit || (started && picks == null && r.rank == n - 1)
                Box(Modifier.offset(x = x).size(avatar, 52.dp)) {
                    Box(
                        Modifier
                            .size(avatar)
                            .clip(CircleShape)
                            .background(if (last) Pal.Pink else if (first) Pal.Sun else Pal.Paper)
                            .border(3.dp, Pal.Ink, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { AnimalSprite(r.animal, avatar * 0.92f) }
                    LaneSticker(r.lane, Modifier.offset(x = (-4).dp, y = (-6).dp), size = 20.dp)
                    Tag(
                        if (hit) "${r.rank + 1}등" else if (first) "1등" else if (last) "꼴찌" else "${r.rank + 1}",
                        bg = if (last) Pal.Tomato else if (first) Pal.Ink else Pal.Paper,
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
private fun Track(frame: Frame, stadium: Stadium, trackTop: Dp, laneH: Dp, width: Dp, picks: List<Int>?) {
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
        // 출발 게이트: 레인마다 동물 몸 길이만큼 칸막이 칸 (앞문은 동물 위에 따로) (시안: "화면 다듬기" ⑤)
        val gateRight = px(0.0) + 2.dp.toPx()
        val gateLeft = gateRight - (laneH * 1.2f).coerceAtMost(110.dp).toPx() * 1.02f
        if (gateRight > -20) {
            drawRect(Color(0xFFDDE3EA).copy(alpha = 0.55f), Offset(gateLeft, top), Size(gateRight - gateLeft, lh * n))
            for (i in 0..n) {
                val y = top + i * lh
                drawLine(Pal.Ink, Offset(gateLeft, y), Offset(gateRight, y), 4.dp.toPx())
            }
            drawLine(Pal.Ink, Offset(gateLeft, top), Offset(gateLeft, top + lh * n), 5.dp.toPx())
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
        // 당첨 등수로 들어온 동물은 결승 직후 깜짝 놀란다 (표시만, 경주에는 영향 없음)
        val hit = r.finished && picks?.contains(r.rank + 1) == true
        val sinceFinish = frame.time - (r.finishTime ?: frame.time)
        val shock = hit && sinceFinish < 1.6
        if (nose < 0.dp) {
            OffscreenTag(r, view, Modifier.offset(x = 40.dp, y = trackTop + laneH * r.lane + (laneH - 30.dp) / 2))
        } else if (nose - sprite < width) {
            Box(Modifier.offset(x = nose - sprite * 0.92f, y = y)) {
                // 출발 전(카운트다운·룰렛)엔 출발 칸에 서서 기다린다
                val moving = view.t > 0 && !r.finished && !r.stopped
                if (moving) Dust(stadium.dust, r.lane, frame.time, Modifier.size(sprite, spriteH))
                AnimalSprite(
                    r.animal, sprite,
                    running = moving,
                    fx = if (shock) Fx.SCARED else r.fx,
                    fxTime = if (shock) sinceFinish.toFloat() else r.fxElapsed.toFloat(),
                    venue = stadium.venue,
                )
                if (r.rank == 0 && !r.finished && r.fx == Fx.NONE && view.t > 1) {
                    // 머리 꼭대기에 얹기 (동물마다 머리 위치가 달라서 상자 가운데에 두면 허공에 뜬다)
                    val head = headTop(r.animal)
                    Crown(Modifier.offset(x = sprite * (head.x / 200f) - 15.dp, y = sprite * (head.y / 200f) - 19.dp), width = 30.dp)
                }
            }
            // 등수 태그는 코끝 앞(오른쪽)에, 레인 가운데 높이로 — 윗줄 동물을 가리지 않게
            if (r.finished) {
                val tagH = (laneH * 0.5f).coerceIn(18.dp, 28.dp)
                val red = hit || (picks == null && r.rank == n - 1)
                Tag(
                    if (hit) "${r.rank + 1}등 당첨!" else "${r.rank + 1}등",
                    if (red) Pal.Tomato else if (r.rank == 0) Pal.Sun else Pal.Paper,
                    if (red) Color.White else Pal.Ink,
                    fontSize = (tagH.value * 0.55f).sp,
                    modifier = Modifier.offset(x = nose + sprite * 0.1f, y = trackTop + laneH * r.lane + (laneH - tagH) / 2),
                )
            }
        }
    }
    GateFront(frame, trackTop, laneH, n, width)
}

/** 출발 게이트 앞문(출전 번호 색 창살)과 지붕. "출발!"하면 앞문이 위로 말려 올라간다 */
@Composable
private fun GateFront(frame: Frame, trackTop: Dp, laneH: Dp, n: Int, width: Dp) {
    val doorX = width * ((0.0 - frame.camX) / frame.camW).toFloat()
    if (doorX < (-40).dp) return
    val sprite = (laneH * 1.2f).coerceAtMost(110.dp)
    val open = (frame.view.t / 0.3).coerceIn(0.0, 1.0).toFloat()
    Canvas(Modifier.fillMaxSize()) {
        val x = doorX.toPx() + 2.dp.toPx()
        val w = 14.dp.toPx()
        val lh = laneH.toPx()
        val top = trackTop.toPx()
        for (i in 0 until n) {
            val y = top + i * lh
            val h = lh * (1f - open)
            if (h < 1f) continue
            val (cloth, mark) = LaneCloth[i % LaneCloth.size]
            drawRect(cloth, Offset(x, y), Size(w, h))
            drawRect(Pal.Ink, Offset(x, y), Size(w, h), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            for (k in 1..3) {
                val by = y + lh * k / 4f
                if (by < y + h - 4.dp.toPx()) drawLine(mark, Offset(x + 4.dp.toPx(), by), Offset(x + w - 4.dp.toPx(), by), 4.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        // 지붕
        val left = x - sprite.toPx() * 1.02f
        drawRoundRect(Color(0xFFE9EEF3), Offset(left - 4.dp.toPx(), top - 20.dp.toPx()), Size(x + w - left + 8.dp.toPx(), 20.dp.toPx()), CornerRadius(4.dp.toPx()))
        drawRoundRect(Pal.Ink, Offset(left - 4.dp.toPx(), top - 20.dp.toPx()), Size(x + w - left + 8.dp.toPx(), 20.dp.toPx()), CornerRadius(4.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
    }
    Text(
        "START", style = NumberStyle, fontSize = 12.sp, color = Pal.Ink,
        modifier = Modifier.offset(x = doorX - sprite * 0.55f, y = trackTop - 19.dp),
    )
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
                        r.animal, 100.dp, fx = Fx.SCARED,
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
    val sound = LocalSound.current
    LaunchedEffect(showWinner) {
        if (showWinner) sound.play(Sfx.FANFARE)
        sound.duck(showWinner)
    }

    AnimatedVisibility(countdown != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        // 화면 전체를 카운트다운 영역으로 쓰고, 크게 시작한 글자도 잘라내지 않는다
        Box(Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
            // 출발 게이트(화면 왼쪽)를 가리지 않게 오른쪽으로 비켜서
            AnimatedContent(
                targetState = countdown ?: "",
                modifier = Modifier.fillMaxSize().padding(start = 112.dp),
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
                modifier = Modifier.offset(x = 56.dp, y = 130.dp),
            )
            // 이번 경기장 이름표 (카운트다운 때만)
            Text(
                stadium.venue.label, fontSize = 20.sp, color = Pal.Ink,
                modifier = Modifier
                    .offset(x = 56.dp, y = (-150).dp)
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

    // 순위 칩 아래 하늘·펜스 자리에 (아래에 두면 마지막 레인 동물과 등수 태그를 가린다)
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(top = 142.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(view.done && !showWinner, enter = slideInVertically { -it } + fadeIn()) {
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

// ---------------------------------------------------------------- 랜덤 등수 룰렛

/** 룰렛 진행: 멈춘 칸 수, 그리고 결과 발표 중인지. */
private data class Roulette(val stopped: Int, val revealed: Boolean, val fixed: Boolean = false)

/** 해설: 랜덤 등수 판이면 당첨 등수로 들어온 동물마다 한 줄을 더한다 (화면 표시만). */
private fun withPickLines(view: RaceView, picks: List<Int>?): List<RaceEvent> {
    if (picks == null) return view.recentEvents
    val lines = view.racers.filter { it.finished && (it.rank + 1) in picks }.map {
        RaceEvent(-1 - it.lane, it.finishTime ?: view.t, "😱 ${withSubject(it.animal.label)} ${it.rank + 1}등으로 들어왔어요! 당첨!", EventKind.FINISH)
    }
    return (lines + view.recentEvents).sortedByDescending { it.t }
}

/**
 * 출발 전 룰렛 (시안: "랜덤 등수 모드" ②·③).
 * 칸이 하나씩 멈춘 뒤 당첨 등수를 크게 발표한다. 아무 데나 탭하면 [onTap]으로 건너뛴다.
 */
@Composable
private fun RouletteOverlay(state: Roulette, picks: List<Int>, racers: Int, bet: String, onTap: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state.revealed,
            transitionSpec = { (scaleIn(initialScale = 0.85f) + fadeIn()) togetherWith fadeOut(tween(120)) },
            label = "roulette",
        ) { revealed ->
            Column(
                Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Pal.Ivory)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!revealed) {
                    Tag("🎰 오늘의 당첨 등수", Pal.Sun, Pal.Ink, fontSize = 15.sp)
                    Text("${racers}마리 중 ${picks.size}명을 뽑아요", fontSize = 15.sp, color = Pal.InkSoft)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        picks.forEachIndexed { i, p ->
                            Reel(if (i < state.stopped) p else null, racers, seed = i, width = if (picks.size >= 3) 88.dp else 110.dp)
                        }
                    }
                    Text(if (state.stopped < picks.size) "두근두근…" else "결정!", fontSize = 15.sp, color = Pal.InkSoft)
                } else {
                    if (state.fixed) {
                        Tag("🎯 오늘의 당첨 등수", Pal.Sun, Pal.Ink, fontSize = 15.sp)
                        Text("미리 정한 등수예요 · ${racers}마리 중 ${picks.size}명", fontSize = 15.sp, color = Pal.InkSoft)
                    } else Text("오늘의 당첨 등수는…", fontSize = 20.sp, color = Pal.Ink)
                    // 많으면 한 줄에 4개씩, 작게
                    val small = picks.size > 3
                    picks.chunked(4).forEachIndexed { row, line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            line.forEachIndexed { j, p ->
                                val i = row * 4 + j
                                Box(Modifier.rotate(if (i % 2 == 0) -4f else 4f)) {
                                    Box(Modifier.matchParentSize().offset(y = 5.dp).clip(RoundedCornerShape(20.dp)).background(Pal.Ink))
                                    Box(
                                        Modifier
                                            .size(if (small) 66.dp else if (picks.size == 3) 88.dp else 104.dp, if (small) 66.dp else 88.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Pal.Sun)
                                            .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) { RankText(p, if (small) 34.sp else 48.sp) }
                                    if (state.fixed) Tag("지정", Pal.Tomato, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).rotate(8f))
                                }
                            }
                        }
                    }
                    Text(
                        "${RandomRank.label(picks)}으로 들어오면", fontSize = 17.sp, color = Pal.Ink,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text("${shortBet(bet).ifEmpty { "벌칙 당첨" }}!", fontSize = 22.sp, color = Pal.TomatoText, textAlign = TextAlign.Center)
                }
            }
        }
        Text(
            if (state.fixed) "누가 이 등수로 들어올지는… 달려 봐야 알아요!" else if (state.revealed) "잠시 후 출발해요" else "화면을 탭하면 바로 멈춰요",
            fontSize = 15.sp, color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 170.dp),
        )
    }
}

/** 슬롯 한 칸. [value]가 null이면 숫자가 빠르게 돌고, 정해지면 그 등수에서 "딱!" 멈춘다. */
@Composable
private fun Reel(value: Int?, racers: Int, seed: Int, width: Dp) {
    var tick by remember { mutableIntStateOf(seed * 3) }
    LaunchedEffect(value) {
        while (value == null) { delay(65); tick++ }
    }
    val pop by androidx.compose.animation.core.animateFloatAsState(
        if (value != null) 1f else 0.82f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "pop",
    )
    val center = value ?: (tick % racers + 1)
    fun wrap(k: Int) = ((k - 1 + racers) % racers) + 1
    val above = if (value != null) (center - 1).takeIf { it >= 1 } else wrap(center - 1)
    val below = if (value != null) (center + 1).takeIf { it <= racers } else wrap(center + 1)
    Box(
        Modifier
            .size(width, 130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Pal.Paper)
            .border(3.dp, Pal.Ink, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Pal.Ink))
            Box(Modifier.fillMaxWidth().height(48.dp).background(if (value != null) Pal.Sun else Pal.SunPale))
            Box(Modifier.fillMaxWidth().height(3.dp).background(Pal.Ink))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) { above?.let { RankText(it, 22.sp, alpha = 0.35f) } }
            Box(Modifier.height(54.dp).graphicsLayer { scaleX = pop; scaleY = pop }, contentAlignment = Alignment.Center) {
                RankText(center, 38.sp, alpha = if (value != null) 1f else 0.75f)
            }
            Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) { below?.let { RankText(it, 22.sp, alpha = 0.35f) } }
        }
        if (value != null) {
            Tag("딱!", Pal.Tomato, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp))
        }
    }
}

/** "3등": 숫자는 크게(숫자 글꼴), "등"은 작게. */
@Composable
private fun RankText(rank: Int, size: androidx.compose.ui.unit.TextUnit, alpha: Float = 1f) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = NumberStyle.fontFamily, fontSize = size)) { append("$rank") }
            withStyle(SpanStyle(fontSize = size * 0.55f)) { append("등") }
        },
        color = Pal.Ink.copy(alpha = alpha), maxLines = 1,
    )
}
