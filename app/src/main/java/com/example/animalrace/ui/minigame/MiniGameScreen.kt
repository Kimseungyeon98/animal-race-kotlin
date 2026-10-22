package com.example.animalrace.ui.minigame

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.input.pointer.pointerInput
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
 * 몰아주기 미니게임 (시안: "몰아주기 미니게임").
 * 몰빵할 사람은 [session]에 이미 정해져 있고, 게임은 그걸 한 명씩 드러내는 연출이다.
 * 몰빵이 공개되면 [onDecided]로 기록하고, 잠시 뒤 최종 발표 카드를 띄운다.
 */
@Composable
fun MiniGameScreen(session: MiniGameSession, bet: String, onDecided: () -> Unit, onClose: () -> Unit, onAgain: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val revealed = remember(session) { mutableStateListOf<Int>() }
    var done by remember(session) { mutableStateOf(false) }
    val decided by rememberUpdatedState(onDecided)

    fun reveal(i: Int) {
        if (i in revealed) return
        revealed += i
        if (i == session.loser) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // 몰빵이 나오면 남은 사람은 모두 탈출
            session.players.indices.filter { it !in revealed }.forEach { revealed += it }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val loserOut = session.loser in revealed
    LaunchedEffect(loserOut) {
        if (!loserOut) return@LaunchedEffect
        decided()
        delay(1400)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                session.players.forEachIndexed { i, p ->
                    val out = i in revealed
                    val lose = out && i == session.loser
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
                        Text(p.animal.label, fontSize = 14.sp, color = if (lose) Color.White else Pal.Ink)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (session.game) {
                    MiniGame.LADDER -> LadderGame(session, revealed, ::reveal)
                    MiniGame.LOTTO -> LottoGame(session, revealed, ::reveal)
                }
            }
        }

        AnimatedVisibility(done, enter = fadeIn() + scaleIn(spring(dampingRatio = 0.55f), initialScale = 0.85f)) {
            FinalCard(session, bet, onClose, onAgain)
        }
    }
}

// ---------------------------------------------------------------- 사다리

/** 동물이 한 마리씩 사다리를 타고 내려가고, 도착한 칸만 열린다. 탭하면 빨리 내려간다. */
@Composable
private fun LadderGame(session: MiniGameSession, revealed: List<Int>, onReveal: (Int) -> Unit) {
    val n = session.players.size
    val ladder = session.ladder
    val progress = remember(session) { List(n) { mutableFloatStateOf(0f) } }
    var walking by remember(session) { mutableIntStateOf(-1) }
    var fast by remember(session) { mutableStateOf(false) }
    val reveal by rememberUpdatedState(onReveal)

    LaunchedEffect(session) {
        delay(800)
        for (i in 0 until n) {
            // 몰빵이 이미 나왔으면 남은 동물은 바로 도착
            if (session.loser in revealed) { progress[i].floatValue = 1f; continue }
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
            reveal(i)
            delay(600)
        }
        walking = -1
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fast = true },
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
            val paths = remember(session, w, h) {
                List(n) { i -> ladder.path(i).map { (c, l) -> colX(c) to levelY(l) } }
            }

            Canvas(Modifier.fillMaxSize()) {
                val stroke = 5.dp.toPx()
                for (c in 0 until n) {
                    drawLine(Pal.Ink, Offset(colX(c).toPx(), top.toPx()), Offset(colX(c).toPx(), bottom.toPx()), stroke, StrokeCap.Round)
                }
                ladder.rungs.forEach { r ->
                    val y = levelY(r.level).toPx()
                    drawLine(Pal.Ink, Offset(colX(r.left).toPx(), y), Offset(colX(r.left + 1).toPx(), y), stroke, StrokeCap.Round)
                }
                paths.forEachIndexed { i, pts ->
                    val t = progress[i].floatValue
                    if (t <= 0f) return@forEachIndexed
                    val color = if (i == session.loser && i in revealed) Pal.Tomato else TrailColors[i % TrailColors.size]
                    val px = pts.map { (x, y) -> Offset(x.toPx(), y.toPx()) }
                    val path = Path()
                    partial(px, t).forEachIndexed { k, p -> if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                    drawPath(path, color, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = 0.9f)
                }
            }

            // 아래 결과 칸: 도착한 동물이 있어야 열린다
            val loserSlot = ladder.end(session.loser)
            for (k in 0 until n) {
                val arrived = (0 until n).firstOrNull { ladder.end(it) == k && it in revealed }
                val open = arrived != null
                val lose = k == loserSlot
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
                val out = i in revealed
                val lose = out && i == session.loser
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
        }
        val who = session.players.getOrNull(walking)?.animal?.label
        Text(
            when {
                session.loser in revealed -> "${withSubjectUi(session.players[session.loser].animal.label)} 몰빵 칸에 도착! 💸"
                who != null -> "${withSubjectUi(who)} 내려가는 중… 화면을 탭하면 빨리"
                else -> "곧 출발해요!"
            },
            fontSize = 16.sp, color = Pal.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 꺾은선에서 처음부터 길이 비율 [t]만큼의 부분 */
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

/** 당첨자마다 복권 한 장. 자기 카드를 긁어 60% 넘게 벗기면 결과가 드러난다. */
@Composable
private fun LottoGame(session: MiniGameSession, revealed: List<Int>, onReveal: (Int) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        session.players.forEachIndexed { i, p ->
            val out = i in revealed
            val lose = i == session.loser
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.matchParentSize().offset(y = 5.dp).clip(RoundedCornerShape(20.dp)).background(Pal.Ink))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .graphicsLayer { alpha = if (out && !lose) 0.7f else 1f; rotationZ = if (out && lose) -1.5f else 0f }
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (out && lose) Pal.Pink else Pal.Paper)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp))
                        .padding(start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimalSprite(p.animal, 84.dp, fx = if (out && lose) Fx.SCARED else Fx.NONE)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LaneSticker(p.lane, size = 20.dp)
                            Text(p.animal.label, fontSize = 15.sp, color = Pal.Ink, maxLines = 1)
                        }
                    }
                    ScratchArea(
                        prize = if (lose) "몰빵! 💸" else "꽝 탈출 🎉",
                        lose = lose,
                        revealed = out,
                        modifier = Modifier.weight(1f).height(100.dp),
                    ) { onReveal(i) }
                }
                if (out) {
                    Tag(
                        if (lose) "몰빵!" else "탈출!", Pal.Tomato, fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-10).dp).rotate(8f),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (session.loser in revealed) "${withSubjectUi(session.players[session.loser].animal.label)} 몰빵! 💸"
            else "자기 동물 카드를 손가락으로 슥슥 긁어 주세요",
            fontSize = 16.sp, color = Pal.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val GX = 12
private const val GY = 5

/** 은박 긁기 칸. 손가락이 지나간 자리만 은박이 지워지고, 충분히 긁으면 [onScratched]. */
@Composable
private fun ScratchArea(prize: String, lose: Boolean, revealed: Boolean, modifier: Modifier, onScratched: () -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val cells = remember { BooleanArray(GX * GY) }
    val scratched by rememberUpdatedState(onScratched)
    val foil by animateFloatAsState(if (revealed) 0f else 1f, tween(450), label = "foil")
    val measurer = rememberTextMeasurer()

    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (lose) Pal.Pink else Pal.Mint)
            .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(prize, fontSize = 28.sp, color = if (lose) Pal.TomatoText else Color(0xFF1D8A3A), maxLines = 1, overflow = TextOverflow.Clip)
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = foil; compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(revealed) {
                    if (revealed) return@pointerInput
                    val radius = 17.dp.toPx()
                    fun mark(p: Offset) {
                        val cw = size.width / GX.toFloat()
                        val ch = size.height / GY.toFloat()
                        for (gy in 0 until GY) for (gx in 0 until GX) {
                            if (hypot(p.x - (gx + 0.5f) * cw, p.y - (gy + 0.5f) * ch) < radius + cw * 0.3f) cells[gy * GX + gx] = true
                        }
                        if (cells.count { it } >= cells.size * 0.6f) scratched()
                    }
                    detectDragGestures(
                        onDragStart = { strokes += listOf(it); mark(it) },
                        onDrag = { change, _ ->
                            val p = change.position
                            strokes[strokes.lastIndex] = strokes.last() + p
                            mark(p)
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
            val label = measurer.measure("여기를 긁어 주세요", TextStyle(fontFamily = Jua, fontSize = 17.sp, color = Color(0xFF6A7684)))
            drawText(label, topLeft = Offset((size.width - label.size.width) / 2, (size.height - label.size.height) / 2))
            // 긁은 자리 지우기
            val w = 34.dp.toPx()
            strokes.forEach { s ->
                if (s.size == 1) drawCircle(Color.Black, w / 2, s[0], blendMode = BlendMode.Clear)
                else {
                    val path = Path().apply { moveTo(s[0].x, s[0].y); s.drop(1).forEach { lineTo(it.x, it.y) } }
                    drawPath(path, Color.Black, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 최종 발표

@Composable
private fun FinalCard(session: MiniGameSession, bet: String, onClose: () -> Unit, onAgain: () -> Unit) {
    val loser = session.players[session.loser]
    val escaped = session.players.filterIndexed { i, _ -> i != session.loser }.joinToString("·") { it.animal.label }
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
                    Text("${withRo(loser.animal.label)} 출전한 사람!", fontSize = 20.sp, color = Pal.Ink)
                }
                Text("${shortBet(bet).ifEmpty { "벌칙" }} 몰빵!", fontSize = 26.sp, color = Pal.TomatoText, textAlign = TextAlign.Center)
                Text("${session.game.emoji} ${session.game.title} · $escaped 탈출", fontSize = 14.sp, color = Pal.InkSoft)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CartoonButton(onClick = onClose, color = Pal.Paper, modifier = Modifier.weight(1f), height = 52.dp, depth = 4.dp) {
                        Text("결과로", fontSize = 18.sp, color = Pal.Ink)
                    }
                    CartoonButton(onClick = onAgain, color = Pal.Sun, modifier = Modifier.weight(1.3f), height = 52.dp, depth = 4.dp) {
                        Text("한 판 더", fontSize = 18.sp, color = Pal.Ink)
                    }
                }
            }
        }
    }
}
