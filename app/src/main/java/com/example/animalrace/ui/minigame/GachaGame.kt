package com.example.animalrace.ui.minigame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.abs
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.MiniGameSession
import com.example.animalrace.model.Fx
import com.example.animalrace.model.MiniGameDraw
import com.example.animalrace.race.withSubject
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.art.svgPath
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.sound.LocalSound
import com.example.animalrace.ui.sound.Sfx
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val CapsuleColors = listOf(Color(0xFFFFC93C), Color(0xFF3FD0A6), Color(0xFF5AA9FF), Color(0xFFB983FF), Color(0xFFFF8FB1), Color(0xFFF2543F))
private val Smoke = Color(0xFF5A4A80)

private enum class Gacha { LEVER, DROPPED, OPENING, OPENED }

private class DomeCapsule(val angle: Float, val radius: Float, val color: Color, val tilt: Float)

/**
 * 가챠 캡슐 (시안: "가챠 캡슐").
 * 출전 번호 순으로 레버를 한 바퀴 돌려 캡슐을 뽑고, 세 번 톡톡 두드려 깬다.
 * [MiniGameSession.bomb]번째로 뽑힌 캡슐이 몰빵. 캡슐 색은 장식일 뿐 결과와 무관.
 */
@Composable
internal fun GachaGame(session: MiniGameSession, loser: Int?, onReveal: (Int, Boolean) -> Unit) {
    val n = session.players.size
    val order = remember(session) { session.players.indices.sortedBy { session.players[it].lane } }
    var k by remember(session) { mutableIntStateOf(0) }
    var phase by remember(session) { mutableStateOf(Gacha.LEVER) }
    var taps by remember(session) { mutableIntStateOf(0) }
    var lever by remember(session) { mutableFloatStateOf(0f) }
    // 레버를 돌린 누적 각도 (차례가 바뀌어도 이어서): 돔 안 캡슐이 이만큼 섞인다
    var stir by remember(session) { mutableFloatStateOf(0f) }
    val drop = remember(session) { Animatable(0f) }
    val lid = remember(session) { Animatable(0f) }
    val wobble = remember(session) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val rnd = remember(session) { Random(session.hashCode()) }
    // 돔 안 캡슐 (장식): 중심에서의 각도·거리, 색, 기울기
    val dome = remember(session) {
        List(16) { DomeCapsule(rnd.nextFloat() * 6.283f, 20f + rnd.nextFloat() * 70f, CapsuleColors[rnd.nextInt(CapsuleColors.size)], rnd.nextFloat() * 180f) }
    }
    val colors = remember(session) { List(n) { CapsuleColors[rnd.nextInt(CapsuleColors.size)] } }
    val current = order[MiniGameDraw.turn(k.coerceAtMost(n - 1), n)]
    val bust = k == session.bomb

    fun pulled() {
        phase = Gacha.DROPPED
        sound.play(Sfx.ROLL)
        scope.launch {
            drop.snapTo(0f)
            drop.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            // 굴러 나오면 바로 깨기 화면으로
            delay(250)
            if (phase == Gacha.DROPPED) phase = Gacha.OPENING
        }
    }
    fun tap() {
        if (phase != Gacha.OPENING) return
        taps++
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sound.play(if (taps < 3) Sfx.TAP else if (bust) Sfx.POOF else Sfx.POP)
        scope.launch { wobble.snapTo(10f); wobble.animateTo(0f, spring(dampingRatio = 0.3f)) }
        if (taps >= 3) {
            phase = Gacha.OPENED
            scope.launch { lid.animateTo(1f, tween(500)) }
            if (bust) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onReveal(current, true)
            } else onReveal(current, false)
        }
    }
    fun next() {
        k++; taps = 0; lever = 0f; phase = Gacha.LEVER
        scope.launch { lid.snapTo(0f); drop.snapTo(0f) }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Pal.Cream)
                .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp)),
        ) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            // 기계(시안 390×480)를 칸에 맞춰
            val s = minOf(wPx / 390f, hPx / 490f)
            val dx = (wPx - 390f * s) / 2
            val dy = (hPx - 480f * s) / 2
            val density = LocalDensity.current

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(phase, k) {
                        if (phase != Gacha.LEVER || loser != null) return@pointerInput
                        val c = Offset(dx + 195f * s, dy + 352f * s)
                        var last = 0f
                        var tick = 0
                        detectDragGestures(
                            onDragStart = { p -> last = atan2(p.y - c.y, p.x - c.x) },
                        ) { change, _ ->
                            val p = change.position
                            val a = atan2(p.y - c.y, p.x - c.x)
                            var d = Math.toDegrees((a - last).toDouble()).toFloat()
                            if (d > 180f) d -= 360f
                            if (d < -180f) d += 360f
                            last = a
                            if (d > 0f) { lever += d; stir += d } // 시계 방향만
                            if ((lever / 45f).toInt() > tick) { tick = (lever / 45f).toInt(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sound.play(Sfx.RATCHET) }
                            if (lever >= 360f && phase == Gacha.LEVER) pulled()
                        }
                    },
            ) {
                withTransform({ translate(dx, dy); scale(s, s, Offset.Zero) }) {
                    drawOval(Color(0xFFE8C7D3), Offset(55f, 456f), androidx.compose.ui.geometry.Size(280f, 24f))
                    // 돔
                    ink(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(195f, 150f), 118f)) }, Color(0xFFE8F7FF), 5f)
                    val left = n - k
                    // 레버를 돌리면 캡슐들이 돔 안을 빙글빙글 돌며 통통 튄다 (캡슐마다 빠르기·방향이 달라 섞여 보임)
                    dome.take((left * 3).coerceIn(2, dome.size)).forEachIndexed { i, c ->
                        val speed = (0.5f + (i % 4) * 0.25f) * (if (i % 2 == 0) 1f else -0.7f)
                        val a = c.angle + Math.toRadians((stir * speed).toDouble()).toFloat()
                        val hop = if (stir > 0f) abs(sin(Math.toRadians((stir * 3f + i * 40f).toDouble()).toFloat())) * 10f else 0f
                        capsule(Offset(195f + cos(a) * c.radius, 160f + sin(a) * c.radius * 0.8f - hop), 19f, c.color, c.tilt + stir * speed * 2f, 3f)
                    }
                    drawPath(svgPath("M120 80 A100 100 0 0 1 190 48"), Color.White.copy(alpha = 0.9f), style = androidx.compose.ui.graphics.drawscope.Stroke(10f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    // 몸통
                    ink(svgPath("M92 246 H298 L312 452 Q312 462 300 462 H90 Q78 462 78 452 Z"), Pal.Tomato, 5f)
                    ink(svgPath("M84 236 h222 v20 h-222 Z"), Color(0xFFD7262B), 4f)
                    ink(svgPath("M132 268 h126 q10 0 10 10 v10 q0 10 -10 10 h-126 q-10 0 -10 -10 v-10 q0 -10 10 -10 Z"), Pal.Cream, 3f)
                    // 배출구
                    drawRoundRect(Pal.Ink, Offset(150f, 408f), androidx.compose.ui.geometry.Size(90f, 44f), androidx.compose.ui.geometry.CornerRadius(10f))
                    // 레버
                    ink(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(195f, 352f), 40f)) }, Color.White, 4f)
                    rotate(lever, Offset(195f, 352f)) {
                        ink(svgPath("M185 328 q0 -10 10 -10 q10 0 10 10 v48 q0 10 -10 10 q-10 0 -10 -10 Z"), Pal.Sun, 4f)
                        drawCircle(Pal.Ink, 6f, Offset(195f, 326f))
                    }
                    if (phase == Gacha.LEVER && loser == null) {
                        drawPath(svgPath("M248 318 A62 62 0 0 1 252 392"), Pal.Ink, style = androidx.compose.ui.graphics.drawscope.Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 9f))))
                        ink(svgPath("M244 386 l10 8 4 -12"), null, 4f)
                    }
                    // 굴러 나온 캡슐: 배출구 → 오른쪽으로 데구르르
                    if (phase == Gacha.DROPPED) {
                        val t = drop.value
                        val x = 195f + t * 70f
                        val y = if (t < 0.4f) 420f + t / 0.4f * 20f else 440f - sin((t - 0.4f) * 12f) * 6f * (1f - t)
                        capsule(Offset(x, y), 22f, colors[k], t * 540f, 3.5f)
                    }
                    // 쓴 캡슐 쌓기
                    for (j in 0 until k) capsule(Offset(345f, 445f - j * 30f), 14f, colors[j], 0f, 2.5f, alpha = 0.6f)
                }
            }
            with(density) {
                Text("몰아주기 가챠", fontSize = (17f * s).toDp().value.sp, color = Pal.Ink,
                    modifier = Modifier.offset(x = (dx + 140f * s).toDp(), y = (dy + 272f * s).toDp()))
            }
            when (phase) {
                Gacha.LEVER -> if (loser == null) Tag("레버를 한 바퀴 돌려요 ↻", Pal.Paper, Pal.Ink, fontSize = 15.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                Gacha.DROPPED -> Tag("데구르르~", Pal.Paper, Pal.Ink, fontSize = 15.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                else -> BigCapsule(colors[k], taps, lid.value, wobble.value, bust, phase == Gacha.OPENED, ::tap)
            }
            if (phase == Gacha.OPENED && !bust && loser == null) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).clickable { sound.play(Sfx.CLICK); next() }) {
                    Tag("다음 차례로 →", Pal.Ink, fontSize = 17.sp, modifier = Modifier.padding(4.dp))
                }
            }
            Tag("남은 캡슐 ${n - k}", Pal.Paper, Pal.Ink, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
        }

        val p = session.players[loser ?: current]
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp))
                .padding(start = 6.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnimalSprite(p.animal, 56.dp, fx = if (loser != null) Fx.SCARED else Fx.NONE)
            Column {
                Text(if (loser != null) "${withSubject(p.animal.label)} 몰빵 💸" else "${p.animal.label} 차례!", fontSize = 19.sp, color = Pal.Ink)
                Text(
                    when {
                        loser != null -> "잠시 뒤 최종 발표"
                        phase == Gacha.OPENED -> "꽝 탈출! 🎉"
                        phase == Gacha.OPENING -> "캡슐을 톡톡 두드려 깨요"
                        n - k == 1 -> "마지막 캡슐… 그래도 열어 봐야죠"
                        else -> "레버를 드르륵 돌려 주세요"
                    },
                    fontSize = 12.sp, color = Pal.InkSoft, maxLines = 1,
                )
            }
        }
    }
}

/** 화면 가득 큰 캡슐: 톡톡 → 금 → 뚜껑이 날아가고 쪽지 */
@Composable
private fun BigCapsule(color: Color, taps: Int, lid: Float, wobble: Float, bust: Boolean, opened: Boolean, onTap: () -> Unit) {
    val confetti = remember { List(26) { Triple(Random.nextFloat() * 360f - 180f, Random.nextFloat() * -260f - 40f, CapsuleColors[it % CapsuleColors.size]) } }
    Canvas(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.35f))
            .pointerInput(opened) { if (!opened) detectTapGestures { onTap() } },
    ) {
        val c = Offset(size.width / 2, size.height * 0.55f)
        val sc = size.width / 390f
        withTransform({ translate(c.x, c.y); scale(sc, sc, Offset.Zero); rotate(wobble, Offset.Zero) }) {
            // 쪽지
            if (opened) {
                val up = lid * 70f
                translate(0f, -up) {
                    ink(svgPath("M-110 -38 h220 v76 h-220 Z"), if (bust) Pal.Pink else Pal.Mint, 4f)
                }
            }
            ink(svgPath("M-120 0 A120 120 0 0 0 120 0 Z"), Color.White, 6f)
            // 뚜껑
            withTransform({ translate(lid * 160f, -lid * 320f); rotate(lid * 120f, Offset.Zero) }) {
                ink(svgPath("M-120 0 A120 120 0 0 1 120 0 Z"), color, 6f)
                if (taps >= 1) ink(svgPath("M-30 -110 l14 30 -18 18 20 26"), null, 4f)
                if (taps >= 2) ink(svgPath("M40 -100 l-10 28 22 14 -14 30 M-80 -70 l20 22 -6 24"), null, 4f)
            }
            if (opened && bust) {
                for (j in 0 until 7) drawCircle(Smoke.copy(alpha = 0.5f * (1f - lid * 0.4f)), 36f + j * 6f, Offset(-90f + j * 30f, -60f - lid * 120f - (j % 2) * 30f))
            }
            if (opened && !bust) {
                confetti.forEach { (x, y, col) -> drawRect(col, Offset(x * lid, y * lid), androidx.compose.ui.geometry.Size(10f, 16f)) }
            }
        }
    }
    // 쪽지 글씨: 캔버스와 같은 계산으로 쪽지(중심 y = 0.55h - 70단위) 위에
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val slipY = maxHeight * 0.05f - maxWidth * (70f / 390f) * lid
        if (opened && lid > 0.6f) Text(if (bust) "몰빵! 💸" else "꽝 탈출 🎉", fontSize = 28.sp, color = if (bust) Pal.TomatoText else Pal.Ink, modifier = Modifier.offset(y = slipY))
        else if (!opened) {
            // 시안 "③ 톡톡 깨기": 두드린 만큼 노랗게 차는 동그라미 3개 + 안내, 캡슐 오른쪽 아래에 까딱이는 손가락
            Column(
                Modifier.offset(y = maxHeight * 0.05f + maxWidth * (165f / 390f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (i < taps) Pal.Sun else Color.Transparent)
                                .border(3.dp, Color.White, CircleShape),
                        )
                    }
                }
                Text("톡톡 두드려서 깨기!", fontSize = 16.sp, color = Color.White)
            }
            val bob by rememberInfiniteTransition(label = "hand").animateFloat(
                0f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "bob",
            )
            Text(
                "👆", fontSize = 44.sp,
                modifier = Modifier.offset(x = maxWidth * (70f / 390f), y = maxHeight * 0.05f + maxWidth * (40f / 390f) + 8.dp * bob),
            )
        }
    }
}

/** 반반 캡슐 하나 (위는 색, 아래는 흰색) */
private fun DrawScope.capsule(o: Offset, r: Float, color: Color, deg: Float, sw: Float, alpha: Float = 1f) {
    rotate(deg, o) {
        translate(o.x, o.y) {
            val top = Path().apply { arcTo(androidx.compose.ui.geometry.Rect(Offset.Zero, r), 180f, 180f, false); close() }
            val bottom = Path().apply { arcTo(androidx.compose.ui.geometry.Rect(Offset.Zero, r), 0f, 180f, false); close() }
            drawPath(top, color.copy(alpha = alpha)); drawPath(bottom, Color.White.copy(alpha = alpha))
            drawPath(top, Pal.Ink.copy(alpha = alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawPath(bottom, Pal.Ink.copy(alpha = alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
        }
    }
}
