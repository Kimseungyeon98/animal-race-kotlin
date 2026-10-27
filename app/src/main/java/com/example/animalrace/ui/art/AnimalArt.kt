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

/** 머리 꼭대기 (200×170 좌표): 1등 왕관 같은 것을 얹는 자리 */
internal fun headTop(animal: Animal): Offset = when (animal) {
    HORSE -> Offset(158f, 36f)
    CHEETAH -> Offset(160f, 62f)
    RABBIT -> Offset(142f, 50f)
    TURTLE -> Offset(164f, 84f)
    DOG -> Offset(150f, 46f)
    CAT -> Offset(155f, 52f)
    PIG -> Offset(146f, 62f)
    MONKEY -> Offset(154f, 50f)
    PENGUIN -> Offset(108f, 42f)
    KANGAROO -> Offset(148f, 42f)
    ELEPHANT -> Offset(152f, 46f)
    DUCK -> Offset(140f, 44f)
    SNAIL -> Offset(100f, 58f)
    CHICKEN -> Offset(136f, 40f)
    SKUNK -> Offset(154f, 65f)
    HEDGEHOG -> Offset(150f, 90f)
    CAPYBARA -> Offset(152f, 54f)
    SLOTH -> Offset(152f, 52f)
    HAMSTER -> Offset(148f, 66f)
    PANDA -> Offset(151f, 55f)
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

private val HamFur = Color(0xFFF2C38E)
private val HamWhite = Color(0xFFFFF8EE)
private val HamFeet = Color(0xFFF2C9A0)
private val HamEarIn = Color(0xFFD9A28A)
private val HamNose = Color(0xFFF28FA0)

/** 햄스터 (시안 "햄스터 다듬기" B): 다른 동물처럼 옆모습. 머리가 몸에 묻힌 통통한 덩어리 + 볼록한 볼주머니 */
private fun DrawScope.hamsterSide(a: Float, b: Float, bob: Float) {
    val sw = 4.5f
    val far = c(0xFFE0A970)
    shadow(100f, 160f, 58f, 7f)
    translate(0f, bob) {
        // 짧은 다리 네 개 (같은 모양, 먼 쪽은 진하게). 몸 뒤에 그려 아래만 살짝 보이게
        fun leg(x: Float, fill: Color, rot: Float) = rotate(rot, Offset(x, 134f)) {
            rrect(x - 7f, 128f, 14f, 22f, 7f, fill)
            ellipse(x + 3f, 151f, 9f, 4.5f, HamFeet, sw = 3.5f)
        }
        leg(80f, far, b); leg(136f, far, a)
        leg(68f, HamFur, a); leg(124f, HamFur, b)
        circle(45f, 112f, 5f, HamFeet, sw = 3.5f)
        circle(124f, 72f, 9f, far, sw = sw)
        val body = oval(100f, 110f, 58f, 37f)
        drawPath(body, HamFur)
        clipPath(body) {
            drawOval(HamWhite, Offset(66f, 114f), Size(84f, 44f))
            // 등의 진한 털 무늬
            svg("M56 92 q30 -26 74 -12", null, sw = 9f, stroke = far)
        }
        paint(body, null, sw, Ink)
        ellipse(148f, 96f, 32f, 29f, HamFur, sw = sw)
        // 볼주머니: 볼록한 흰 볼
        ellipse(156f, 110f, 17f, 13f, HamWhite, sw = 0f)
        circle(136f, 68f, 10f, HamFur, sw = sw); dot(136f, 69f, 5f, HamEarIn)
        eye(160f, 90f, 5f, 161.6f, 88.3f, 1.7f)
        ellipse(178f, 98f, 4.2f, 3.2f, HamNose, sw = 2.2f)
        svg("M171 107 q3 3 6 0", null, sw = 2.2f)
        svg("M176 102 l13 -3 M176 105 l13 2", null, sw = 1.6f)
    }
}

/** 오리 (시안 "오리 · 닭 다듬기" C안): 들린 꼬리·말린 깃·둥근 머리·날개 + 흰 몸·머리, 주황 부리·발. 머리·목·몸은 한 도형 */
private fun DrawScope.duck(a: Float, b: Float, bob: Float, flap: Float) {
    val orange = c(0xFFFF7A1C)
    val wing = c(0xFFEDEAF1)
    val feather = c(0xFFD3CCD9)
    shadow(100f, 160f, 54f, 7f)
    translate(0f, bob) {
        fun leg(x: Float, rot: Float) = rotate(rot, Offset(x, 132f)) {
            svg("M$x 132 l-1 15", null, sw = 9f)
            svg("M$x 132 l-1 15", null, sw = 4f, stroke = c(0xFFE8892A))
            svg("M${x - 9} 151 q10 -8 24 -1 q-12 7 -24 1 z", orange, sw = 3.5f)
        }
        leg(92f, a); leg(114f, b)
        svg("M52 96 q-16 -4 -26 -20 q16 0 30 10 z", White)
        svg("M40 80 q-6 -8 2 -12", null, sw = 3.5f)
        // 몸 + 목 + 머리를 하나로 합쳐 윤곽선을 한 번만 (흰색끼리 겹친 선이 보이지 않게)
        val whole = Path().apply {
            op(oval(96f, 106f, 50f, 32f), oval(140f, 66f, 24f, 24f), androidx.compose.ui.graphics.PathOperation.Union)
            op(this, oval(132f, 86f, 15f, 14f), androidx.compose.ui.graphics.PathOperation.Union)
        }
        drawPath(whole, White)
        clipPath(whole) { drawOval(c(0xFFF1EEF4), Offset(52f, 116f), Size(96f, 30f)) }
        paint(whole, null, 5f, Ink)
        svg("M124 92 Q96 80 58 104 Q92 120 118 114 Q130 104 124 92 Z", wing, sw = 4f, rot = flap, px = 96f, py = 100f)
        svg("M74 106 q14 6 30 4 M88 98 q14 -3 26 1", null, sw = 2.5f, stroke = feather, rot = flap, px = 96f, py = 100f)
        svg("M159 61 q24 -3 28 8 q-3 9 -28 6 z", orange, sw = 4f)
        svg("M162 69 q12 2 22 0", null, sw = 2f, stroke = c(0xFFD95F10))
        dot(171f, 65f, 1.8f)
        eye(147f, 60f, 4.5f, 148.5f, 58.5f, 1.6f)
    }
}

/** 둥근 끝 가시 왕관: 타원 (cx,cy,rx,ry)의 [from]~[to]도 구간에 [n]개, 끝은 out배 바깥 */
private fun spikeCrown(cx: Float, cy: Float, rx: Float, ry: Float, from: Float, to: Float, n: Int, out: Float): Path {
    fun pt(deg: Float, k: Float): Offset {
        val t = Math.toRadians(deg.toDouble())
        return Offset(cx + rx * k * kotlin.math.cos(t).toFloat(), cy - ry * k * kotlin.math.sin(t).toFloat())
    }
    fun lerp(p: Offset, q: Offset, f: Float) = Offset(p.x + (q.x - p.x) * f, p.y + (q.y - p.y) * f)
    return Path().apply {
        val step = (to - from) / n
        var base = pt(from, 1f)
        moveTo(cx, cy); lineTo(base.x, base.y)
        for (i in 0 until n) {
            val tip = pt(from + step * (i + 0.5f), out)
            val next = pt(from + step * (i + 1), 1f)
            val p1 = lerp(base, tip, 0.72f); val p2 = lerp(tip, next, 0.28f)
            lineTo(p1.x, p1.y); quadraticTo(tip.x, tip.y, p2.x, p2.y); lineTo(next.x, next.y)
            base = next
        }
        close()
    }
}

private val HogSpike = Color(0xFF7E5236)
private val HogSpikeIn = Color(0xFFA8754C)
private val HogFace = Color(0xFFF6DDB6)
private val HogFeet = Color(0xFFE2B98E)

/** 고슴도치 (시안 "고슴도치 더 귀엽게", 4.3): 통통한 몸 + 둥근 끝 가시 두 겹 + 큰 동그란 얼굴·눈, 짧은 다리 */
private fun DrawScope.hedgehogCute(a: Float, b: Float, bob: Float) {
    val sw = 4.5f
    val U = androidx.compose.ui.graphics.PathOperation.Union
    shadow(100f, 160f, 56f, 7f)
    translate(0f, bob) {
        fun leg(x: Float, fill: Color, rot: Float) = rotate(rot, Offset(x, 134f)) {
            rrect(x - 7f, 130f, 14f, 20f, 7f, fill)
            ellipse(x + 3f, 151f, 9f, 4.5f, HogFeet, sw = 3.5f)
        }
        leg(80f, c(0xFFC99A6E), b); leg(128f, c(0xFFC99A6E), a)
        // 몸 + 바깥 가시를 한 도형으로 (윤곽선 한 번)
        val whole = Path().apply { op(spikeCrown(94f, 112f, 54f, 40f, 200f, 32f, 9, 1.32f), oval(98f, 120f, 52f, 28f), U) }
        drawPath(whole, HogSpike)
        clipPath(whole) {
            // 안쪽 가시 (밝은 갈색, 반 칸 엇갈림) + 아래 크림색 배
            drawPath(spikeCrown(96f, 118f, 42f, 30f, 192f, 40f, 7, 1.3f), HogSpikeIn)
            drawOval(HogFace, Offset(54f, 128f), Size(96f, 34f))
        }
        paint(whole, null, sw, Ink)
        leg(66f, HogFeet, a); leg(114f, HogFeet, b)
        // 얼굴: 크고 동그랗게 + 앞으로 살짝 나온 주둥이
        val face = Path().apply { op(oval(148f, 110f, 27f, 26f), oval(170f, 117f, 15f, 10f), U) }
        circle(134f, 86f, 7.5f, HogFace, sw = sw); dot(134f, 87f, 3.6f, c(0xFFE5A98F))
        paint(face, HogFace, sw, Ink)
        dot(185f, 115f, 5.5f); dot(183.3f, 113.2f, 1.6f, White)
        // 큰 눈 + 반짝이 두 개
        dot(158f, 104f, 6.5f); dot(160f, 101.5f, 2.3f, White); dot(155.8f, 106.6f, 1.1f, White)
        svg("M171 124 q5 4 10 0", null, sw = 2.4f)
    }
}

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
                val gold = c(0xFFF5C04A)
                val spot = c(0xFF6B4A1E)
                val body = listOf(oval(94f, 98f, 60f, 25f))
                // 긴 꼬리: 끝으로 갈수록 진한 고리 무늬 + 흰 끝
                val tail = "M38 94 q-28 2 -30 -30"
                svg(tail, null, sw = 12f)
                svg(tail, null, sw = 5f, stroke = gold)
                dot(18f, 82f, 3f, spot); dot(11f, 72f, 3f, spot)
                circle(8f, 63f, 3.5f, spot, sw = 3f)
                farLeg(56f, 104f, 14f, 46f, c(0xFFDDA437), b, 63f, 108f, hind = true, Foot.PAW)
                farLeg(120f, 104f, 14f, 46f, c(0xFFDDA437), a, 127f, 108f, hind = false, Foot.PAW)
                ellipse(94f, 98f, 60f, 25f, gold)
                // 밝은 배 + 작은 점무늬 여럿
                svg("M46 108 q46 18 96 2", null, sw = 5f, stroke = c(0xFFFFE3A6))
                for ((x, y, r) in listOf(Triple(60f, 92f, 4f), Triple(76f, 86f, 3f), Triple(90f, 98f, 3.5f), Triple(104f, 88f, 4f),
                    Triple(118f, 96f, 3f), Triple(130f, 86f, 3f), Triple(70f, 104f, 3f), Triple(48f, 98f, 3f), Triple(112f, 106f, 2.5f))) dot(x, y, r, spot)
                nearLeg(68f, 104f, 14f, 46f, gold, a, 75f, 108f, hind = true, body, Foot.PAW)
                nearLeg(132f, 104f, 14f, 46f, gold, b, 139f, 108f, hind = false, body, Foot.PAW)
                dot(74f, 128f, 2.5f, spot); dot(136f, 130f, 2.5f, spot)
                // 작고 날렵한 머리, 뒤로 붙은 작은 귀
                circle(146f, 66f, 6.5f, c(0xFFDDA437), sw = 4f)
                circle(158f, 63f, 7f, gold, sw = 4f)
                ellipse(160f, 80f, 23f, 19f, gold)
                // 주둥이는 테두리 없이 머리 안쪽에 (선이 엉키지 않게)
                ellipse(172f, 88f, 10f, 7.5f, c(0xFFFFF1D0), sw = 0f)
                eye(163f, 75f, 4.5f, 164.5f, 73.5f, 1.6f)
                // 치타의 눈물 줄무늬: 눈 앞쪽에서 주둥이 가장자리까지
                svg("M166 79 q0 8 4 13", null, sw = 3.5f)
                dot(180f, 84f, 3.2f)
                svg("M173 93 q4 3 8 -1", null, sw = 2f)
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
                val body = listOf(oval(92f, 100f, 58f, 40f))
                // 꼬리: 열린 돼지꼬리 (닫힌 고리는 눈처럼 보여서)
                val curl = "M38 92 q-8 -10 -15 -5 q-6 5 1 9 q7 3 -2 10 q-4 3 -8 1"
                svg(curl, null, sw = 9f)
                svg(curl, null, sw = 4f, stroke = c(0xFFFF9DB5))
                farLeg(56f, 120f, 18f, 30f, c(0xFFF28AA5), b, 65f, 124f, hind = true, Foot.HOOF, c(0xFFC9637F))
                farLeg(110f, 120f, 18f, 30f, c(0xFFF28AA5), a, 119f, 124f, hind = false, Foot.HOOF, c(0xFFC9637F))
                // 먼 쪽 귀 (머리 뒤, 진하게)
                svg("M126 68 Q126 52 134 42 Q142 52 146 64 Z", c(0xFFF28AA5))
                ellipse(92f, 100f, 58f, 40f, c(0xFFFFB8C9))
                // 등 윤기 · 배 그늘
                svg("M58 74 q26 -14 56 -8", null, sw = 5f, stroke = c(0xFFFFD6E0))
                svg("M54 120 q38 20 80 4", null, sw = 3f, stroke = c(0xFFF28AA5))
                nearLeg(70f, 120f, 18f, 30f, c(0xFFFFB8C9), a, 79f, 124f, hind = true, body, Foot.HOOF, c(0xFFC9637F))
                nearLeg(124f, 120f, 18f, 30f, c(0xFFFFB8C9), b, 133f, 124f, hind = false, body, Foot.HOOF, c(0xFFC9637F))
                // 머리: 목 없이 몸에 묻힌 둥근 얼굴
                ellipse(146f, 90f, 32f, 29f, c(0xFFFFB8C9))
                // 주둥이 (예전 그대로, 머리가 내려온 만큼만 아래로)
                ellipse(176f, 98f, 14f, 11f, c(0xFFFF9DB5))
                ellipse(172f, 98f, 2.5f, 4f, Ink, sw = 0f)
                ellipse(181f, 98f, 2.5f, 4f, Ink, sw = 0f)
                // 가까운 귀: 앞으로 접혀 내려온 귀
                // 가까운 귀: 앞으로 기운 세모 귀
                svg("M140 66 Q144 48 158 38 Q164 52 162 64 Z", c(0xFFFF9DB5))
                svg("M147 60 Q151 50 157 45", null, sw = 3f, stroke = c(0xFFF28AA5))
                eye(156f, 82f, 5.2f, 157.6f, 80f, 1.8f)
                // 입: 주둥이 아래로 비켜 보이게
                svg("M152 113 q7 6 15 1", null, sw = 3f)
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
                // 작은 꼬리: 몸 뒤 아래에 바짝 붙여 (몸보다 먼저 그려 끝만 보이게)
                svg("M72 132 l-14 12 l20 -2 z", c(0xFF2E3A59), sw = 4f)
                ellipse(100f, 94f, 42f, 56f, c(0xFF2E3A59))
                ellipse(114f, 104f, 26f, 40f, White)
                svg("M66 86 q-24 22 -12 42 q16 -14 18 -32 z", c(0xFF2E3A59), rot = b, px = 68f, py = 88f)
                svg("M136 74 l20 6 l-20 7 z", c(0xFFFFB020))
                eye(122f, 66f, 5f, 123.5f, 64.5f, 1.8f)
            }
        }
        KANGAROO -> {
            shadow(100f, 160f, 64f, 7f)
            translate(0f, bob) {
                val fur = c(0xFFD59A5B)
                val dark = c(0xFFB97D40)
                val cream = c(0xFFF1D2A8)
                // 굵게 시작해 가늘어지는 꼬리 (땅을 짚음)
                svg("M80 110 Q42 124 14 148 Q10 156 20 156 Q50 140 86 132 Z", fur)
                // 먼 쪽 긴 뒷발
                svg("M84 143 L128 145 Q138 147 134 154 L84 155 Q77 150 84 143 Z", dark, rot = b, px = 90f, py = 148f)
                // 먼 쪽 귀 · 팔
                ellipse(134f, 30f, 7f, 18f, dark, rot = -18f)
                svg("M128 84 q8 14 22 15", null, sw = 10f)
                svg("M128 84 q8 14 22 15", null, sw = 4.5f, stroke = dark)
                // 몸통 (앞으로 기운 상체) + 배·주머니
                ellipse(104f, 96f, 30f, 44f, fur, rot = 24f)
                ellipse(117f, 106f, 15f, 27f, cream, rot = 24f, sw = 0f)
                // 힘센 허벅지
                ellipse(86f, 124f, 29f, 22f, fur, rot = -12f)
                svg("M72 118 q10 -8 24 -6", null, sw = 3f, stroke = c(0xFFE8B880))
                // 가까운 긴 뒷발
                svg("M82 142 L132 144 Q143 146 139 154 L82 156 Q74 150 82 142 Z", fur, rot = a, px = 90f, py = 148f)
                // 머리: 길쭉한 주둥이 + 코 + 입
                ellipse(148f, 58f, 20f, 17f, fur)
                ellipse(167f, 66f, 15f, 10f, cream, rot = 14f)
                ellipse(181f, 64f, 4.2f, 3.2f, Ink, sw = 0f)
                svg("M170 74 q5 3 10 -1", null, sw = 2.2f)
                // 가까운 귀 (안쪽 분홍)
                ellipse(150f, 28f, 8f, 19f, fur, rot = 10f)
                ellipse(150f, 30f, 3.5f, 12f, c(0xFFF4B8A8), rot = 10f, sw = 0f)
                eye(153f, 52f, 5f, 154.6f, 50.2f, 1.7f)
                // 가까운 팔 + 앞발
                svg("M134 84 q8 14 22 14", null, sw = 11f)
                svg("M134 84 q8 14 22 14", null, sw = 5.5f, stroke = fur)
                circle(157f, 98f, 4.5f, fur, sw = 3f)
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
        DUCK -> duck(a, b, bob, p.wing)
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
                val yellow = c(0xFFFFB020)
                // 굵은 다리 + 발가락 세 개
                fun leg(x: Float, rot: Float) = rotate(rot, Offset(x, 134f)) {
                    svg("M$x 134 l-2 18", null, sw = 9f)
                    svg("M$x 134 l-2 18", null, sw = 4f, stroke = yellow)
                    svg("M${x - 2} 152 l-9 2 M${x - 2} 152 l9 2 M${x - 2} 152 l-1 5", null, sw = 6.5f)
                    svg("M${x - 2} 152 l-9 2 M${x - 2} 152 l9 2 M${x - 2} 152 l-1 5", null, sw = 2.5f, stroke = yellow)
                }
                leg(90f, a); leg(114f, b)
                // 꼬리: 채워진 둥근 깃털 세 개가 부채꼴로
                // 꼬리: 위쪽이 물결진 부채 한 덩어리 + 깃털 줄
                svg("M72 102 Q44 102 36 80 Q32 64 44 62 Q46 48 60 50 Q72 50 72 62 Q82 58 86 70 Q88 90 76 102 Z", White)
                svg("M48 74 q8 12 18 22 M62 62 q4 14 8 30", null, sw = 2.5f, stroke = c(0xFFD9D0C2))
                val body = oval(100f, 106f, 46f, 38f)
                paint(body, White, 5f, Ink)
                svg("M122 44 q2 -18 13 -9 q6 -16 15 -3 q11 -7 11 11 z", c(0xFFFF5A4A))
                circle(138f, 68f, 25f, White)
                // 목: 머리와 몸 사이 선을 흰색으로 덮어 이어지게
                ellipse(126f, 90f, 14f, 12f, White, sw = 0f)
                svg("M158 58 l26 11 l-26 11 z", c(0xFFFFC93C), sw = 3.5f)
                svg("M160 69 l16 0", null, sw = 2f)
                ellipse(158f, 90f, 6f, 9f, c(0xFFFF5A4A))
                eye(147f, 62f, 4.5f, 148.5f, 60.5f, 1.6f)
                // 날개: 둥근 날개 + 끝이 물결진 깃털
                svg("M124 94 Q98 80 62 104 Q94 122 120 116 Q132 106 124 94 Z", c(0xFFF6F0E6), sw = 4f, rot = p.wing, px = 96f, py = 100f)
                svg("M78 106 q14 6 30 4 M88 98 q14 -2 26 2", null, sw = 2.5f, stroke = c(0xFFD9D0C2), rot = p.wing, px = 96f, py = 100f)
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
        HEDGEHOG -> hedgehogCute(a, b, bob)
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
        HAMSTER -> hamsterSide(a, b, bob)
        PANDA -> {
            val black = c(0xFF2E2838)
            shadow(100f, 160f, 60f, 7f)
            translate(0f, bob) {
                val far = c(0xFF1E1A28)
                roundLeg(60f, 116f, 18f, 34f, far, b, 69f, 120f, hind = true)
                roundLeg(106f, 116f, 18f, 34f, far, a, 115f, 120f, hind = false)
                // 몸: 흰 몸 위에 어깨를 감싸는 둥근 검은 띠 (몸 윤곽 안에서만) → 윤곽선은 마지막에
                val bodyPath = oval(88f, 106f, 50f, 34f)
                drawPath(bodyPath, White)
                clipPath(bodyPath) {
                    svg("M100 66 Q116 104 108 150 L150 150 Q150 100 130 66 Z", black, sw = 0f)
                }
                paint(bodyPath, null, 5f, Ink)
                // 띠 경계는 부드러운 곡선 하나만
                clipPath(bodyPath) { svg("M100 66 Q116 104 108 150", null, sw = 4f) }
                roundLeg(74f, 116f, 18f, 34f, black, a, 83f, 120f, hind = true)
                roundLeg(120f, 116f, 18f, 34f, black, b, 129f, 120f, hind = false)
                circle(125f, 56f, 13f, black); circle(177f, 56f, 13f, black)
                ellipse(151f, 84f, 36f, 31f, White)
                // 눈 패치는 짙은 회색, 눈동자는 그 위에 어둡게 + 작은 반짝이 (흰 눈은 무서워 보여서)
                val patch = c(0xFF5B5566)
                ellipse(138f, 84f, 12f, 9.5f, patch, rot = -25f, sw = 0f)
                ellipse(164f, 84f, 12f, 9.5f, patch, rot = 25f, sw = 0f)
                eye(140f, 82f, 4.6f, 141.6f, 80.3f, 1.6f)
                eye(162f, 82f, 4.6f, 163.6f, 80.3f, 1.6f)
                svg("M146 94 q5 -3 10 0 q-1 5 -5 6 q-4 -1 -5 -6 z", Ink, sw = 2f)
                svg("M143 103 q8 7 16 0", null, sw = 3f)
                svg("M148 106 q3 8 6 0 z", c(0xFFE5484D), sw = 2f)
            }
        }
    }
}
