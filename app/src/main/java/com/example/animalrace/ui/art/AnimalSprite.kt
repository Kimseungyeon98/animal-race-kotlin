package com.example.animalrace.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Animal.*
import com.example.animalrace.model.Fx
import com.example.animalrace.ui.theme.Fredoka
import com.example.animalrace.ui.theme.Jua
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 연출 조각 하나(타원·둥근 사각형·글자). 좌표는 200×170 그림 기준. [top]/[bottom]이 null이면 타원. */
internal data class FxPart(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val fill: Color = Color.Transparent,
    val border: Color? = null, val borderW: Float = 2f,
    val top: Float? = null, val bottom: Float? = null,
    val alpha: Float = 1f, val rot: Float = 0f,
    val text: String = "", val fontSize: Float = 12f, val textColor: Color = Ink, val numberFont: Boolean = false,
)

internal data class FxFrame(
    val back: List<FxPart> = emptyList(),
    val front: List<FxPart> = emptyList(),
    val dx: Float = 0f, val dy: Float = 0f, val rot: Float = 0f,
    val pose: Int = 0,
)

private val BubbleSelf = Color.White
private val BubbleMean = Color(0xFFFFC93C)
private val BubbleHurt = Color(0xFFFFE1DC)
private val Sweat = Color(0xFF9BD7FF)
private val Flame1 = Color(0xFFFF8A3D)
private val Flame2 = Color(0xFFFFC93C)
private val Banana = Color(0xFFFFD84A)
private val Star = Color(0xFFFFB020)

private fun bubble(text: String, x: Float, y: Float, bg: Color = BubbleSelf) =
    FxPart(x, y, text.length * 15f + 20f, 28f, fill = bg, border = Ink, borderW = 3f, top = 14f, bottom = 14f, text = text, fontSize = 16f)

private fun speedLine(x: Float, y: Float, w: Float, alpha: Float = 1f) =
    FxPart(x, y, w, 7f, fill = Color.White, border = Ink, top = 4f, bottom = 4f, alpha = alpha)

/**
 * 반복해서 흘러가는 조각(속도선·불꽃)의 진행도 0~1. 처음 위치로 되돌아갈 때 툭 튀지 않도록
 * 시작과 끝에서 서서히 나타나고 사라지는 투명도를 함께 준다.
 */
private fun loopFade(p: Float): Float = (sin(p * PI.toFloat())).coerceIn(0f, 1f)

/** 좌우 떨림: 프레임마다 ±로 튀지 않고 부드럽게 흔들린다 (약 2Hz). */
private fun wobble(t: Float, amp: Float) = sin(t * PI.toFloat() / 4f) * amp

private fun blob(x: Float, y: Float, w: Float, h: Float, fill: Color, alpha: Float = 1f) =
    FxPart(x, y, w, h, fill = fill, border = Ink, alpha = alpha)

private fun glyph(t: String, x: Float, y: Float, size: Float, color: Color, alpha: Float = 1f) =
    FxPart(x, y, size * 1.2f, size * 1.2f, text = t, fontSize = size, textColor = color, alpha = alpha, numberFont = true)

private fun banana(x: Float, y: Float, rot: Float, alpha: Float = 1f) =
    FxPart(x, y, 28f, 11f, fill = Banana, border = Ink, top = 10f, bottom = 14f, rot = rot, alpha = alpha)

/**
 * [fx] 연출의 한 순간. [tick]은 60ms 단위 시간(디자인 시안과 같은 단위).
 * 스킬 연출은 동물마다 다르고, 피격 반응(미끌·휘청·깜짝)은 공통이다.
 */
internal fun fxFrame(animal: Animal, fx: Fx, tick: Float, loop: Boolean = false): FxFrame {
    val t = tick
    val norm = 1 + (t / 3).toInt() % 2
    val fast = 1 + (t / 2).toInt() % 2
    val cycle = 40f
    val ph = t % cycle
    val back = mutableListOf<FxPart>()
    val front = mutableListOf<FxPart>()
    var dx = 0f; var dy = 0f; var rot = 0f; var pose = norm

    val hit = when {
        fx == Fx.HIT_CYCLE && animal == TURTLE -> Fx.BLOCK
        fx == Fx.HIT_CYCLE -> listOf(Fx.SLIP, Fx.QUAKE, Fx.SCARED)[(t / cycle).toInt() % 3]
        else -> fx
    }

    when (hit) {
        Fx.SKILL, Fx.SKILL_ALT -> {
            // 반복 재생일 때는 두 단계 스킬(질주→헉헉, 질주→그루밍, 먹기→돌진)을 한 주기 안에서 이어서 보여 준다
            val alt = hit == Fx.SKILL_ALT || (loop && when (animal) {
                CHEETAH -> ph >= 24
                CAT -> ph >= 20
                PIG -> ph >= 16
                else -> false
            })
            when (animal) {
                HORSE -> {
                    pose = fast
                    for (k in 0..2) { val m = (t * 7 + k * 9) % 26; back += speedLine(14 - m, 72f + k * 18, 34f - k * 6, loopFade(m / 26)) }
                    front += bubble("스퍼트!", 112f, -16f)
                }
                CHEETAH -> if (!alt) {
                    pose = fast
                    for (k in 0..2) { val m = (t * 5 + k * 7) % 14; back += blob(16 - m, 84f + k * 11, 30f - k * 6, 15f, if (k % 2 == 1) Flame2 else Flame1, 0.95f * loopFade(m / 14)) }
                    front += bubble("슝!", 132f, -16f)
                } else {
                    pose = 0; dy = 4f
                    front += blob(184f, 50 + t % 10, 8f, 12f, Sweat)
                    front += blob(194f, 62 + t % 8, 6f, 9f, Sweat)
                    front += bubble("헉헉…", 112f, -16f)
                }
                RABBIT -> {
                    pose = 0; rot = 4f
                    for (k in 0..2) {
                        val zy = 44 - (t * 1.5f + k * 14) % 42
                        front += glyph("Z", 176f + k * 9, zy, 14f + k * 4, Ink, ((zy + 4) / 30).coerceIn(0f, 1f))
                    }
                    front += bubble("쿨쿨…", 70f, -16f)
                }
                TURTLE -> {
                    val on = (sin(t / 20 * 2 * PI.toFloat()) + 1) / 2
                    front += blob(78f, 62f, 12f, 7f, Color.White, 0.3f + 0.7f * on)
                    front += blob(116f, 70f, 8f, 5f, Color.White, 1f - 0.7f * on)
                    front += bubble("꾸준히~", 108f, -10f)
                }
                DOG -> {
                    pose = fast
                    for (k in 0..2) front += glyph("~", 194 + (t * 3 + k * 8) % 16, 58f + k * 16, 22f, Color(0xFF8A5A34), 0.9f)
                    front += bubble("킁킁!", 116f, -18f)
                }
                CAT -> if (!alt) {
                    pose = fast
                    for (k in 0..1) { val m = (t * 7 + k * 11) % 24; back += speedLine(14 - m, 82f + k * 20, 30f, loopFade(m / 24)) }
                    front += bubble("냥!", 134f, -16f)
                } else {
                    pose = 0; dy = 2f
                    front += blob(184f, 126f, 26f, 26f, Color(0xFFFF9DC0))
                    front += glyph("♡", 178f, 36 - (t % 20), 18f, Color(0xFFF2543F), 0.9f)
                    front += bubble("그루밍…", 92f, -16f)
                }
                PIG -> if (!alt) {
                    pose = 0
                    val a = 1 - min(ph, 16f) / 16f
                    front += blob(190 - 13 * a, 98 - 12 * a, 26 * a + 4, 24 * a + 4, Color(0xFFF2543F))
                    front += blob(196f, 86f, 8 * a + 2, 6 * a + 2, Color(0xFF5DB65E))
                    front += bubble("냠냠", 118f, -16f)
                } else {
                    pose = fast
                    back += FxPart(20f, 40f, 160f, 120f, fill = Color(0xFFFFE36A), alpha = 0.5f)
                    for (k in 0..1) { val m = (t * 7 + k * 11) % 24; back += speedLine(10 - m, 84f + k * 22, 32f, loopFade(m / 24)) }
                    front += bubble("힘이 넘쳐!", 92f, -18f)
                }
                MONKEY -> {
                    val bp = t % 30
                    front += banana(150 + bp * 4, 66 - sin(bp / 30 * PI.toFloat()) * 56, bp * 30, if (bp > 26) 0f else 1f)
                    front += bubble("바나나 슝!", 62f, -18f, BubbleMean)
                }
                PENGUIN -> {
                    pose = 0; rot = 70f; dy = 22f; dx = -6f
                    back += FxPart(-10f, 150f, 180f, 10f, fill = Color(0xFFCDEFFF), border = Ink, top = 6f, bottom = 6f)
                    for (k in 0..1) front += glyph("✦", 10 + (t * 6 + k * 40) % 90, 128f + k * 6, 14f, Color(0xFF5AA9FF))
                    front += bubble("슈우욱", 112f, -8f)
                }
                KANGAROO -> {
                    val jp = t % 12
                    dy = -sin(jp / 12 * PI.toFloat()) * 46
                    pose = if (dy < -5) 1 else 0
                    if (jp > 9 || jp < 2) {
                        back += blob(34f, 146f, 28f, 14f, Color(0xFFE8D3B0), 0.85f)
                        back += blob(66f, 150f, 20f, 10f, Color(0xFFE8D3B0), 0.85f)
                    }
                    front += bubble("껑충!", 112f, -18f)
                }
                ELEPHANT -> {
                    val st = ph % 20
                    dy = if (st < 4) -8f else 0f
                    pose = 0
                    val rw = 30 + st * 8; val rh = 8 + st * 2
                    back += FxPart(100 - rw / 2, 154 - rh / 2, rw, rh, border = Ink, borderW = 3f, alpha = max(0f, 1 - st / 20))
                    front += bubble("쿵! 쿵!", 100f, -18f, BubbleMean)
                }
                DUCK -> {
                    pose = 0; dx = wobble(t, 2f)
                    for (k in 0..2) front += glyph(")", 186f + k * 8, 56f - k * 4, 22f + k * 6, Ink, 1 - ((t + k * 3) % 12) / 12)
                    front += bubble("꽥!!", 118f, -20f, BubbleMean)
                }
                SNAIL -> {
                    dx = wobble(t, 1f)
                    for (k in 0..2) { val m = (t * 4 + k * 6) % 12; back += blob(34 - m, 82f + k * 9, 32f - k * 7, 15f, if (k % 2 == 1) Flame2 else Flame1, 0.95f * loopFade(m / 12)) }
                    for (k in 0..1) { val m = (t * 3 + k * 12) % 30; back += blob(-6 - m, 68f + k * 16, 16f, 16f, Color(0xFFE4E0EA), 0.75f * loopFade(m / 30)) }
                    front += bubble("로켓 점화!", 92f, -16f)
                }
                CHICKEN -> {
                    pose = fast
                    dy = -abs(sin(t / 4)) * 8
                    for (k in 0..2) {
                        val fy = 50 + (t * 2 + k * 26) % 80
                        front += FxPart(
                            40f + k * 30 + sin((t + k * 5) / 4) * 8, fy, 16f, 7f, fill = Color.White, border = Ink,
                            rot = (t * 12 + k * 60) % 360, alpha = max(0f, 1 - (fy - 50) / 80),
                        )
                    }
                    front += bubble("푸드덕!", 120f, -16f)
                }
            }
        }
        Fx.SLIP -> {
            pose = 0; rot = -22f; dx = -6f
            back += FxPart(122f, 146f, 34f, 12f, fill = Banana, border = Ink, top = 16f, bottom = 4f)
            for (k in 0..1) {
                val ang = t / 4 + k * PI.toFloat()
                front += glyph("✦", 146 + cos(ang) * 20, 16 + sin(ang) * 6, 16f, Star)
            }
            front += bubble("미끌!", 30f, -14f, BubbleHurt)
        }
        Fx.QUAKE -> {
            pose = 0; rot = sin(t / 2) * 10; dx = sin(t / 2) * 4
            for (k in 0..2) {
                val ang = t / 4 + k * 2.1f
                front += glyph("✦", 140 + cos(ang) * 24, 14 + sin(ang) * 7, 15f, Star)
            }
            front += bubble("휘청~", 30f, -14f, BubbleHurt)
        }
        Fx.SCARED -> {
            pose = 0
            val q = t % cycle
            dy = if (q < 8) -14 * sin(q / 8 * PI.toFloat()) else 0f
            dx = wobble(t * 1.5f, 2f)
            front += glyph("!", 168f, -6f, 36f, Color(0xFFF2543F))
            front += blob(186f, 44f, 8f, 12f, Sweat)
            front += bubble("깜짝!", 30f, -14f, BubbleHurt)
        }
        Fx.BLOCK -> {
            pose = 0
            back += FxPart(
                6f, 28f, 196f, 140f, fill = Color(0x479BD7FF), border = Color(0xFF5AA9FF), borderW = 3f,
                alpha = 0.65f + 0.35f * sin(t / 3),
            )
            val b = t % 30
            front += banana(186 + b * 2, 40 - b * 1.6f + b * b * 0.05f, b * 25, 1 - b / 30)
            front += bubble("팅!", 120f, -14f)
        }
        Fx.RALLY -> {
            pose = fast
            dy = -abs(sin(t / 2)) * 3
            for (k in 0..2) { val m = (t * 7 + k * 9) % 26; back += speedLine(14 - m, 70f + k * 18, 34f - k * 6, loopFade(m / 26)) }
            for (k in 0..1) { val m = (t * 3 + k * 10) % 20; back += blob(18 - m, 140f - k * 6, 18f + m, 12f + m * 0.4f, Color(0xFFE8D3B0), 0.85f * loopFade(m / 20)) }
            front += bubble("분발!", 112f, -18f)
        }
        Fx.TRIP -> {
            pose = 0
            val q = (t / 6).coerceAtMost(1f)
            rot = -18f * q; dy = 8f * q; dx = -4f * q
            back += blob(120f, 146f, 44f, 14f, Color(0xFFE8D3B0), 0.85f)
            for (k in 0..2) {
                val ang = t / 4 + k * 2.1f
                front += glyph("✦", 140 + cos(ang) * 24, 16 + sin(ang) * 7, 15f, Star)
            }
            front += bubble("꽈당!", 30f, -14f, BubbleHurt)
        }
        Fx.NONE, Fx.HIT_CYCLE -> Unit
    }
    return FxFrame(back, front, dx, dy, rot, pose)
}

/** 화면이 떠 있는 동안 흐르는 시간(초). */
@Composable
private fun rememberClock(): Float {
    val t by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1e9f }
    }
    return t
}

/**
 * 동물 캐릭터. 크기는 [size](폭)이고 높이는 폭의 0.85배.
 * - [running]: 다리를 번갈아 움직이며 달림
 * - [fx]: 스킬/피격 연출. [fxTime]을 주면 그 시간(초)에 맞춰 진행하고, 없으면 스스로 반복 재생
 */
@Composable
fun AnimalSprite(
    animal: Animal,
    size: Dp,
    modifier: Modifier = Modifier,
    running: Boolean = false,
    frame: Int = 0,
    fx: Fx = Fx.NONE,
    fxTime: Float? = null,
) {
    val animated = running || (fx != Fx.NONE && fxTime == null)
    val clock = if (animated) rememberClock() else 0f
    val effect = if (fx != Fx.NONE) fxFrame(animal, fx, (fxTime ?: clock) / 0.06f, loop = fxTime == null)
    else FxFrame(pose = if (running) 1 + (clock / 0.18f).toInt() % 2 else frame)
    // 연출 중이어도 달리는 중이면 다리는 계속 굴린다 (연출이 멈춤 자세를 고른 경우 제외)
    val pose = when {
        effect.pose == 0 -> POSES[0]
        animated -> gait(clock + animal.ordinal * 0.05f) // 동물마다 발 구르는 박자를 살짝 엇갈리게
        else -> POSES[effect.pose.coerceIn(0, 2)]
    }
    val s = size.value / 200f

    Box(modifier.size(size, size * 0.85f)) {
        effect.back.forEach { FxPartView(it, s) }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = effect.dx * s * density
                    translationY = effect.dy * s * density
                    rotationZ = effect.rot
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                },
        ) {
            scale(this.size.width / 200f, pivot = Offset.Zero) { drawAnimal(animal, pose) }
        }
        effect.front.forEach { FxPartView(it, s) }
    }
}

@Composable
private fun FxPartView(p: FxPart, s0: Float) {
    // 말풍선은 작은 캐릭터(경주 화면)에서도 읽히도록 최소 배율을 두고, 아래 가운데를 기준으로 키운다
    val s = if (p.text.isNotEmpty() && p.border != null) maxOf(s0, 0.72f) else s0
    val x = (p.x + p.w / 2) * s0 - p.w * s / 2
    val y = (p.y + p.h) * s0 - p.h * s
    val shape: Shape = if (p.top == null) CircleShape
    else RoundedCornerShape((p.top * s).dp, (p.top * s).dp, ((p.bottom ?: p.top) * s).dp, ((p.bottom ?: p.top) * s).dp)
    var m = Modifier
        .offset(x.dp, y.dp)
        .size((p.w * s).coerceAtLeast(1f).dp, (p.h * s).coerceAtLeast(1f).dp)
        .graphicsLayer { alpha = p.alpha; rotationZ = p.rot }
    if (p.fill != Color.Transparent || p.border != null) {
        m = m.clip(shape).background(p.fill, shape)
        if (p.border != null) m = m.border((p.borderW * s).coerceAtLeast(1f).dp, p.border, shape)
    }
    Box(m, contentAlignment = Alignment.Center) {
        if (p.text.isNotEmpty()) {
            Text(
                p.text,
                color = p.textColor,
                fontSize = (p.fontSize * s).coerceAtLeast(8f).sp,
                lineHeight = (p.fontSize * s).coerceAtLeast(8f).sp,
                fontFamily = if (p.numberFont) Fredoka else Jua,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
