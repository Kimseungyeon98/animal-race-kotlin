package com.example.animalrace.ui.minigame

import androidx.compose.animation.AnimatedVisibility
import kotlin.math.roundToInt
import kotlin.math.abs
import com.example.animalrace.ui.theme.LaneCloth
import com.example.animalrace.model.Rung
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.animalrace.model.RaceResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.animalrace.ui.sound.LocalSound
import com.example.animalrace.ui.sound.Sfx
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.MiniGameSession
import com.example.animalrace.model.Fx
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameDraw
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.CartoonButton
import com.example.animalrace.ui.components.LaneSticker
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.components.withRo
import com.example.animalrace.ui.theme.Jua
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.delay
import kotlin.math.hypot

/** 사다리 길 색 (출발 순서대로). 몰빵이 드러나면 그 길만 빨강. */
private val TrailColors = listOf(Color(0xFF3FD0A6), Color(0xFF5AA9FF), Color(0xFFB983FF))

/**
 * 몰아주기 미니게임 (시안: "몰아주기 미니게임", "복권 긁기 v2").
 * 결과는 [session]에 이미 숨겨져 있고(사다리: 몰빵할 사람, 복권: 몰빵 복권 자리), 게임은 그걸 한 명씩 드러내는 연출이다.
 * 몰빵이 공개되면 [onDecided]로 기록하고, 잠시 뒤 최종 발표 카드를 띄운다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiniGameScreen(session: MiniGameSession, bet: String, onDecided: (Int) -> Unit, onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val revealed = remember(session) { mutableStateListOf<Int>() }
    var loser by remember(session) { mutableStateOf<Int?>(null) }
    var done by remember(session) { mutableStateOf(false) }
    val decided by rememberUpdatedState(onDecided)

    /** [i]번 사람의 결과 공개. 몰빵이면 남은 사람은 모두 탈출. */
    fun reveal(i: Int, lose: Boolean) {
        if (i in revealed) return
        revealed += i
        if (lose) {
            loser = i
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            session.players.indices.filter { it !in revealed }.forEach { revealed += it }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            sound.play(Sfx.SAFE)
        }
    }
    LaunchedEffect(loser) {
        val l = loser ?: return@LaunchedEffect
        decided(l)
        // 게임마다의 콱!/펑!/팡! 뒤에 몰빵 뿌우우
        delay(350)
        sound.play(Sfx.BUST)
        delay(1050)
        done = true
    }

    Box(Modifier.fillMaxSize().background(Pal.Sky)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedText(session.game.title, 28.sp, modifier = Modifier.weight(1f))
                Tag(shortBet(bet).ifEmpty { "몰아주기" }, Pal.Cream, Pal.Ink, fontSize = 13.sp)
            }
            // 인원이 많으면 두 줄로 (10명까지)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                session.players.forEachIndexed { i, p ->
                    val out = i in revealed
                    val lose = out && i == loser
                    Row(
                        Modifier
                            .graphicsLayer { alpha = if (out && !lose) 0.6f else 1f }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (lose) Pal.Tomato else if (out) Pal.Mint else Pal.Pink)
                            .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
                            .padding(start = 3.dp, end = 8.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LaneSticker(p.lane, size = 18.dp)
                        Text(p.name, fontSize = 14.sp, color = if (lose) Color.White else Pal.Ink)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (session.game) {
                    MiniGame.LADDER -> LadderGame(session, revealed, loser, ::reveal)
                    MiniGame.LOTTO -> LottoGame(session, revealed, loser, ::reveal)
                    MiniGame.GACHA -> GachaGame(session, loser, ::reveal)
                }
            }
        }

        AnimatedVisibility(done, enter = fadeIn() + scaleIn(spring(dampingRatio = 0.55f), initialScale = 0.85f)) {
            loser?.let { FinalCard(session, it, bet, onClose) }
        }
    }
}

// ---------------------------------------------------------------- 사다리

/**
 * 사다리 타기 v2 (시안: "사다리 타기 v2").
 * 무작위 사다리 위에 출전 번호 순으로 한 사람당 가로줄 1개(넘기기 가능)를 긋고, [출발!]하면
 * 동물이 한 마리씩 타고 내려간다. 몰빵 칸([MiniGameSession.bomb])은 끝까지 가려져 있다가 도착하면 열린다.
 */
@Composable
private fun LadderGame(session: MiniGameSession, revealed: List<Int>, loser: Int?, onReveal: (Int, Boolean) -> Unit) {
    val n = session.players.size
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val order = remember(session) { session.players.indices.sortedBy { session.players[it].lane } }
    // 사람들이 그은 가로줄과 그은 사람
    val added = remember(session) { mutableStateListOf<Pair<Rung, Int>>() }
    var drawTurn by remember(session) { mutableIntStateOf(0) }
    var walkStarted by remember(session) { mutableStateOf(false) }
    var toast by remember(session) { mutableStateOf<String?>(null) }
    val ladder = remember(session, added.size) { added.fold(session.ladder) { l, (r, _) -> l + r } }
    val drawing = drawTurn < n
    val drawer = order.getOrNull(drawTurn)

    val progress = remember(session) { List(n) { mutableFloatStateOf(0f) } }
    var walking by remember(session) { mutableIntStateOf(-1) }
    var fast by remember(session) { mutableStateOf(false) }
    val reveal by rememberUpdatedState(onReveal)
    val current by rememberUpdatedState(ladder)

    LaunchedEffect(walkStarted) {
        if (!walkStarted) return@LaunchedEffect
        delay(500)
        for (i in 0 until n) {
            // 몰빵이 이미 나왔으면 남은 동물은 바로 도착
            if (revealed.size == n) { progress[i].floatValue = 1f; continue }
            walking = i
            fast = false
            // 마지막 한 마리 전에는 잠깐 멈칫
            if (i == n - 1) delay(700)
            var last = withFrameNanos { it }
            while (progress[i].floatValue < 1f) {
                val now = withFrameNanos { it }
                val dt = (now - last) / 1e9f
                last = now
                progress[i].floatValue = (progress[i].floatValue + dt / (if (fast) 0.6f else 2.8f)).coerceAtMost(1f)
            }
            reveal(i, current.end(i) == session.bomb)
            delay(600)
        }
        walking = -1
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(1300); toast = null }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp)),
        ) {
            val w = maxWidth
            val h = maxHeight
            val sprite = 64.dp
            val top = sprite + 22.dp
            val bottom = h - 74.dp
            fun colX(c: Int): Dp = w * ((c + 0.5f) / n)
            fun levelY(l: Int): Dp = when {
                l < 0 -> top
                l >= ladder.levels -> bottom
                else -> top + (bottom - top) * ((l + 1f) / (ladder.levels + 1))
            }
            val paths = remember(ladder, w, h) {
                List(n) { i -> ladder.path(i).map { (c, l) -> colX(c) to levelY(l) } }
            }
            val density = LocalDensity.current
            // 그을 수 있는 자리 (같은 높이에 줄이 없는 곳): 점선 + ⊕로 미리 보여 준다
            val spots = remember(ladder, drawing) {
                if (!drawing) emptyList()
                else (0 until ladder.levels).flatMap { l -> (0 until n - 1).map { c -> Rung(l, c) } }.filter { ladder.canAdd(it) }
            }
            // 내려가던 동물이 가로줄로 꺾일 때마다 통!
            if (walking >= 0) {
                val turns = turnsPassed(paths[walking], progress[walking].floatValue)
                LaunchedEffect(walking, turns) { if (turns > 0) sound.play(Sfx.PLUCK) }
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(drawing, drawTurn) {
                        detectTapGestures { p ->
                            if (!drawing) { fast = true; return@detectTapGestures }
                            val who = drawer ?: return@detectTapGestures
                            with(density) {
                                // 탭한 곳에서 가장 가까운 점선 (손가락 반경 안에서만)
                                val r = spots.minByOrNull { sp ->
                                    val cx = ((colX(sp.left) + colX(sp.left + 1)) / 2).toPx()
                                    val dx = (abs(p.x - cx) - ((colX(1) - colX(0)) / 2).toPx()).coerceAtLeast(0f)
                                    val dy = p.y - levelY(sp.level).toPx()
                                    dx * dx + dy * dy
                                }?.takeIf { sp ->
                                    val cx = ((colX(sp.left) + colX(sp.left + 1)) / 2).toPx()
                                    abs(p.x - cx) < ((colX(1) - colX(0)) / 2).toPx() && abs(p.y - levelY(sp.level).toPx()) < 26.dp.toPx()
                                }
                                if (r != null) {
                                    added += r to who
                                    drawTurn++
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sound.play(Sfx.CLICK)
                                } else {
                                    toast = "점선 위를 탭해 주세요"
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    },
            ) {
                val stroke = 5.dp.toPx()
                // 그을 수 있는 자리: 흐린 점선 + 가운데 ⊕ (시안: "화면 다듬기" ①)
                val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 8.dp.toPx()))
                spots.forEach { sp ->
                    val y = levelY(sp.level).toPx()
                    val x1 = colX(sp.left).toPx() + 8.dp.toPx()
                    val x2 = colX(sp.left + 1).toPx() - 8.dp.toPx()
                    drawLine(Pal.Ink.copy(alpha = 0.38f), Offset(x1, y), Offset(x2, y), 4.dp.toPx(), StrokeCap.Round, dash)
                    val c = Offset((x1 + x2) / 2, y)
                    val r = 11.dp.toPx()
                    drawCircle(Pal.SunPale, r, c)
                    drawCircle(Pal.Ink, r, c, style = Stroke(2.5.dp.toPx()))
                    drawLine(Pal.Ink, c - Offset(5.dp.toPx(), 0f), c + Offset(5.dp.toPx(), 0f), 2.5.dp.toPx(), StrokeCap.Round)
                    drawLine(Pal.Ink, c - Offset(0f, 5.dp.toPx()), c + Offset(0f, 5.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
                }
                for (c in 0 until n) {
                    drawLine(Pal.Ink, Offset(colX(c).toPx(), top.toPx()), Offset(colX(c).toPx(), bottom.toPx()), stroke, StrokeCap.Round)
                }
                session.ladder.rungs.forEach { r ->
                    val y = levelY(r.level).toPx()
                    drawLine(Pal.Ink, Offset(colX(r.left).toPx(), y), Offset(colX(r.left + 1).toPx(), y), stroke, StrokeCap.Round)
                }
                // 사람들이 그은 줄: 출전 번호 색 + 테두리
                added.forEach { (r, who) ->
                    val y = levelY(r.level).toPx()
                    val x1 = colX(r.left).toPx() + 3.dp.toPx()
                    val x2 = colX(r.left + 1).toPx() - 3.dp.toPx()
                    drawLine(Pal.Ink, Offset(x1, y), Offset(x2, y), 10.dp.toPx(), StrokeCap.Round)
                    drawLine(LaneCloth[session.players[who].lane % LaneCloth.size].first, Offset(x1, y), Offset(x2, y), 5.dp.toPx(), StrokeCap.Round)
                }
                paths.forEachIndexed { i, pts ->
                    val t = progress[i].floatValue
                    if (t <= 0f) return@forEachIndexed
                    val color = if (i == loser) Pal.Tomato else TrailColors[i % TrailColors.size]
                    val px = pts.map { (x, y) -> Offset(x.toPx(), y.toPx()) }
                    val path = Path()
                    partial(px, t).forEachIndexed { k, p -> if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                    drawPath(path, color, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = 0.9f)
                }
            }

            // 그은 줄의 번호 핀
            added.forEach { (r, who) ->
                LaneSticker(
                    session.players[who].lane,
                    Modifier.offset(x = (colX(r.left) + colX(r.left + 1)) / 2 - 10.dp, y = levelY(r.level) - 22.dp),
                    size = 20.dp,
                )
            }

            // 아래 결과 칸: 도착한 동물이 있어야 열린다
            for (k in 0 until n) {
                val open = (0 until n).any { ladder.end(it) == k && it in revealed && progress[it].floatValue >= 1f }
                val lose = k == session.bomb
                val slotW = (w / n - 14.dp).coerceAtMost(104.dp)
                Box(
                    Modifier
                        .offset(x = colX(k) - slotW / 2, y = bottom + 12.dp)
                        .rotate(if (open && lose) -4f else 0f)
                        .size(slotW, 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!open) Pal.Silver else if (lose) Pal.Tomato else Pal.Mint)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (!open) "?" else if (lose) "💸 몰빵" else "🎉 통과",
                        fontSize = if (open) 17.sp else 24.sp, color = if (open && lose) Color.White else Pal.Ink, maxLines = 1,
                    )
                }
            }

            // 동물: 출발 전엔 위에, 내려가는 중엔 길 위에, 도착하면 칸 위에
            session.players.forEachIndexed { i, p ->
                val pts = paths[i]
                val t = progress[i].floatValue
                val pos = pointAt(pts, t)
                val out = i in revealed && t >= 1f
                val lose = out && i == loser
                AnimalSprite(
                    p.animal, sprite,
                    running = i == walking && t < 1f,
                    fx = if (lose) Fx.SCARED else Fx.NONE,
                    modifier = Modifier
                        .offset(x = pos.first - sprite / 2, y = pos.second - sprite * 0.8f)
                        .graphicsLayer { alpha = if (out && !lose) 0.55f else 1f },
                )
                if (t == 0f) {
                    LaneSticker(p.lane, Modifier.offset(x = pos.first - sprite / 2 - 2.dp, y = 6.dp), size = 20.dp)
                }
            }

            toast?.let {
                Tag(it, Pal.Paper, Pal.Ink, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            }
        }

        // 아래: 줄 긋기 차례 → [출발!] → 진행 안내
        when {
            drawing && drawer != null -> {
                val p = session.players[drawer]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Pal.Ivory, RoundedCornerShape(20.dp)) // 깜짝! 말풍선이 위로 삐져나와도 잘리지 않게 clip 없이
                        .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp))
                        .padding(start = 6.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimalSprite(p.animal, 56.dp)
                    Column(Modifier.weight(1f)) {
                        Text("${p.name} 차례!", fontSize = 19.sp, color = Pal.Ink)
                        Text("점선을 탭해서 가로줄 1개", fontSize = 12.sp, color = Pal.InkSoft, maxLines = 1)
                    }
                    CartoonButton(onClick = { drawTurn++; sound.play(Sfx.CLICK) }, color = Pal.Paper, height = 42.dp, depth = 3.dp, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("넘기기", fontSize = 15.sp, color = Pal.Ink)
                    }
                }
            }
            !walkStarted -> CartoonButton(onClick = { walkStarted = true; sound.play(Sfx.CLICK) }, color = Pal.Sun, modifier = Modifier.fillMaxWidth(), height = 62.dp, depth = 5.dp) {
                Text("🪜 출발!", fontSize = 24.sp, color = Pal.Ink)
            }
            else -> {
                val who = session.players.getOrNull(walking)?.name
                Text(
                    when {
                        loser != null -> "${withSubjectUi(session.players[loser].name)} 몰빵 칸에 도착! 💸"
                        who != null -> "${withSubjectUi(who)} 내려가는 중… 화면을 탭하면 빨리"
                        else -> "곧 출발해요!"
                    },
                    fontSize = 16.sp, color = Pal.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(62.dp).padding(top = 18.dp),
                )
            }
        }
    }
}

/** 꺾은선에서 처음부터 길이 비율 [t]만큼의 부분 */
/** 길이 비율 [t]까지 가는 동안 들어선 가로 구간 수 */
private fun turnsPassed(pts: List<Pair<Dp, Dp>>, t: Float): Int {
    val segs = pts.zipWithNext()
    val lens = segs.map { (a, b) -> abs((b.first - a.first).value) + abs((b.second - a.second).value) }
    var at = lens.sum() * t
    var turns = 0
    for ((k, s) in segs.withIndex()) {
        if (at <= 0f) break
        if (s.first.first != s.second.first) turns++
        at -= lens[k]
    }
    return turns
}

private fun partial(pts: List<Offset>, t: Float): List<Offset> {
    val total = pts.zipWithNext { a, b -> (b - a).getDistance() }.sum()
    var left = total * t
    val out = mutableListOf(pts.first())
    for (k in 1 until pts.size) {
        val seg = (pts[k] - pts[k - 1]).getDistance()
        if (left >= seg) { out += pts[k]; left -= seg } else {
            if (seg > 0f) out += pts[k - 1] + (pts[k] - pts[k - 1]) * (left / seg)
            break
        }
    }
    return out
}

/** Dp 꺾은선 위에서 길이 비율 [t]인 점 */
private fun pointAt(pts: List<Pair<Dp, Dp>>, t: Float): Pair<Dp, Dp> {
    val p = partial(pts.map { (x, y) -> Offset(x.value, y.value) }, t).last()
    return p.x.dp to p.y.dp
}

private fun withSubjectUi(word: String) = com.example.animalrace.race.withSubject(word)

// ---------------------------------------------------------------- 복권

/**
 * 복권 긁기 v2 (시안: "복권 긁기 v2", 4.7 "3바퀴 + 4칸 격자").
 * 똑같이 생긴 복권을 사람 수 × 바퀴 수만큼 놓고, 몰빵이 나올 때까지 출전 번호 순으로 돌아가며 한 장씩 고른다.
 * 고른 복권은 화면 가득 펼쳐서 긁고(공개 조건은 [ScratchArea]), 꽝이면 다음 사람이 남은 장에서 고른다.
 * 몰빵은 [MiniGameSession.roundBomb] 자리의 복권에 숨어 있다.
 */
@Composable
private fun LottoGame(session: MiniGameSession, revealed: List<Int>, loser: Int?, onReveal: (Int, Boolean) -> Unit) {
    val sound = LocalSound.current
    val n = session.players.size
    // 복권은 사람 수 × 바퀴 수(4명까지 3바퀴, 5명부터 2바퀴): 몰빵이 나올 때까지 출전 번호 순으로 돌아가며 고른다
    // (누구나 최대 같은 장수라 걸릴 확률은 모두 1/n)
    val rounds = MiniGameDraw.rounds(n)
    val tickets = MiniGameDraw.slots(n)
    val bomb = session.roundBomb
    val order = remember(session) { session.players.indices.sortedBy { session.players[it].lane } }
    // 복권 자리마다 고른 사람 (아직 안 골랐으면 null)
    val owner = remember(session) { mutableStateListOf<Int?>().apply { repeat(tickets) { add(null) } } }
    // 긁은 복권 자리. 사람은 자기 복권을 다 긁어야(2장) 탈출로 표시된다
    val scratched = remember(session) { mutableStateListOf<Int>() }
    var turn by remember(session) { mutableIntStateOf(0) }
    var open by remember(session) { mutableStateOf<Int?>(null) }
    var shown by remember(session) { mutableStateOf(0) }
    val current = order.getOrNull(turn % n)?.takeIf { loser == null }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // 지금 차례
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Pal.Ivory, RoundedCornerShape(20.dp)) // 깜짝! 말풍선이 위로 삐져나와도 잘리지 않게 clip 없이
                    .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val who = session.players[loser ?: current ?: order.last()]
                AnimalSprite(who.animal, 60.dp, fx = if (loser != null) Fx.SCARED else Fx.NONE)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (loser != null) "${withSubjectUi(who.name)} 몰빵! 💸" else "${who.name} 차례!", fontSize = 20.sp, color = Pal.Ink)
                    val left = owner.count { it == null }
                    val round = if (loser != null || turn < n) "" else if (turn < n * 2) "두 번째 바퀴 · " else "세 번째 바퀴 · "
                    Text(
                        if (loser != null) "잠시 뒤 최종 발표" else if (left == tickets) "똑같이 생긴 복권 ${tickets}장 중 한 장을 골라 주세요" else "${round}남은 복권 ${left}장 중 한 장을 골라 주세요",
                        fontSize = 13.sp, color = Pal.InkSoft,
                    )
                }
            }
            // 똑같이 생긴 복권들: 두 칸 격자, 출전 번호 순 (시안: "화면 다듬기" ②)
            // 2~3장은 크게, 4장 이상은 남은 높이에 맞춰 납작하게
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                // 6·8·10장은 두 칸, 9·12장은 세 칸 (마지막 줄에 한 장만 남지 않게), 그보다 많으면 네 칸에 긁기 칸만 있는 작은 복권
                val cols = when {
                    tickets == 1 -> 1
                    tickets % 3 == 0 && tickets in 9..12 -> 3
                    tickets <= 10 -> 2
                    else -> 4
                }
                val rows = (tickets + cols - 1) / cols
                val gap = 14.dp
                val footer = 44.dp
                val cardW = ((maxWidth - gap * (cols - 1)) / cols).coerceAtMost(175.dp)
                val tallH = cardW / 0.76f
                val cardH = minOf(tallH, (maxHeight - footer - gap * (rows - 1)) / rows)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
                    (0 until tickets).chunked(cols).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            line.forEach { t ->
                                val who = owner[t]
                                TicketFace(
                                    used = who?.let { session.players[it] },
                                    lose = who != null && who == loser,
                                    compact = cardH < tallH * 0.85f || cols >= 3,
                                    narrow = cols == 3,
                                    mini = cols == 4,
                                    modifier = Modifier
                                        .size(cardW, cardH)
                                        .rotate(if (tickets <= 3) (if (t % 2 == 0) -1.5f else 1.5f) else 0f),
                                    onClick = if (who == null && current != null && open == null) {
                                        { owner[t] = current; shown = t; open = t }
                                    } else null,
                                )
                            }
                        }
                    }
                    Text(
                        if (loser != null) "${withSubjectUi(session.players[loser].name)} 몰빵! 💸"
                        else if (owner.count { it == null } == 1) "마지막 한 장… 직접 긁어서 확인!"
                        else "어느 장에 몰빵이 숨어 있을까…",
                        fontSize = 16.sp, color = Pal.Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // 고른 복권을 화면 가득
        AnimatedVisibility(open != null, enter = fadeIn() + scaleIn(spring(dampingRatio = 0.7f), initialScale = 0.6f), exit = fadeOut()) {
            val t = shown
            val p = owner[t] ?: return@AnimatedVisibility
            val lose = t == bomb
            val out = t in scratched
            val next = order.getOrNull((turn + 1) % n)
            BigTicket(
                player = session.players[p],
                lose = lose,
                revealed = out,
                onScratched = {
                    sound.play(Sfx.POP)
                    if (t !in scratched) scratched += t
                    // 몰빵이면 바로, 꽝이면 자기 복권을 다 긁었을 때만 탈출
                    if (lose || owner.count { it == p } >= rounds) onReveal(p, lose) else sound.play(Sfx.SAFE)
                },
                nextLabel = if (out && !lose && loser == null && next != null) "${session.players[next].name} 차례로 →" else null,
                onNext = { open = null; turn++ },
            )
        }
    }
}

/** 고르기 화면의 복권 한 장. 아직 안 고른 장은 모두 똑같이 생겼고, 긁은 장은 결과와 주인이 남는다. */
@Composable
private fun TicketFace(used: RaceResult?, lose: Boolean, modifier: Modifier, onClick: (() -> Unit)?, compact: Boolean = false, narrow: Boolean = false, mini: Boolean = false) {
    Box(modifier.graphicsLayer { alpha = if (used != null && !lose) 0.55f else 1f }) {
        Box(Modifier.matchParentSize().offset(y = 4.dp).clip(RoundedCornerShape(16.dp)).background(Pal.Ink))
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Pal.Cream)
                .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(if (mini) 5.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // 네 칸 작은 복권은 긁기 칸만
            if (!mini) Text(if (narrow) "🎫 복권" else "🎫 몰아주기 복권", fontSize = if (compact) 13.sp else 15.sp, color = Pal.Ink, maxLines = 1)
            if (!compact) {
                Text("긁어서 확인하세요", fontSize = 12.sp, color = Pal.InkSoft, maxLines = 1)
                Text("★ ★ ★", fontSize = 14.sp, color = Pal.TomatoText)
            }
            if (!mini) DashedLine()
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (used == null) Pal.Silver else if (lose) Pal.Pink else Pal.Mint)
                    .border(2.dp, Pal.Ink, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (used == null) {
                    Canvas(Modifier.fillMaxSize()) {
                        rotate(35f) {
                            var x = -size.height * 2
                            while (x < size.width * 2) {
                                drawRect(Color(0xFFDDE3EA), Offset(x, -size.height), Size(4.dp.toPx(), size.height * 3))
                                x += 10.dp.toPx()
                            }
                        }
                    }
                    Text("? ? ?", fontSize = if (mini) 15.sp else 20.sp, color = Color(0xFF6A7684))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (lose) "몰빵!" else if (mini) "꽝" else "꽝 탈출", fontSize = if (mini) 16.sp else 20.sp, color = if (lose) Pal.TomatoText else Color(0xFF1D8A3A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            LaneSticker(used.lane, size = 16.dp)
                            Text(used.name, fontSize = 12.sp, color = Pal.Ink, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** 복권 가운데 점선 (뜯는 선) */
@Composable
private fun DashedLine() {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            Pal.Ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
        )
    }
}

/** 화면 가득 펼친 복권: 누구 복권인지 + 큰 긁기 칸. 꽝이면 아래에 [다음 차례로] 버튼. */
@Composable
private fun BigTicket(player: RaceResult, lose: Boolean, revealed: Boolean, onScratched: () -> Unit, nextLabel: String?, onNext: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.5f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.fillMaxSize().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().rotate(if (revealed && lose) -1.5f else 0f)) {
                Box(Modifier.matchParentSize().offset(y = 6.dp).clip(RoundedCornerShape(26.dp)).background(Pal.Ink))
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (revealed && lose) Pal.Pink else Pal.Cream)
                        .border(4.dp, Pal.Ink, RoundedCornerShape(26.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("🎫 몰아주기 복권", fontSize = 26.sp, color = Pal.Ink)
                    Text("꽝이면 탈출, 몰빵이면 벌칙!", fontSize = 15.sp, color = Pal.InkSoft)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimalSprite(player.animal, 60.dp, fx = if (revealed && lose) Fx.SCARED else Fx.NONE)
                        LaneSticker(player.lane, size = 24.dp)
                        Text("${player.name}의 복권", fontSize = 20.sp, color = Pal.Ink)
                    }
                    Text("★ ★ ★ ★ ★", fontSize = 18.sp, color = Pal.TomatoText)
                    DashedLine()
                    Spacer(Modifier.height(4.dp))
                    key(player.lane) {
                        ScratchArea(
                            prize = if (lose) "몰빵! 💸" else "꽝 탈출 🎉",
                            lose = lose,
                            revealed = revealed,
                            fontSize = 52.sp,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            onScratched = onScratched,
                        )
                    }
                }
                if (revealed) {
                    Tag(
                        if (lose) "몰빵!" else "탈출!", Pal.Tomato, fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = 6.dp, y = (-40).dp).rotate(10f),
                    )
                }
            }
            if (nextLabel != null) {
                CartoonButton(onClick = onNext, color = Pal.Sun, modifier = Modifier.fillMaxWidth(), height = 54.dp, depth = 4.dp) {
                    Text(nextLabel, fontSize = 19.sp, color = Pal.Ink)
                }
            } else {
                Text(
                    if (revealed) "" else "손가락으로 슥슥 긁어 주세요",
                    fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 14.dp),
                )
            }
        }
    }
}

private const val GX = 14
private const val GY = 6
private const val TX = 12
private const val TY = 3

/** 긁을 때 튀는 은박 가루 하나 (위치 px, 속도 px/초, 생긴 시각 초). */
private class Flake(val x: Float, val y: Float, val vx: Float, val vy: Float, val born: Float, val size: Float, val spin: Float)

private fun clock() = System.nanoTime() / 1e9f

/**
 * 은박 긁기 칸. 손가락이 지나간 자리만 은박이 지워지고 은박 가루가 튀며, 긁는 동안 진동이 톡톡 온다.
 * 전체를 90% 넘게 긁거나 글자 자리를 빠짐없이 긁으면 [onScratched] — 글자 주변만 긁어 두고 애태우다가
 * 마지막에 글자를 직접 확인할 수 있게. 결과 색(초록/빨강)은 다 드러난 뒤에만 입힌다.
 */
@Composable
private fun ScratchArea(
    prize: String,
    lose: Boolean,
    revealed: Boolean,
    modifier: Modifier,
    fontSize: TextUnit = 36.sp,
    onScratched: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val cells = remember { BooleanArray(GX * GY) }
    // 결과 글자가 놓인 자리 (긁는 칸 기준 좌표)와 그 안의 확인 점들
    val textBox = remember { arrayOf(Rect.Zero) }
    val textHits = remember { BooleanArray(TX * TY) }
    val flakes = remember { mutableStateListOf<Flake>() }
    var now by remember { mutableFloatStateOf(clock()) }
    val scratched by rememberUpdatedState(onScratched)
    val foil by animateFloatAsState(if (revealed) 0f else 1f, tween(450), label = "foil")
    val bg by animateColorAsState(if (!revealed) Pal.Cream else if (lose) Pal.Pink else Pal.Mint, tween(350), label = "bg")
    val ink by animateColorAsState(if (!revealed) Pal.Ink else if (lose) Pal.TomatoText else Color(0xFF1D8A3A), tween(350), label = "ink")
    val measurer = rememberTextMeasurer()

    // 가루가 남아 있는 동안만 프레임마다 시간을 흘린다
    val busy = flakes.isNotEmpty()
    LaunchedEffect(busy) {
        while (busy) {
            withFrameNanos { }
            now = clock()
            flakes.removeAll { now - it.born > 0.7f }
            if (flakes.isEmpty()) break
        }
    }

    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            prize, fontSize = fontSize, color = ink, maxLines = 1, overflow = TextOverflow.Clip,
            modifier = Modifier.onGloballyPositioned { textBox[0] = it.boundsInParent() },
        )
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = foil; compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(revealed) {
                    if (revealed) return@pointerInput
                    val radius = 18.dp.toPx()
                    val tick = 44.dp.toPx()
                    var travel = 0f
                    var last = Offset.Zero
                    fun mark(p: Offset) {
                        val cw = size.width / GX.toFloat()
                        val ch = size.height / GY.toFloat()
                        for (gy in 0 until GY) for (gx in 0 until GX) {
                            if (hypot(p.x - (gx + 0.5f) * cw, p.y - (gy + 0.5f) * ch) < radius) cells[gy * GX + gx] = true
                        }
                        // 글자 칸은 위아래 여백을 빼고 실제 글자 높이 근처만
                        val r = textBox[0]
                        if (r.width > 0f) {
                            val top = r.top + r.height * 0.22f
                            val h = r.height * 0.56f
                            for (ty in 0 until TY) for (tx in 0 until TX) {
                                val sx = r.left + r.width * (tx + 0.5f) / TX
                                val sy = top + h * (ty + 0.5f) / TY
                                if (hypot(p.x - sx, p.y - sy) < radius) textHits[ty * TX + tx] = true
                            }
                        }
                        if (cells.count { it } >= cells.size * 0.9f || (r.width > 0f && textHits.all { it })) scratched()
                    }
                    fun dust(p: Offset, dir: Offset) {
                        val t = clock()
                        repeat(2) {
                            flakes += Flake(
                                p.x, p.y,
                                vx = -dir.x * 3f + (Math.random().toFloat() - 0.5f) * 220f,
                                vy = -120f - Math.random().toFloat() * 160f,
                                born = t, size = 3f + Math.random().toFloat() * 4f, spin = Math.random().toFloat() * 360f,
                            )
                        }
                        now = t
                    }
                    detectDragGestures(
                        onDragStart = { strokes += listOf(it); last = it; mark(it) },
                        onDrag = { change, amount ->
                            val p = change.position
                            strokes[strokes.lastIndex] = strokes.last() + p
                            mark(p)
                            travel += (p - last).getDistance()
                            last = p
                            if (travel > tick) {
                                travel = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sound.play(Sfx.SCRATCH)
                            }
                            if (flakes.size < 40) dust(p, amount)
                        },
                    )
                },
        ) {
            // 은박: 회색 바탕에 사선 줄무늬 + 안내 글자
            drawRect(Pal.Silver)
            rotate(35f) {
                var x = -size.height * 2
                while (x < size.width * 2) {
                    drawRect(Color(0xFFDDE3EA), Offset(x, -size.height), Size(8.dp.toPx(), size.height * 3))
                    x += 20.dp.toPx()
                }
            }
            val label = measurer.measure("여기를 긁어 주세요", TextStyle(fontFamily = Jua, fontSize = 20.sp, color = Color(0xFF6A7684)))
            drawText(label, topLeft = Offset((size.width - label.size.width) / 2, (size.height - label.size.height) / 2))
            // 긁은 자리 지우기
            val w = 36.dp.toPx()
            strokes.forEach { s ->
                if (s.size == 1) drawCircle(Color.Black, w / 2, s[0], blendMode = BlendMode.Clear)
                else {
                    val path = Path().apply { moveTo(s[0].x, s[0].y); s.drop(1).forEach { lineTo(it.x, it.y) } }
                    drawPath(path, Color.Black, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
                }
            }
        }
        // 은박 가루: 손가락 자리에서 튀어 올랐다 떨어지며 사라짐
        Canvas(Modifier.fillMaxSize()) {
            val d = density
            flakes.forEach { f ->
                val t = (now - f.born).coerceAtLeast(0f)
                val x = f.x + f.vx * t
                val y = f.y + f.vy * t + 900f * t * t
                val a = (1f - t / 0.7f).coerceIn(0f, 1f)
                rotate(f.spin + t * 540f, Offset(x, y)) {
                    drawRect(Color(0xFFB9C2CC), Offset(x - f.size * d / 2, y - f.size * d / 2), Size(f.size * d, f.size * d * 0.7f), alpha = a)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 최종 발표

@Composable
private fun FinalCard(session: MiniGameSession, loserIndex: Int, bet: String, onClose: () -> Unit) {
    val loser = session.players[loserIndex]
    val escaped = session.players.filterIndexed { i, _ -> i != loserIndex }.joinToString("·") { it.name }
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(horizontal = 20.dp)) {
            Box(Modifier.matchParentSize().offset(y = 6.dp).clip(RoundedCornerShape(26.dp)).background(Pal.Ink))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Pal.Pink)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(26.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Tag("💸 몰빵 당첨!", Pal.Tomato, fontSize = 16.sp)
                AnimalSprite(loser.animal, 150.dp, fx = Fx.SCARED, modifier = Modifier.padding(top = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LaneSticker(loser.lane, size = 26.dp, border = 3.dp)
                    Text(loser.who ?: "${withRo(loser.name)} 출전한 사람!", fontSize = 20.sp, color = Pal.Ink)
                }
                Text("${shortBet(bet).ifEmpty { "벌칙" }} 몰빵!", fontSize = 26.sp, color = Pal.TomatoText, textAlign = TextAlign.Center)
                Text("${session.game.emoji} ${session.game.title} · $escaped 탈출", fontSize = 14.sp, color = Pal.InkSoft)
                Spacer(Modifier.height(6.dp))
                CartoonButton(onClick = onClose, color = Pal.Sun, modifier = Modifier.fillMaxWidth(), height = 52.dp, depth = 4.dp) {
                    Text("결과로", fontSize = 18.sp, color = Pal.Ink)
                }
            }
        }
    }
}
