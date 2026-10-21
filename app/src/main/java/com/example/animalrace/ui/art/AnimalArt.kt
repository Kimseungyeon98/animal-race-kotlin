package com.example.animalrace.ui.art

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Animal.*

/*
 * 캐릭터 일러스트. 디자인 캔버스의 SVG(200×170 좌표)를 그대로 옮겼다.
 * 모든 도형은 "채우기 → 잉크 외곽선" 순서로 그리고, 다리·날개는 legA/legB/wing 각도로 돌린다.
 */

internal val Ink = Color(0xFF2B2140)
private val White = Color.White

/** 달리기 프레임: 0 = 기본, 1·2 = 다리 교차 */
internal data class Pose(val legA: Float, val legB: Float, val bob: Float, val wing: Float)

internal val POSES = listOf(
    Pose(0f, 0f, 0f, 0f),
    Pose(26f, -26f, -5f, -14f),
    Pose(-22f, 22f, 0f, 8f),
)

/**
 * 달리는 자세를 시간에 따라 연속으로 계산한다. 두 교차 자세를 번갈아 끊어 보여 주면
 * 움직이는 화면에서 깜빡이듯 번져 보여서, 사인 곡선으로 부드럽게 이어 준다.
 */
internal fun gait(seconds: Float, period: Float = 0.34f): Pose {
    val w = 2f * PI.toFloat() * seconds / period
    val k = (1f - cos(w)) / 2f
    val a = POSES[1]
    val b = POSES[2]
    return Pose(
        legA = a.legA + (b.legA - a.legA) * k,
        legB = a.legB + (b.legB - a.legB) * k,
        bob = -3.5f * abs(sin(w)),
        wing = a.wing + (b.wing - a.wing) * k,
    )
}

private val pathCache = HashMap<String, Path>()

/** SVG path 문자열을 한 번만 파싱해서 재사용한다. */
internal fun svgPath(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private fun DrawScope.paint(path: Path, fill: Color?, sw: Float, stroke: Color) {
    if (fill != null) drawPath(path, fill)
    if (sw > 0f) drawPath(path, stroke, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.svg(d: String, fill: Color?, sw: Float = 5f, stroke: Color = Ink, rot: Float = 0f, px: Float = 0f, py: Float = 0f) {
    if (rot == 0f) paint(svgPath(d), fill, sw, stroke)
    else rotate(rot, Offset(px, py)) { paint(svgPath(d), fill, sw, stroke) }
}

private fun DrawScope.ellipse(
    cx: Float, cy: Float, rx: Float, ry: Float, fill: Color?, rot: Float = 0f,
    px: Float = cx, py: Float = cy, sw: Float = 5f,
) {
    val p = Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }
    if (rot == 0f) paint(p, fill, sw, Ink) else rotate(rot, Offset(px, py)) { paint(p, fill, sw, Ink) }
}

private fun DrawScope.circle(cx: Float, cy: Float, r: Float, fill: Color?, sw: Float = 5f) = ellipse(cx, cy, r, r, fill, sw = sw)

private fun DrawScope.rrect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, rot: Float = 0f, px: Float = 0f, py: Float = 0f) {
    val p = Path().apply { addRoundRect(RoundRect(x, y, x + w, y + h, CornerRadius(r, r))) }
    if (rot == 0f) paint(p, fill, 5f, Ink) else rotate(rot, Offset(px, py)) { paint(p, fill, 5f, Ink) }
}

private fun DrawScope.dot(cx: Float, cy: Float, r: Float, color: Color = Ink) = drawCircle(color, r, Offset(cx, cy))

private fun DrawScope.eye(cx: Float, cy: Float, r: Float, hx: Float, hy: Float, hr: Float) {
    dot(cx, cy, r); dot(hx, hy, hr, White)
}

private fun DrawScope.shadow(cx: Float, cy: Float, rx: Float, ry: Float) =
    drawOval(Ink.copy(alpha = 0.15f), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))

private fun c(hex: Long) = Color(hex)

// ---------- 다리 ----------
// 위는 굵고 아래로 가늘어지는 다리 + 발. 먼 쪽 다리는 몸통 뒤에 통째로 그리고,
// 가까운 다리는 몸통 위에 채운 뒤 테두리를 몸통 바깥에서만 그려서 몸 윤곽이 끊김 없이 다리로 이어지게 한다.

private enum class Foot { PAW, HOOF, PAD }

private val legCache = HashMap<String, Path>()

/** 예전 막대 다리 자리 (x, y, w, h)에 맞춘 다리 모양. 뒷다리는 허벅지에서 살짝 꺾인다. */
private fun legShape(x: Float, y: Float, w: Float, h: Float, hind: Boolean): Path = legCache.getOrPut("$x,$y,$w,$h,$hind") {
    val cx = x + w / 2
    val a = w / 2 + 2.5f
    val b = maxOf(4.5f, w / 2 - 1.5f)
    val y0 = y - 4
    val l = h + 4
    Path().apply {
        if (hind) {
            moveTo(cx - a - 2, y0)
            cubicTo(cx - a - 3, y0 + l * 0.35f, cx - b - 1, y0 + l * 0.55f, cx - b, y0 + l * 0.75f)
            lineTo(cx - b, y0 + l); lineTo(cx + b, y0 + l); lineTo(cx + b, y0 + l * 0.75f)
            cubicTo(cx + b + 1, y0 + l * 0.5f, cx + a, y0 + l * 0.3f, cx + a, y0)
        } else {
            moveTo(cx - a, y0)
            cubicTo(cx - a, y0 + l * 0.4f, cx - b - 1.5f, y0 + l * 0.6f, cx - b, y0 + l)
            lineTo(cx + b, y0 + l)
            cubicTo(cx + b + 1.5f, y0 + l * 0.6f, cx + a, y0 + l * 0.4f, cx + a, y0)
        }
        close()
    }
}

private fun DrawScope.foot(foot: Foot, x: Float, w: Float, y: Float, h: Float, color: Color, claws: Boolean) {
    val cx = x + w / 2
    val b = maxOf(4.5f, w / 2 - 1.5f)
    val yb = y + h
    when (foot) {
        Foot.PAW -> ellipse(cx + 2.5f, yb + 1, b + 4, 5.5f, color, sw = 4f)
        Foot.HOOF -> {
            val r = CornerRadius(3f, 3f)
            val hoof = Path().apply { addRoundRect(RoundRect(cx - b - 1, yb - 3, cx + b + 1, yb + 5, CornerRadius.Zero, CornerRadius.Zero, r, r)) }
            paint(hoof, color, 4f, Ink)
        }
        Foot.PAD -> {
            ellipse(cx + 1, yb + 1, b + 5, 5.5f, color, sw = 4f)
            for (i in 0..2) svg("M${cx - b + 2 + i * (b - 1)} ${yb + 3} q2 -3 4 0", null, sw = 2f, stroke = White)
        }
    }
    if (claws) svg("M${cx - 3} ${yb + 5} l-3 6 M${cx + 2} ${yb + 6} l0 6 M${cx + 7} ${yb + 5} l3 6", null, sw = 3f, stroke = c(0xFFE9E1DD))
}

/** [shapes] 바깥에서만 그린다 (몸통 안쪽은 잘라 냄). */
private fun DrawScope.outside(shapes: List<Path>, block: DrawScope.() -> Unit) {
    if (shapes.isEmpty()) block() else clipPath(shapes[0], ClipOp.Difference) { outside(shapes.drop(1), block) }
}

private fun DrawScope.farLegOf(shape: Path, fill: Color, rot: Float, px: Float, py: Float, feet: DrawScope.() -> Unit) {
    rotate(rot, Offset(px, py)) { paint(shape, fill, 5f, Ink); feet() }
}

private fun DrawScope.nearLegOf(shape: Path, fill: Color, rot: Float, px: Float, py: Float, body: List<Path>, feet: DrawScope.() -> Unit) {
    rotate(rot, Offset(px, py)) { drawPath(shape, fill) }
    outside(body) { rotate(rot, Offset(px, py)) { drawPath(shape, Ink, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)) } }
    rotate(rot, Offset(px, py)) { feet() }
}

private fun DrawScope.farLeg(
    x: Float, y: Float, w: Float, h: Float, fill: Color, rot: Float, px: Float, py: Float,
    hind: Boolean, foot: Foot, footColor: Color = fill, claws: Boolean = false,
) = farLegOf(legShape(x, y, w, h, hind), fill, rot, px, py) { foot(foot, x, w, y, h, footColor, claws) }

private fun DrawScope.nearLeg(
    x: Float, y: Float, w: Float, h: Float, fill: Color, rot: Float, px: Float, py: Float,
    hind: Boolean, body: List<Path>, foot: Foot, footColor: Color = fill, claws: Boolean = false,
) = nearLegOf(legShape(x, y, w, h, hind), fill, rot, px, py, body) { foot(foot, x, w, y, h, footColor, claws) }

/** 위쪽을 둥글게 마무리한 가늘어지는 다리. 몸 위에 통째로 그려도 몸에 붙은 다리처럼 보인다(몸과 색이 다른 동물용). */
private fun roundLegShape(x: Float, y: Float, w: Float, h: Float, hind: Boolean): Path = legCache.getOrPut("r$x,$y,$w,$h,$hind") {
    val cx = x + w / 2
    val a = w / 2 + 2.5f
    val b = maxOf(4.5f, w / 2 - 1.5f)
    val left = if (hind) cx - a - 2 else cx - a
    Path().apply {
        moveTo(left, y)
        if (hind) {
            cubicTo(cx - a - 3, y + h * 0.35f, cx - b - 1, y + h * 0.55f, cx - b, y + h * 0.75f)
            lineTo(cx - b, y + h); lineTo(cx + b, y + h); lineTo(cx + b, y + h * 0.75f)
            cubicTo(cx + b + 1, y + h * 0.5f, cx + a, y + h * 0.3f, cx + a, y)
        } else {
            cubicTo(cx - a, y + h * 0.4f, cx - b - 1.5f, y + h * 0.6f, cx - b, y + h)
            lineTo(cx + b, y + h)
            cubicTo(cx + b + 1.5f, y + h * 0.6f, cx + a, y + h * 0.4f, cx + a, y)
        }
        cubicTo(cx + a, y - a * 1.1f, left, y - a * 1.1f, left, y)
        close()
    }
}

private fun DrawScope.roundLeg(x: Float, y: Float, w: Float, h: Float, fill: Color, rot: Float, px: Float, py: Float, hind: Boolean) =
    farLegOf(roundLegShape(x, y, w, h, hind), fill, rot, px, py) { foot(Foot.PAW, x, w, y, h, fill, false) }

private fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }

private fun roundBox(x: Float, y: Float, w: Float, h: Float, r: Float) =
    Path().apply { addRoundRect(RoundRect(x, y, x + w, y + h, CornerRadius(r, r))) }

/** 200×170 좌표계 기준으로 동물 한 마리를 그린다. 호출하는 쪽에서 크기에 맞게 scale한다. */
internal fun DrawScope.drawAnimal(animal: Animal, p: Pose) {
    val a = p.legA; val b = p.legB; val bob = p.bob
    when (animal) {
        HORSE -> {
            shadow(100f, 160f, 64f, 7f)
            translate(0f, bob) {
                svg("M40 84 q-28 6 -24 44 q14 -10 22 -26 z", c(0xFF6B3E22))
                val hindLeg = svgPath("M58 96 C56 108 62 116 66 122 C68 130 66 140 66 146 L78 146 C78 138 80 128 78 120 C77 110 82 102 80 96 Z")
                val frontLeg = svgPath("M128 98 C128 110 134 120 134 130 C134 136 133 142 132 146 L144 146 C145 140 146 134 145 126 C146 116 150 108 150 98 Z")
                val hoof = c(0xFF4A2E1A)
                val hindHoof: DrawScope.() -> Unit = { svg("M64 144 h16 v7 q0 3 -3 3 h-10 q-3 0 -3 -3 z", hoof, sw = 4f) }
                val frontHoof: DrawScope.() -> Unit = { svg("M130 144 h16 v7 q0 3 -3 3 h-10 q-3 0 -3 -3 z", hoof, sw = 4f) }
                translate(-12f, 0f) {
                    farLegOf(hindLeg, c(0xFFA86B3C), b, 70f, 104f, hindHoof)
                    farLegOf(frontLeg, c(0xFFA86B3C), a, 138f, 104f, frontHoof)
                }
                // 몸통과 목을 하나의 곡선 윤곽으로 그려 어깨에 이음새 선이 생기지 않게 한다
                val torso = svgPath("M36 98 C36 76 56 66 86 66 C104 66 116 64 124 56 C130 50 134 44 140 38 L164 50 C160 62 156 74 154 86 C152 104 140 124 104 126 C72 128 36 122 36 98 Z")
                paint(torso, c(0xFFC98A52), 5f, Ink)
                nearLegOf(hindLeg, c(0xFFC98A52), a, 70f, 104f, listOf(torso), hindHoof)
                nearLegOf(frontLeg, c(0xFFC98A52), b, 138f, 104f, listOf(torso), frontHoof)
                svg("M144 30 C134 42 124 56 104 66 L112 72 C128 64 140 52 152 38 Z", c(0xFF6B3E22))
                svg("M140 36 l4 -18 l12 16 z", c(0xFFC98A52))
                ellipse(160f, 52f, 28f, 17f, c(0xFFC98A52), rot = 24f)
                ellipse(180f, 66f, 12f, 10f, c(0xFFE8B98A))
                svg("M148 34 q2 -9 11 -9 q-1 8 -6 13 z", c(0xFF6B3E22), sw = 3.5f)
                eye(160f, 48f, 4.5f, 161.5f, 46.5f, 1.6f)
                dot(186f, 66f, 2.2f)
            }
        }
        CHEETAH -> {
            shadow(100f, 160f, 66f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 98f, 60f, 26f))
                svg("M38 90 q-30 -8 -24 -42", null, sw = 12f)
                svg("M38 90 q-30 -8 -24 -42", null, sw = 5f, stroke = c(0xFFF5C04A))
                farLeg(56f, 104f, 14f, 46f, c(0xFFDDA437), b, 63f, 108f, hind = true, Foot.PAW)
                farLeg(118f, 104f, 14f, 46f, c(0xFFDDA437), a, 125f, 108f, hind = false, Foot.PAW)
                ellipse(92f, 98f, 60f, 26f, c(0xFFF5C04A))
                val spot = c(0xFF6B4A1E)
                dot(66f, 92f, 5f, spot); dot(88f, 102f, 4f, spot); dot(106f, 88f, 5f, spot)
                dot(58f, 106f, 4f, spot); dot(124f, 102f, 4f, spot); dot(84f, 84f, 3.5f, spot)
                nearLeg(68f, 104f, 14f, 46f, c(0xFFF5C04A), a, 75f, 108f, hind = true, body, Foot.PAW)
                nearLeg(130f, 104f, 14f, 46f, c(0xFFF5C04A), b, 137f, 108f, hind = false, body, Foot.PAW)
                circle(146f, 62f, 8f, c(0xFFF5C04A)); circle(168f, 60f, 8f, c(0xFFF5C04A))
                circle(158f, 84f, 24f, c(0xFFF5C04A))
                ellipse(176f, 92f, 11f, 8f, c(0xFFFFF1D0))
                eye(164f, 78f, 4.5f, 165.5f, 76.5f, 1.6f)
                svg("M163 84 q1 10 8 14", null, sw = 3f)
                dot(185f, 89f, 3f)
            }
        }
        RABBIT -> {
            shadow(100f, 160f, 60f, 7f)
            translate(0f, bob) {
                ellipse(58f, 140f, 24f, 12f, c(0xFFF4EEF2), a, 70f, 136f)
                circle(34f, 100f, 14f, White)
                ellipse(128f, 34f, 11f, 32f, c(0xFFF4EEF2), rot = -18f)
                ellipse(128f, 36f, 4f, 20f, c(0xFFFFB3C7), rot = -18f, sw = 0f)
                ellipse(88f, 106f, 52f, 38f, c(0xFFF4EEF2))
                ellipse(122f, 142f, 18f, 10f, c(0xFFF4EEF2), b, 114f, 136f)
                ellipse(152f, 36f, 11f, 30f, c(0xFFF4EEF2), rot = 14f)
                circle(146f, 82f, 34f, c(0xFFF4EEF2))
                eye(158f, 76f, 5f, 160f, 74f, 1.8f)
                ellipse(160f, 95f, 8f, 5f, c(0xFFFFB3C7), sw = 0f)
                svg("M175 82 l7 -3 l0 7 z", c(0xFFFF7A9A), sw = 3f)
            }
        }
        TURTLE -> {
            shadow(100f, 160f, 66f, 7f)
            translate(0f, bob) {
                ellipse(66f, 140f, 16f, 12f, c(0xFF9BD86E), a, 66f, 132f)
                ellipse(132f, 140f, 16f, 12f, c(0xFF9BD86E), b, 132f, 132f)
                svg("M34 122 l-12 8 l10 4 z", c(0xFF9BD86E))
                circle(164f, 104f, 22f, c(0xFF9BD86E))
                eye(172f, 98f, 4.5f, 174f, 96f, 1.6f)
                svg("M172 112 q6 4 12 0", null, sw = 3f)
                svg("M38 124 Q44 52 100 50 Q156 52 162 124 Z", c(0xFF4FAE5A))
                svg("M100 66 l18 10 l0 20 l-18 10 l-18 -10 l0 -20 z", c(0xFF6CC46B), sw = 4f)
                svg("M62 104 l10 -14 l10 6", null, sw = 4f)
                svg("M138 104 l-10 -14 l-10 6", null, sw = 4f)
                rrect(32f, 116f, 136f, 16f, 8f, c(0xFF3E8E4A))
            }
        }
        DOG -> {
            shadow(100f, 160f, 62f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 100f, 54f, 32f))
                svg("M42 86 q-20 -18 -8 -36", null, sw = 12f)
                svg("M42 86 q-20 -18 -8 -36", null, sw = 5f, stroke = c(0xFFE2A86A))
                farLeg(58f, 108f, 16f, 42f, c(0xFFC98E52), b, 66f, 112f, hind = true, Foot.PAW)
                farLeg(116f, 108f, 16f, 42f, c(0xFFC98E52), a, 124f, 112f, hind = false, Foot.PAW)
                ellipse(92f, 100f, 54f, 32f, c(0xFFE2A86A))
                nearLeg(72f, 108f, 16f, 42f, c(0xFFE2A86A), a, 80f, 112f, hind = true, body, Foot.PAW)
                nearLeg(130f, 108f, 16f, 42f, c(0xFFE2A86A), b, 138f, 112f, hind = false, body, Foot.PAW)
                circle(150f, 74f, 30f, c(0xFFE2A86A))
                ellipse(176f, 84f, 16f, 12f, c(0xFFF6D2A6))
                svg("M174 94 q2 16 10 12 q1 -8 -2 -14", c(0xFFFF8FA3), sw = 3f)
                ellipse(190f, 79f, 6f, 5f, Ink, sw = 0f)
                svg("M132 50 q-18 6 -14 36 q14 0 20 -16 z", c(0xFF8A5A34))
                eye(157f, 66f, 5f, 158.5f, 64.5f, 1.8f)
            }
        }
        CAT -> {
            shadow(100f, 160f, 62f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 102f, 54f, 28f))
                svg("M40 94 q-28 -14 -14 -48", null, sw = 12f)
                svg("M40 94 q-28 -14 -14 -48", null, sw = 5f, stroke = c(0xFFF4A340))
                farLeg(58f, 108f, 14f, 42f, c(0xFFD98A2E), b, 65f, 112f, hind = true, Foot.PAW)
                farLeg(116f, 108f, 14f, 42f, c(0xFFD98A2E), a, 123f, 112f, hind = false, Foot.PAW)
                ellipse(92f, 102f, 54f, 28f, c(0xFFF4A340))
                val stripe = c(0xFFC76A12)
                svg("M80 76 q4 10 0 20", null, sw = 4f, stroke = stripe)
                svg("M96 74 q4 10 0 20", null, sw = 4f, stroke = stripe)
                svg("M112 78 q4 10 0 18", null, sw = 4f, stroke = stripe)
                nearLeg(72f, 108f, 14f, 42f, c(0xFFF4A340), a, 79f, 112f, hind = true, body, Foot.PAW)
                nearLeg(128f, 108f, 14f, 42f, c(0xFFF4A340), b, 135f, 112f, hind = false, body, Foot.PAW)
                svg("M134 60 l4 -26 l18 18 z", c(0xFFF4A340))
                svg("M158 52 l14 -20 l6 26 z", c(0xFFF4A340))
                circle(154f, 78f, 28f, c(0xFFF4A340))
                ellipse(172f, 88f, 12f, 9f, c(0xFFFFF1D0))
                eye(162f, 72f, 5f, 163.5f, 70.5f, 1.8f)
                svg("M179 82 l7 0 l-3.5 4 z", c(0xFFFF8FA3), sw = 2f)
                svg("M178 92 l18 -4", null, sw = 2f)
                svg("M178 96 l18 2", null, sw = 2f)
            }
        }
        PIG -> {
            shadow(100f, 160f, 62f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 98f, 56f, 40f))
                val curl = "M40 92 q-14 -2 -16 -12 q-1 -9 7 -9 q7 1 5 8 q-2 6 -9 4"
                svg(curl, null, sw = 10f)
                svg(curl, null, sw = 4.5f, stroke = c(0xFFFF9DB5))
                farLeg(56f, 118f, 18f, 30f, c(0xFFF28AA5), b, 65f, 122f, hind = true, Foot.HOOF, c(0xFFC9637F))
                farLeg(110f, 118f, 18f, 30f, c(0xFFF28AA5), a, 119f, 122f, hind = false, Foot.HOOF, c(0xFFC9637F))
                ellipse(92f, 98f, 56f, 40f, c(0xFFFFB8C9))
                nearLeg(70f, 118f, 18f, 30f, c(0xFFFFB8C9), a, 79f, 122f, hind = true, body, Foot.HOOF, c(0xFFC9637F))
                nearLeg(124f, 118f, 18f, 30f, c(0xFFFFB8C9), b, 133f, 122f, hind = false, body, Foot.HOOF, c(0xFFC9637F))
                svg("M128 60 l4 -26 l18 20 z", c(0xFFFF9DB5))
                circle(146f, 86f, 32f, c(0xFFFFB8C9))
                ellipse(176f, 94f, 14f, 11f, c(0xFFFF9DB5))
                ellipse(172f, 94f, 2.5f, 4f, Ink, sw = 0f)
                ellipse(181f, 94f, 2.5f, 4f, Ink, sw = 0f)
                eye(152f, 76f, 5f, 154f, 74f, 1.8f)
                ellipse(146f, 100f, 7f, 4.5f, c(0xFFFF7A9A), sw = 0f)
            }
        }
        MONKEY -> {
            shadow(100f, 160f, 58f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 104f, 46f, 32f))
                svg("M48 104 q-32 2 -28 -30 q4 -16 18 -8", null, sw = 12f)
                svg("M48 104 q-32 2 -28 -30 q4 -16 18 -8", null, sw = 5f, stroke = c(0xFF9A6A3E))
                farLeg(64f, 112f, 15f, 38f, c(0xFF7E522C), b, 71f, 116f, hind = true, Foot.PAW, c(0xFFE3BE91))
                farLeg(110f, 112f, 15f, 38f, c(0xFF7E522C), a, 117f, 116f, hind = false, Foot.PAW, c(0xFFE3BE91))
                ellipse(92f, 104f, 46f, 32f, c(0xFF9A6A3E))
                nearLeg(78f, 112f, 15f, 38f, c(0xFF9A6A3E), a, 85f, 116f, hind = true, body, Foot.PAW, c(0xFFF2D2A9))
                nearLeg(124f, 112f, 15f, 38f, c(0xFF9A6A3E), b, 131f, 116f, hind = false, body, Foot.PAW, c(0xFFF2D2A9))
                circle(132f, 72f, 13f, c(0xFF9A6A3E))
                circle(132f, 72f, 6f, c(0xFFF2D2A9), sw = 0f)
                circle(154f, 76f, 28f, c(0xFF9A6A3E))
                ellipse(164f, 82f, 20f, 17f, c(0xFFF2D2A9))
                eye(166f, 74f, 4.5f, 167.5f, 72.5f, 1.6f)
                svg("M166 90 q7 5 14 0", null, sw = 3f)
            }
        }
        PENGUIN -> {
            shadow(102f, 160f, 48f, 7f)
            translate(0f, bob) {
                ellipse(88f, 150f, 15f, 7f, c(0xFFFF9F1C), a, 92f, 144f)
                ellipse(118f, 150f, 15f, 7f, c(0xFFFF9F1C), b, 114f, 144f)
                ellipse(100f, 94f, 42f, 56f, c(0xFF2E3A59))
                ellipse(114f, 104f, 26f, 40f, White)
                svg("M66 86 q-24 22 -12 42 q16 -14 18 -32 z", c(0xFF2E3A59), rot = b, px = 68f, py = 88f)
                svg("M136 74 l20 6 l-20 7 z", c(0xFFFFB020))
                eye(122f, 66f, 5f, 123.5f, 64.5f, 1.8f)
                ellipse(126f, 84f, 7f, 4f, c(0xFFFFB3C7), sw = 0f)
            }
        }
        KANGAROO -> {
            shadow(100f, 160f, 62f, 7f)
            translate(0f, bob) {
                svg("M70 124 q-38 8 -54 30", null, sw = 14f)
                svg("M70 124 q-38 8 -54 30", null, sw = 6f, stroke = c(0xFFD59A5B))
                ellipse(84f, 150f, 26f, 8f, c(0xFFB97D40), a, 90f, 146f)
                ellipse(134f, 26f, 7f, 17f, c(0xFFD59A5B), rot = -12f)
                ellipse(98f, 102f, 34f, 46f, c(0xFFD59A5B), rot = 28f)
                ellipse(110f, 110f, 16f, 26f, c(0xFFF1D2A8), rot = 28f, sw = 4f)
                ellipse(84f, 128f, 24f, 17f, c(0xFFD59A5B))
                ellipse(106f, 152f, 26f, 8f, c(0xFFD59A5B), b, 98f, 146f)
                svg("M128 90 q14 12 24 4", null, sw = 6f)
                ellipse(148f, 26f, 7f, 17f, c(0xFFD59A5B), rot = 12f)
                ellipse(146f, 54f, 22f, 18f, c(0xFFD59A5B))
                ellipse(166f, 60f, 13f, 9f, c(0xFFF1D2A8))
                eye(150f, 49f, 4.5f, 151.5f, 47.5f, 1.6f)
                dot(177f, 57f, 3f)
            }
        }
        ELEPHANT -> {
            shadow(100f, 160f, 66f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 96f, 56f, 40f))
                svg("M38 92 q-12 10 -8 24", null, sw = 4f)
                farLeg(56f, 108f, 22f, 42f, c(0xFF93A0B1), b, 67f, 112f, hind = true, Foot.PAD, c(0xFFB4BFCC))
                farLeg(114f, 108f, 22f, 42f, c(0xFF93A0B1), a, 125f, 112f, hind = false, Foot.PAD, c(0xFFB4BFCC))
                ellipse(92f, 96f, 56f, 40f, c(0xFFA9B4C2))
                nearLeg(72f, 110f, 22f, 42f, c(0xFFA9B4C2), a, 83f, 114f, hind = true, body, Foot.PAD, c(0xFFC9D1DB))
                nearLeg(130f, 110f, 22f, 42f, c(0xFFA9B4C2), b, 141f, 114f, hind = false, body, Foot.PAD, c(0xFFC9D1DB))
                svg("M170 88 q20 20 10 46 q-3 7 -12 4 q8 -24 -10 -42 z", c(0xFFA9B4C2))
                circle(152f, 76f, 32f, c(0xFFA9B4C2))
                svg("M164 98 q6 10 18 8", White, sw = 3f)
                ellipse(136f, 80f, 20f, 26f, c(0xFF93A0B1))
                ellipse(136f, 80f, 10f, 15f, c(0xFFF4B8C8), sw = 0f)
                eye(164f, 68f, 4.5f, 165.5f, 66.5f, 1.6f)
            }
        }
        DUCK -> {
            shadow(100f, 160f, 54f, 7f)
            translate(0f, bob) {
                rotate(a, Offset(92f, 134f)) {
                    svg("M92 134 l-2 16", null, stroke = c(0xFFE8892A))
                    ellipse(94f, 152f, 12f, 5f, c(0xFFFF9F1C))
                }
                rotate(b, Offset(112f, 134f)) {
                    svg("M112 134 l2 16", null, stroke = c(0xFFE8892A))
                    ellipse(118f, 152f, 12f, 5f, c(0xFFFF9F1C))
                }
                svg("M46 94 l-20 -16 l26 4 z", c(0xFFC9CFD8))
                ellipse(96f, 106f, 50f, 32f, c(0xFFD7DCE3))
                circle(128f, 108f, 22f, c(0xFFB5764A))
                svg("M70 100 q26 -18 48 2 q-22 26 -48 -2 z", c(0xFFF4F6F9))
                circle(140f, 66f, 24f, c(0xFF2F9E6E))
                svg("M124 86 q16 8 32 0", null, sw = 6f, stroke = White)
                svg("M160 64 q22 0 24 8 q-4 7 -24 4 z", c(0xFFFFC93C))
                eye(147f, 60f, 4.5f, 148.5f, 58.5f, 1.6f)
            }
        }
        SNAIL -> {
            shadow(104f, 160f, 72f, 6f)
            rotate(a, Offset(166f, 102f)) {
                svg("M166 102 l-6 -30", null)
                circle(160f, 70f, 7f, c(0xFFF3D9B1))
            }
            rotate(b, Offset(178f, 102f)) {
                svg("M178 102 l6 -28", null)
                circle(184f, 72f, 7f, c(0xFFF3D9B1))
            }
            svg("M30 150 q0 -12 30 -12 l100 0 q8 -40 22 -30 q12 10 4 42 z", c(0xFFF3D9B1))
            dot(174f, 120f, 3.5f)
            svg("M176 132 q5 4 10 0", null, sw = 3f)
            translate(0f, bob) {
                circle(100f, 98f, 42f, c(0xFFE89A4C))
                svg("M100 98 m-6 0 a6 6 0 1 1 12 0 a12 12 0 1 1 -24 0 a20 20 0 1 1 40 0 a28 28 0 1 1 -56 0", null, sw = 4f)
            }
        }
        CHICKEN -> {
            shadow(100f, 160f, 50f, 7f)
            translate(0f, bob) {
                svg("M90 138 l-6 18 m0 0 l-8 0 m8 0 l8 0", null, stroke = c(0xFFE8892A), rot = a, px = 90f, py = 138f)
                svg("M112 138 l4 18 m0 0 l-8 0 m8 0 l8 0", null, stroke = c(0xFFE8892A), rot = b, px = 112f, py = 138f)
                svg("M58 92 q-28 -18 -22 -40 q18 6 26 20 q-4 -24 12 -32 q8 16 4 34 z", White)
                ellipse(100f, 104f, 46f, 40f, White)
                svg("M122 46 q2 -16 12 -8 q6 -14 14 -2 q10 -6 10 10 z", c(0xFFFF5A4A))
                circle(138f, 68f, 26f, White)
                svg("M162 66 l18 7 l-18 7 z", c(0xFFFFC93C))
                ellipse(160f, 90f, 6f, 9f, c(0xFFFF5A4A))
                eye(146f, 62f, 4.5f, 148f, 60f, 1.6f)
                svg("M74 100 q24 -16 44 4 q-20 24 -44 -4 z", c(0xFFECE6F3), rot = p.wing, px = 96f, py = 100f)
            }
        }
        SKUNK -> {
            shadow(100f, 160f, 58f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(96f, 108f, 44f, 30f))
                svg("M62 108 q-50 -6 -46 -56 q6 -36 44 -30 q26 6 18 34 q-8 -6 -16 -2 q-10 6 -4 22 q6 14 16 18 z", c(0xFF3A3348))
                svg("M52 98 q-30 -10 -24 -44 q6 -22 28 -18 q12 4 10 16 q-14 -2 -20 8 q-6 14 6 28 z", White, sw = 0f)
                farLeg(66f, 116f, 14f, 34f, c(0xFF2E2838), b, 73f, 120f, hind = true, Foot.PAW)
                farLeg(112f, 116f, 14f, 34f, c(0xFF2E2838), a, 119f, 120f, hind = false, Foot.PAW)
                ellipse(96f, 108f, 44f, 30f, c(0xFF4A4458))
                svg("M62 96 q36 -28 84 -12", null, sw = 11f, stroke = White)
                nearLeg(80f, 116f, 14f, 34f, c(0xFF4A4458), a, 87f, 120f, hind = true, body, Foot.PAW)
                nearLeg(126f, 116f, 14f, 34f, c(0xFF4A4458), b, 133f, 120f, hind = false, body, Foot.PAW)
                circle(139f, 66f, 9f, c(0xFF2E2838))
                circle(153f, 88f, 25f, c(0xFF4A4458))
                svg("M147 64 q6 12 6 24", null, sw = 7f, stroke = White)
                ellipse(173f, 96f, 12f, 9f, c(0xFFE9E4F0))
                dot(184f, 94f, 4.5f, c(0xFFFF8FA8))
                dot(161f, 86f, 5.5f, White); dot(161f, 86f, 3.5f, c(0xFF1E1A24)); dot(162.3f, 84.7f, 1.4f, White)
            }
        }
        HEDGEHOG -> {
            shadow(100f, 160f, 56f, 7f)
            translate(0f, bob) {
                // 몸과 색이 달라서 다리를 모두 가시 치마 아래에 숨겨 아래쪽만 보이게 한다
                farLeg(66f, 124f, 14f, 26f, c(0xFFC9A27A), b, 73f, 128f, hind = true, Foot.PAW)
                farLeg(104f, 124f, 14f, 26f, c(0xFFC9A27A), a, 111f, 128f, hind = false, Foot.PAW)
                farLeg(80f, 124f, 14f, 26f, c(0xFFE6C49A), a, 87f, 128f, hind = true, Foot.PAW)
                farLeg(124f, 124f, 14f, 26f, c(0xFFE6C49A), b, 131f, 128f, hind = false, Foot.PAW)
                svg("M34 132 L24 114 L40 110 L28 92 L48 94 L42 74 L60 82 L62 60 L78 74 L88 54 L98 74 L112 58 L116 78 L132 68 L130 88 L146 86 L150 104 L154 132 Z", c(0xFF7A5236))
                svg("M54 128 q4 -40 46 -46 q34 2 42 46 z", c(0xFF9C6B46), sw = 0f)
                circle(140f, 90f, 8f, c(0xFFF0D2A8))
                ellipse(150f, 112f, 28f, 24f, c(0xFFF0D2A8))
                svg("M168 104 q22 4 18 16 q-8 6 -22 0 z", c(0xFFF0D2A8))
                dot(186f, 113f, 4.5f)
                eye(158f, 102f, 4.5f, 159.5f, 100.5f, 1.6f)
                ellipse(156f, 116f, 6f, 3.5f, c(0xFFFF9DB5), sw = 0f)
            }
        }
        CAPYBARA -> {
            shadow(100f, 160f, 66f, 7f)
            translate(0f, bob) {
                val body = listOf(roundBox(36f, 76f, 116f, 60f, 30f))
                farLeg(58f, 118f, 16f, 32f, c(0xFF8E6440), b, 66f, 122f, hind = true, Foot.PAW)
                farLeg(104f, 118f, 16f, 32f, c(0xFF8E6440), a, 112f, 122f, hind = false, Foot.PAW)
                rrect(36f, 76f, 116f, 60f, 30f, c(0xFFB07D52))
                nearLeg(72f, 118f, 16f, 32f, c(0xFFB07D52), a, 80f, 122f, hind = true, body, Foot.PAW)
                nearLeg(120f, 118f, 16f, 32f, c(0xFFB07D52), b, 128f, 122f, hind = false, body, Foot.PAW)
                ellipse(130f, 58f, 8f, 6f, c(0xFF8E6440))
                rrect(118f, 56f, 66f, 50f, 20f, c(0xFFB07D52))
                rrect(164f, 68f, 24f, 34f, 11f, c(0xFF8E6440))
                ellipse(181f, 77f, 2.2f, 3.2f, Ink, sw = 0f)
                svg("M142 72 q6 5 12 0", null, sw = 4f)
                svg("M168 98 q6 4 12 0", null, sw = 3f)
            }
        }
        SLOTH -> {
            shadow(100f, 160f, 60f, 7f)
            translate(0f, bob) {
                val body = listOf(oval(92f, 104f, 52f, 32f))
                farLeg(60f, 114f, 14f, 38f, c(0xFF8A7468), b, 67f, 118f, hind = true, Foot.PAW)
                // 머리 쪽 앞발 두 개에만 발톱
                farLeg(108f, 114f, 14f, 38f, c(0xFF8A7468), a, 115f, 118f, hind = false, Foot.PAW, claws = true)
                ellipse(92f, 104f, 52f, 32f, c(0xFFA89088))
                svg("M52 112 l-6 10 l10 -2 l-2 10 l8 -6", null, sw = 3f)
                nearLeg(76f, 110f, 14f, 44f, c(0xFFA89088), a, 83f, 114f, hind = true, body, Foot.PAW)
                nearLeg(122f, 110f, 14f, 44f, c(0xFFA89088), b, 129f, 114f, hind = false, body, Foot.PAW, claws = true)
                circle(152f, 80f, 30f, c(0xFFA89088))
                svg("M129 80 q2 -21 25 -21 q25 0 25 21 q0 19 -25 23 q-23 -4 -25 -23 z", c(0xFFF1EBE8))
                ellipse(141f, 85f, 10f, 5.5f, c(0xFF6D4C41), rot = -28f, sw = 0f)
                ellipse(167f, 85f, 10f, 5.5f, c(0xFF6D4C41), rot = 28f, sw = 0f)
                svg("M137 84 q4 -4.5 8 0", null, sw = 2.6f, stroke = c(0xFF1E1A24))
                svg("M163 84 q4 -4.5 8 0", null, sw = 2.6f, stroke = c(0xFF1E1A24))
                svg("M148 89 h12 q2 0 0 3 l-5 5 q-1 1 -2 0 l-5 -5 q-2 -3 0 -3 z", c(0xFF1E1A24), sw = 0f)
                svg("M143 99 q11 7 22 0", null, sw = 3f, stroke = c(0xFF1E1A24))
            }
        }
        HAMSTER -> {
            val sw = 4.5f
            shadow(100f, 160f, 54f, 7f)
            translate(0f, bob) {
                ellipse(72f, 146f, 11f, 7f, c(0xFFF2C9A0), rot = b, px = 72f, py = 138f, sw = sw)
                ellipse(104f, 148f, 11f, 7f, c(0xFFF2C9A0), rot = a, px = 104f, py = 140f, sw = sw)
                ellipse(88f, 120f, 46f, 32f, c(0xFFF3DCBC), sw = sw)
                ellipse(98f, 134f, 26f, 14f, c(0xFFFFF8EE), sw = 0f)
                circle(112f, 56f, 13f, c(0xFFEACBA6), sw = sw); dot(112f, 57f, 6.5f, c(0xFFD9A28A))
                circle(162f, 54f, 13f, c(0xFFEACBA6), sw = sw); dot(162f, 55f, 6.5f, c(0xFFD9A28A))
                ellipse(139f, 92f, 44f, 40f, c(0xFFF6E6CF), sw = sw)
                ellipse(146f, 106f, 17f, 11f, c(0xFFFFF8EE), sw = 0f)
                ellipse(119f, 104f, 8f, 4.5f, c(0xFFFFB3BF), sw = 0f)
                ellipse(173f, 102f, 8f, 4.5f, c(0xFFFFB3BF), sw = 0f)
                eye(127f, 90f, 4.5f, 128.5f, 88.5f, 1.5f)
                eye(165f, 88f, 4.5f, 166.5f, 86.5f, 1.5f)
                ellipse(146f, 100f, 5f, 3.6f, c(0xFFF28FA0), sw = 2.5f)
                svg("M139 106 q3.5 4 7 0 q3.5 4 7 0", null, sw = 2.5f)
                ellipse(127f, 132f, 7f, 6f, c(0xFFFFF3E2), rot = a, px = 127f, py = 126f, sw = 3.5f)
                ellipse(153f, 132f, 7f, 6f, c(0xFFFFF3E2), rot = b, px = 153f, py = 126f, sw = 3.5f)
            }
        }
        PANDA -> {
            val black = c(0xFF2E2838)
            shadow(100f, 160f, 60f, 7f)
            translate(0f, bob) {
                val far = c(0xFF1E1A28)
                roundLeg(60f, 116f, 18f, 34f, far, b, 69f, 120f, hind = true)
                roundLeg(106f, 116f, 18f, 34f, far, a, 115f, 120f, hind = false)
                ellipse(88f, 106f, 50f, 34f, White)
                svg("M100 78 q22 -6 32 8 l-6 46 q-14 4 -26 -4 z", black)
                // 몸과 색이 달라서, 가까운 다리는 몸 위에 테두리까지 통째로 그리고 위쪽을 둥글게 마무리
                roundLeg(74f, 116f, 18f, 34f, black, a, 83f, 120f, hind = true)
                roundLeg(120f, 116f, 18f, 34f, black, b, 129f, 120f, hind = false)
                circle(125f, 56f, 13f, black); circle(177f, 56f, 13f, black)
                ellipse(151f, 84f, 36f, 31f, White)
                ellipse(138f, 84f, 11.5f, 9f, black, rot = -25f, sw = 0f)
                ellipse(164f, 84f, 11.5f, 9f, black, rot = 25f, sw = 0f)
                dot(140f, 81f, 4.2f, White); dot(162f, 81f, 4.2f, White)
                svg("M146 94 q5 -3 10 0 q-1 5 -5 6 q-4 -1 -5 -6 z", Ink, sw = 2f)
                svg("M143 103 q8 7 16 0", null, sw = 3f)
                svg("M148 106 q3 8 6 0 z", c(0xFFE5484D), sw = 2f)
                ellipse(129f, 98f, 5f, 3f, c(0xFFFFB3BF), sw = 0f)
                ellipse(173f, 98f, 5f, 3f, c(0xFFFFB3BF), sw = 0f)
            }
        }
    }
}
