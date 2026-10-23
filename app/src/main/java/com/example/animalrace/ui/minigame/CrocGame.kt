package com.example.animalrace.ui.minigame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
import kotlin.math.sin

private val CrocGreen = Color(0xFF5FB86A)
private val CrocDark = Color(0xFF4FA35A)
private val CrocShade = Color(0xFF3E8A48)
private val Gum = Color(0xFFF38BA0)
private val Water = Color(0xFF5AA9FF)

/** 시안 좌표(390 폭)에서 아래턱 윗변 높이 */
private fun lowerY(x: Float) = 392f - (x - 84f) * (6f / 258f)

/**
 * 악어 이빨 (시안: "악어 이빨").
 * 이빨 [MiniGameDraw.teeth]개를 출전 번호 순으로 하나씩 꾹 누르고, [MiniGameSession.tooth]를 누르면 콱!
 * 손가락을 대면 턱이 덜덜 떨고, 0.3초 넘게 꾹 눌러야 확정 (짧게 떼면 취소). 이빨이 줄수록 악어가 사나워진다.
 */
@Composable
internal fun CrocGame(session: MiniGameSession, loser: Int?, onReveal: (Int, Boolean) -> Unit) {
    val n = session.players.size
    val teeth = MiniGameDraw.teeth(n)
    val order = remember(session) { session.players.indices.sortedBy { session.players[it].lane } }
    val pressed = remember(session) { mutableStateListOf<Int>() }
    var hover by remember(session) { mutableStateOf<Int?>(null) }
    var phew by remember(session) { mutableStateOf<Int?>(null) }
    var hint by remember(session) { mutableStateOf<String?>(null) }
    val jaw = remember(session) { Animatable(-30f) }
    val shake = remember(session) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sound = LocalSound.current
    val left = teeth - pressed.size
    val current = order[MiniGameDraw.turn(pressed.size, n)]
    val snapped = loser != null
    val anger = 1f - left.toFloat() / teeth

    // 숨쉬기·떨림용 시간
    val clock = rememberInfiniteTransition(label = "croc")
    val t by clock.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart), label = "t")

    val step = 222f / (teeth - 1)
    val toothX = remember(teeth) { List(teeth) { 112f + it * step } }

    fun press(i: Int) {
        pressed += i
        sound.play(Sfx.CLICK)
        if (i == session.tooth) {
            sound.play(Sfx.SNAP)
            scope.launch { jaw.animateTo(-2f, tween(90)) }
            scope.launch { repeat(5) { shake.animateTo(8f, tween(40)); shake.animateTo(-8f, tween(40)) }; shake.animateTo(0f, spring()) }
            onReveal(current, true)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scope.launch { jaw.animateTo(-23f, tween(70)); jaw.animateTo(-30f, spring(dampingRatio = 0.4f)) }
            phew = current
            sound.play(Sfx.PHEW)
        }
    }

    // 남은 이빨이 한 바퀴(인원수) 이하로 줄면 심장박동 진동
    LaunchedEffect(left, snapped) {
        if (snapped || left > n) return@LaunchedEffect
        while (true) {
            sound.play(Sfx.HEART)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(140)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(300L + left * 220L)
        }
    }
    LaunchedEffect(phew) { if (phew != null) { delay(1300); phew = null } }
    LaunchedEffect(hint) { if (hint != null) { delay(1200); hint = null } }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(if (snapped) Pal.Pink else Pal.Sky)
                .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp)),
        ) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            val s = wPx / 390f
            // 물 높이(시안 y 470)를 칸 높이의 80%에
            val dy = hPx * 0.8f - 470f * s
            val density = androidx.compose.ui.platform.LocalDensity.current

            fun toothAt(p: Offset): Int? {
                val x = p.x / s
                val y = (p.y - dy) / s
                return toothX.indices.firstOrNull { i ->
                    i !in pressed && abs(x - toothX[i]) < step / 2 && y in (lowerY(toothX[i]) - 70f)..(lowerY(toothX[i]) + 30f)
                }
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(snapped, pressed.size) {
                        if (snapped) return@pointerInput
                        detectTapGestures(onPress = { p ->
                            val i = toothAt(p) ?: return@detectTapGestures
                            hover = i
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val start = System.currentTimeMillis()
                            val up = tryAwaitRelease()
                            hover = null
                            if (up && System.currentTimeMillis() - start >= 300) press(i)
                            else if (up) hint = "꾹~ 눌러야 해요"
                        })
                    },
            ) {
                // 물
                drawRect(Water, Offset(0f, dy + 466f * s), Size(size.width, size.height))
                withTransform({ translate(shake.value * s, dy); scale(s, s, Offset.Zero) }) {
                    ink(svgPath("M0 470 Q48 456 97 470 T195 470 T292 470 T390 470"), null, 3f)
                    // 몸통
                    ink(svgPath("M-20 470 Q-10 395 70 392 L120 392 L120 470 Z"), CrocDark, 4f)
                    ink(svgPath("M10 404 l10 -14 l10 14 M40 398 l10 -14 l10 14"), CrocShade, 3f)
                    // 아래턱 + 잇몸
                    ink(svgPath("M70 392 L350 386 Q372 388 372 410 Q370 432 346 434 L70 444 Z"), CrocGreen, 4f)
                    ink(svgPath("M84 392 L342 388 Q352 389 352 396 L90 402 Z"), Gum, 3f)
                    // 아래 이빨
                    toothX.forEachIndexed { i, x ->
                        val y = lowerY(x)
                        val down = i in pressed
                        val h = if (down) -8f else -26f
                        val wob = if (hover == i) sin(t * 90f) * 2f else 0f
                        val p = Path().apply { moveTo(x - 11f, y); lineTo(x + wob, y + h); lineTo(x + 11f, y); close() }
                        ink(p, if (down) Color(0xFFD9D2C3) else if (hover == i) Pal.SunPale else Color.White, 3f)
                    }
                    // 윗턱: 숨쉬기 + 손가락을 대면 덜덜
                    val breath = if (snapped) 0f else sin(t * 2.2f) * 1.2f
                    val tremble = if (hover != null) sin(t * 70f) * 1.6f else 0f
                    rotate(jaw.value + breath + tremble, Offset(78f, 386f)) {
                        ink(svgPath("M70 386 L340 380 Q376 380 378 356 Q378 336 352 334 L120 330 Q86 330 70 350 Z"), CrocGreen, 4f)
                        ink(svgPath("M84 384 L344 378 Q352 377 352 370 L90 372 Z"), Gum, 3f)
                        for (k in 0 until 8) {
                            val x = 126f + k * 28f
                            ink(Path().apply { moveTo(x - 9f, 381f); lineTo(x, 395f); lineTo(x + 9f, 381f); close() }, Color.White, 2.5f)
                        }
                        drawOval(Pal.Ink, Offset(345f, 341f), Size(10f, 6f))
                        drawOval(Pal.Ink, Offset(329f, 339f), Size(10f, 6f))
                        ink(svgPath("M120 332 Q130 296 158 300 Q180 304 178 330"), CrocGreen, 4f)
                        // 눈: 이빨이 줄수록 가늘게
                        val ry = if (snapped) 13f else 13f - 7f * anger
                        ink(Path().apply { addOval(androidx.compose.ui.geometry.Rect(135f, 318f - ry, 165f, 318f + ry)) }, Color.White, 3f)
                        val pr = if (snapped) 9f else 7f - 3f * anger
                        drawOval(Pal.Ink, Offset(150f, 318f - pr), Size(8f, pr * 2))
                        val brow = 8f * anger
                        drawLine(Pal.Ink, Offset(132f, 302f - brow * 0.2f), Offset(168f, 300f + brow), 4f, androidx.compose.ui.graphics.StrokeCap.Round)
                        ink(svgPath("M200 340 q8 -6 16 0 M230 338 q8 -6 16 0 M260 337 q8 -6 16 0"), null, 3f, CrocShade)
                    }
                    // 침
                    if (!snapped && left <= n) {
                        val drip = (t * 0.8f) % 1f
                        ink(svgPath("M300 396 q-4 16 0 24 q4 -8 0 -24"), Color(0xFFBFE6FF), 2f)
                        drawCircle(Color(0xFFBFE6FF), 3f, Offset(300f, 422f + drip * 30f), alpha = 1f - drip)
                    }
                    if (snapped) {
                        translate(toothX[session.tooth] - 280f, -20f) {
                            ink(svgPath("M250 300 l14 22 24 -8 -10 22 22 12 -24 6 4 24 -20 -14 -14 20 -6 -24 -24 -2 18 -16 -14 -20 24 4 z"), Pal.Sun, 3f)
                        }
                    }
                }
                // 이빨이 줄수록 가장자리가 붉게
                if (!snapped && anger > 0.3f) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Pal.Tomato.copy(alpha = 0.55f * (anger - 0.3f) / 0.7f)),
                            center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension * 0.75f,
                        ),
                    )
                }
            }

            // 누르는 동물 (콱! 되면 입에 물려 대롱대롱)
            val who = loser ?: current
            val atTooth = if (snapped) session.tooth else (hover ?: pressed.lastOrNull())
            val ax = atTooth?.let { toothX[it] } ?: 330f
            with(density) {
                val sprite = 74.dp
                val px = (ax * s).toDp() - sprite / 2
                val py = ((if (snapped) 360f else 300f) * s + dy).toDp() - sprite
                AnimalSprite(
                    session.players[who].animal, sprite,
                    fx = if (snapped) Fx.SCARED else Fx.NONE,
                    modifier = Modifier.offset(x = px, y = py).rotate(if (snapped) -14f else 0f),
                )
                if (snapped) {
                    Text("콱!", fontSize = 22.sp, color = Pal.Ink, modifier = Modifier.offset(x = ((toothX[session.tooth] - 30f + 18f) * s).toDp(), y = (306f * s + dy).toDp()))
                }
            }

            Tag("남은 이빨 $left", Pal.Paper, Pal.Ink, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            (phew?.let { "휴~ 살았다! (${session.players[it].animal.label})" } ?: hint)?.let {
                Tag(it, Pal.Paper, Pal.Ink, fontSize = 15.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp))
            }
        }

        // 아래: 지금 차례
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
            AnimalSprite(p.animal, 56.dp, fx = if (snapped) Fx.SCARED else Fx.NONE)
            Column {
                Text(if (snapped) "${withSubject(p.animal.label)} 콱! 몰빵 💸" else "${p.animal.label} 차례!", fontSize = 19.sp, color = Pal.Ink)
                Text(
                    if (snapped) "잠시 뒤 최종 발표" else if (left <= n) "남은 이빨 ${left}개… 두근두근" else "이빨 하나를 꾹~ 눌러 주세요",
                    fontSize = 12.sp, color = Pal.InkSoft, maxLines = 1,
                )
            }
        }
    }
}

/** 채우고(있으면) 잉크 테두리 */
internal fun DrawScope.ink(path: Path, fill: Color?, sw: Float, stroke: Color = Pal.Ink) {
    if (fill != null) drawPath(path, fill)
    if (sw > 0f) drawPath(
        path, stroke,
        style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
    )
}
