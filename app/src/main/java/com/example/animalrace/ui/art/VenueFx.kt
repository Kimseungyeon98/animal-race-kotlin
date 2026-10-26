package com.example.animalrace.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.example.animalrace.model.Venue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/*
 * 경기장별 "혼자 넘어짐(Fx.TRIP)" 장면. 시안: 캔버스 "경기장별 연출 v3" (Venue-Events.dc.html)
 * 시안 좌표(90×77 그림이 (5,4)에 놓인 100×85 상자)를 그대로 옮기고 [F]배 해서 200×170 그림 좌표로 쓴다.
 */

private const val F = 200f / 90f
private fun mx(x: Float) = (x - 5f) * F
private fun my(y: Float) = (y - 4f) * F

private val White = Color.White
private val StarYellow = Color(0xFFFFD84D)
private val BubbleHurtBg = Color(0xFFFFE1DC)

private val cache = HashMap<String, Path>()
private fun svg(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** 경로를 좌우 반전 (viewBox 너비 [vw]). M/Q/L/Z와 숫자 쌍만 쓰는 경로용 */
private fun mirror(d: String, vw: Float): String {
    var n = 0
    return Regex("-?\\d+(\\.\\d+)?").replace(d) { m -> if (n++ % 2 == 0) (vw - m.value.toFloat()).toString() else m.value }
}

/**
 * 시안 좌표 상자 ([x],[y],[w],[h])에 viewBox [vbW]×[vbH] 경로를 그린다. 선 굵기 [sw]는 viewBox 단위.
 * [rot]는 상자 아래 가운데를 축으로 돈다 (시안의 transform-origin: 50% 100%).
 */
private fun DrawScope.part(
    path: Path, vbW: Float, vbH: Float, x: Float, y: Float, w: Float, h: Float,
    fill: Color?, stroke: Color? = Ink, sw: Float = 2.5f, alpha: Float = 1f, rot: Float = 0f,
) {
    if (alpha <= 0f) return
    val k = min(w / vbW, h / vbH) * F
    val ox = mx(x)
    val oy = my(y)
    withTransform({
        if (rot != 0f) rotate(rot, Offset(ox + w * F / 2, oy + h * F))
        translate(ox, oy)
        scale(k, k, Offset.Zero)
    }) {
        if (fill != null) drawPath(path, fill, alpha = alpha)
        if (stroke != null && sw > 0f) drawPath(path, stroke, alpha = alpha, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 시안 좌표 중심 ([cx],[cy]), 지름 [d]의 동그라미 */
private fun DrawScope.dot(cx: Float, cy: Float, d: Float, fill: Color, line: Float = 0f, alpha: Float = 1f) {
    if (alpha <= 0f) return
    val c = Offset(mx(cx), my(cy))
    drawCircle(fill, d / 2 * F, c, alpha = alpha)
    if (line > 0f) drawCircle(Ink, d / 2 * F, c, alpha = alpha, style = Stroke(line * F))
}

private const val STAR = "M10 0 L12.5 6.5 L20 7 L14 11.5 L16 19 L10 14.5 L4 19 L6 11.5 L0 7 L7.5 6.5 Z"

private fun hurt(text: String) =
    FxPart(30f, -14f, text.length * 17f + 40f, 30f, fill = BubbleHurtBg, border = Ink, borderW = 3f, top = 15f, bottom = 15f, text = text, fontSize = 16f)

/** 경기장에 맞는 "혼자 넘어짐" 장면. [loop]이면 한 주기를 반복, 아니면 시간 순서대로 한 번. */
internal fun venueTrip(venue: Venue, t: Float, loop: Boolean): FxFrame = when (venue) {
    Venue.MEADOW -> meadowTrip(t)
    Venue.BEACH -> beachTrip(t)
    Venue.SNOW -> snowTrip(t, loop)
    Venue.DESERT -> desertTrip(t)
}

// ---------------------------------------------------------------- 초원: 돌부리에 걸려 꽈당

private const val ROCK = "M3 27 Q1 13 10 8 Q15 1 24 3 Q36 6 37 27 Z"
private const val ROCK_LINES = "M24 3 Q31 12 29 27 M14 10 Q12 18 15 26"
private const val ROCK_SHINE = "M10 11 Q14 6 19 7"
private const val MOUND = "M-5 30 Q6 23 18 27 Q30 22 45 30 Z"

private fun meadowTrip(t: Float): FxFrame {
    val q = (t / 6).coerceAtMost(1f)
    val tw = abs(sin(t / 3))
    val sc = 0.8f + tw * 0.4f
    val front = mutableListOf<FxPart>()
    for (k in 0..2) {
        val ang = t / 4 + k * 2.1f
        front += FxPart(140 + cos(ang) * 24, 16 + sin(ang) * 7, 18f, 18f, text = "✦", fontSize = 15f, textColor = Color(0xFFFFB020), numberFont = true)
    }
    front += hurt("꽈당!")
    return FxFrame(
        back = listOf(FxPart(120f, 146f, 44f, 14f, fill = Color(0xFFE8D3B0), border = Ink, alpha = 0.85f)),
        front = front, dx = -4f * q, dy = 8f * q, rot = -18f * q, pose = 0,
        over = {
            part(svg(ROCK), 40f, 30f, 52f, 38f, 60f, 45f, Color(0xFFA8A29A))
            part(svg(ROCK_LINES), 40f, 30f, 52f, 38f, 60f, 45f, null, Color(0xFF7E776E), 2f)
            part(svg(ROCK_SHINE), 40f, 30f, 52f, 38f, 60f, 45f, null, White, 3f)
            part(svg(MOUND), 40f, 30f, 52f, 38f, 60f, 45f, Color(0xFFD9A866), sw = 2f)
            part(svg(STAR), 20f, 19f, 46f, 24f, 22f * sc, 21f * sc, StarYellow, sw = 2f, alpha = 0.6f + 0.4f * tw)
        },
    )
}

// ---------------------------------------------------------------- 해변: 앞에서 파도가 철썩

private val WAVE_BODY = mirror("M2 68 Q14 64 22 50 Q34 24 54 12 Q72 2 88 8 Q100 14 96 28 Q93 34 87 30 Q85 38 77 35 Q73 42 67 38 Q60 46 66 54 Q74 61 94 64 Q74 71 57 63 Q48 71 30 68 Q16 71 2 68 Z", 100f)
private val WAVE_DEEP = mirror("M22 54 Q32 30 50 17 Q56 15 58 19 Q54 25 58 29 Q54 33 58 38 Q50 42 48 50 Q41 59 30 61 Q22 60 22 54 Z", 100f)
private val WAVE_STREAKS = mirror("M28 56 L36 42 M36 59 L43 46 M44 57 L49 48", 100f)
private val WAVE_FOAM = mirror("M54 13 Q70 5 86 9 Q97 14 95 26 Q91 30 87 26 Q85 34 78 32 Q74 38 68 34 Q64 30 66 26 Q60 25 62 20 Q56 20 54 13 Z", 100f)
private const val DROP = "M5 0 Q10 8 5 12 Q0 8 5 0 Z"
private const val WASH = "M0 14 Q14 4 28 10 Q42 2 58 9 Q74 2 90 9 Q106 3 124 14 L124 20 L0 20 Z"
private val WaveLight = Color(0xFF42C0E8)
private val WaveDeep = Color(0xFF0A6FA6)
private val Spray = Color(0xFF8FD6F2)

private fun beachTrip(t: Float): FxFrame {
    val ph = (t % 30f) / 30f
    val rise = sin(ph * PI.toFloat())
    val x = 16 + (1 - rise) * 34
    val y = -44 + (1 - rise) * 26
    val rot = 6 - rise * 6
    return FxFrame(
        front = listOf(hurt("철썩!")),
        dx = wobble(t, 1.5f), pose = 0,
        under = {
            part(svg(WAVE_BODY), 100f, 70f, x, y, 124f, 87f, WaveLight, rot = rot)
            part(svg(WAVE_DEEP), 100f, 70f, x, y, 124f, 87f, WaveDeep, null, rot = rot)
            part(svg(WAVE_STREAKS), 100f, 70f, x, y, 124f, 87f, null, WaveLight, 3.5f, rot = rot)
            part(svg(WAVE_FOAM), 100f, 70f, x, y, 124f, 87f, White, sw = 2f, rot = rot)
        },
        over = {
            for (k in 0..3) {
                val p = (ph + k * 0.22f) % 1f
                part(svg(DROP), 10f, 12f, 70 - k * 18 - p * 14, -6 + p * 60, 8f, 10f, Spray, sw = 1.8f, alpha = 1 - p)
            }
            part(svg(WASH), 124f, 20f, -12f, 66f, 124f, 20f, Spray, sw = 2f, alpha = 0.5f + rise * 0.5f)
        },
    )
}

// ---------------------------------------------------------------- 설원: 빙판에서 미끄덩 → 눈더미 와르르

private const val ICE = "M4 14 Q4 2 40 3 Q72 0 104 3 Q140 2 140 14 Q140 26 104 25 Q72 28 40 25 Q4 26 4 14 Z"
private const val ICE_CRACKS = "M30 8 L44 14 L40 22 M44 14 L58 12 M96 6 L104 14 L118 12 M104 14 L100 22"
private const val ICE_SHINE = "M18 9 L34 7 M66 6 L86 5 M114 18 L126 17"
private const val WIND = "M2 5 Q16 0 32 5"
private const val CHIP = "M4 0 L8 4 L4 8 L0 4 Z"
private const val CLUMP_FALL = "M6 22 Q0 12 10 9 Q14 0 26 4 Q36 -2 44 6 Q56 6 54 16 Q58 24 46 25 Q30 30 18 26 Q6 28 6 22 Z"
private const val CLUMP_LAND = "M2 26 Q0 16 10 14 Q14 4 26 7 Q36 2 44 9 Q56 10 56 20 Q60 28 48 28 Q30 31 14 29 Q2 31 2 26 Z"
private const val CLUMP_SHADE = "M16 12 Q22 9 28 11 M36 10 Q42 9 46 13"

private fun snowTrip(t: Float, loop: Boolean): FxFrame {
    // 약 1.8초 동안: 앞 절반은 미끄덩, 0.45부터 눈더미가 떨어져 0.6에 머리에 착지
    val ph = if (loop) (t % 30f) / 30f else (t / 30f).coerceAtMost(1f)
    val sliding = ph < 0.5f
    val tw = abs(sin(t / 2))
    return FxFrame(
        front = listOf(hurt(if (sliding) "미끄덩!" else "와르르!")),
        rot = if (sliding) sin(t * 0.9f) * 24 else -10f,
        dx = if (sliding) sin(t * 0.45f) * 12 * F else 0f,
        dy = if (sliding) 0f else 6 * F,
        pose = 0,
        under = {
            part(svg(ICE), 144f, 28f, -22f, 56f, 144f, 28f, Color(0xFFCFE8F8))
            part(svg(ICE_CRACKS), 144f, 28f, -22f, 56f, 144f, 28f, null, Color(0xFF7FAED6), 1.8f)
            part(svg(ICE_SHINE), 144f, 28f, -22f, 56f, 144f, 28f, null, White, 3f)
        },
        over = {
            if (sliding) {
                for (k in 0..2) part(svg(WIND), 34f, 10f, -30 - (ph * 40) % 10, 22f + k * 14, 34f, 10f, null, Ink, 2.2f, alpha = 0.55f)
            } else {
                part(svg(STAR), 20f, 19f, 4f, -8f, 16f, 16f, White, sw = 2f, alpha = 0.5f + tw * 0.5f)
                part(svg(STAR), 20f, 19f, 78f, -2f, 12f, 12f, White, sw = 2f, alpha = 1 - tw * 0.5f)
            }
            for (c in 0..3) {
                val q = (ph * 2 + c * 0.25f) % 1f
                val dir = if (c % 2 == 1) 1 else -1
                part(svg(CHIP), 8f, 8f, 46 + dir * (14 + q * 44), 66 - sin(q * PI.toFloat()) * 34, 8f, 8f,
                    Color(0xFFE8F6FF), sw = 1.6f, alpha = 1 - q, rot = q * 180)
            }
            if (ph >= 0.45f) {
                val fall = ((ph - 0.45f) / 0.15f).coerceIn(0f, 1f)
                val sy = -70 + fall * 66
                part(svg(if (fall < 1f) CLUMP_FALL else CLUMP_LAND), 60f, 31f, 26f, sy - 10, 84f, 43f, White, sw = 2.2f)
                part(svg(CLUMP_SHADE), 60f, 31f, 26f, sy - 10, 84f, 43f, null, Color(0xFFBFD9EE), 2f)
                if (fall >= 1f) {
                    val pf = ((ph - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    for (f in 0..4) {
                        val ang = -PI.toFloat() + f * (PI.toFloat() / 4)
                        val r = 30 + pf * 26
                        dot(68 + cos(ang) * r, 10 + sin(ang) * r * 0.8f, 9 - pf * 5, White, 1.6f, 1 - pf)
                    }
                } else {
                    for (g in 0..2) dot(43f + g * 18, sy - 19 - g * 6, 6f, White, 1.4f)
                }
            }
        },
    )
}

// ---------------------------------------------------------------- 사막: 모래 토네이도에 휘말려 빙글빙글
// 시안: 캔버스 "사막 회오리 다듬기" A안 (Tornado.dc.html). 시안 픽셀 좌표(300×330 칸)를 그대로 쓰고
// 시안 점 (TW_AX, TW_AY)를 그림 좌표 (TW_OX, TW_OY)에 맞춰 [TW_K]배로 그린다.

private const val TW_TOP = 58f
private const val TW_BOT = 258f
private const val TW_CX = 150f
private const val TW_AX = 150f
private const val TW_AY = 164.5f
private const val TW_K = 1.6f
private const val TW_OX = 104f
private const val TW_OY = 19f
private val SandLight = Color(0xFFF2CC96)
private val Sand = Color(0xFFD69A5C)
private val SandMid = Color(0xFFC68749)
private val SandDark = Color(0xFF8E5428)
private val SandDust = Color(0xFFEBC38E)
private val SandCream = Color(0xFFFFF0D6)
private val Tumble = Color(0xFFC9A15E)
private val TumbleLine = Color(0xFF8A6A30)
private val Pebble = Color(0xFFA8A29A)

/** 몸통 단면: 높이 비율 [f](0 위 ~ 1 바닥) → (중심 x, y, 반폭). 위는 하늘에 붙고 바닥이 크게 춤춘다. */
private fun twLevel(t: Float, f: Float): Triple<Float, Float, Float> {
    val w = 70 * (1 - f).pow(1.15f) + 9
    val cx = TW_CX + sin(t / 7 + f * 2.4f) * (2 + f * 16)
    return Triple(cx, TW_TOP + f * (TW_BOT - TW_TOP), w)
}

/** 점들을 부드러운 곡선으로 (중점 사이를 Q로) */
private fun Path.smoothThrough(pts: List<Offset>, move: Boolean) {
    if (move) moveTo(pts[0].x, pts[0].y) else lineTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size - 1) {
        val m = (pts[i] + pts[i + 1]) / 2f
        quadraticTo(pts[i].x, pts[i].y, m.x, m.y)
    }
    lineTo(pts.last().x, pts.last().y)
}

/** 타원 반쪽: 앞(아래 반원) 또는 뒤(위 반원), 왼쪽에서 오른쪽으로 */
private fun halfRing(cx: Float, y: Float, w: Float, ry: Float, front: Boolean) = Path().apply {
    moveTo(cx - w, y)
    arcTo(androidx.compose.ui.geometry.Rect(cx - w, y - ry, cx + w, y + ry), 180f, if (front) -180f else 180f, false)
}

private fun DrawScope.ink(path: Path, color: Color, width: Float, alpha: Float = 1f, dash: androidx.compose.ui.graphics.PathEffect? = null) =
    drawPath(path, color, alpha = alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash))

/** 동그라미 여러 개를 한 덩어리 구름으로: 잉크를 먼저 전부 깔고 그 위에 채우기 (겉테두리만 남음) */
private fun DrawScope.cloud(puffs: List<Pair<Offset, Pair<Float, Color>>>) {
    puffs.forEach { (c, rc) -> drawCircle(Ink, rc.first + 2.4f, c) }
    puffs.forEach { (c, rc) -> drawCircle(rc.second, rc.first, c) }
}

private fun DrawScope.twFunnel(t: Float) {
    val l = mutableListOf<Offset>(); val r = mutableListOf<Offset>()
    for (i in 0..12) {
        val (cx, y, w) = twLevel(t, i / 12f)
        l += Offset(cx - w, y); r += Offset(cx + w, y)
    }
    val body = Path().apply { smoothThrough(l, move = true); smoothThrough(r.reversed(), move = false); close() }
    drawPath(body, SandMid)
    ink(body, Ink, 5f)
    // 꼭대기 입구: 얇은 타원 (뚜껑처럼 보이지 않게)
    val (tx, _, tw) = twLevel(t, 0f)
    val ry = tw * 0.13f + 2
    val rect = androidx.compose.ui.geometry.Rect(tx - tw, TW_TOP - ry, tx + tw, TW_TOP + ry)
    drawOval(Sand, rect.topLeft, rect.size)
    drawOval(Ink, rect.topLeft, rect.size, style = Stroke(3f))
    // 맞닿아 감긴 띠 9개 + 띠 위를 흘러가는 밝은 줄 (도는 느낌)
    for (j in 1 until 10) {
        val g = j / 9f
        val (cx, y, w) = twLevel(t, g)
        val bry = w * 0.24f + 2
        val th = 21 - g * 11
        val band = halfRing(cx, y, w, bry, front = true)
        ink(band, Ink, th + 4)
        ink(band, if (j % 2 == 1) Sand else SandLight, th)
        val flow = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 18f), t * (2.4f + g * 1.8f))
        ink(halfRing(cx, y, w * 0.96f, bry, front = true), if (j % 2 == 1) SandLight else SandCream, maxOf(2f, th * 0.28f), 0.9f, flow)
    }
}

/** 꼭대기 양끝 모래 구름 + 떨어져 나가 작아지며 흩어지는 뭉치 */
private fun DrawScope.twRim(t: Float) {
    val (cx, _, w) = twLevel(t, 0f)
    val puffs = mutableListOf<Pair<Offset, Pair<Float, Color>>>()
    listOf(-1, 1).forEachIndexed { si, side ->
        for (k in 0..2) puffs += Offset(cx + side * (w - 4 + k * 9), TW_TOP + 3 - k * 3 + sin(t / 4 + k + si) * 1.5f) to
            ((11 - k * 2.5f) to if (k % 2 == 1) SandLight else SandDust)
    }
    for (j in 0..3) {
        val ph = (t * 0.02f + j / 4f) % 1f
        val side = if (j % 2 == 1) 1 else -1
        val r = 8 * (1 - ph)
        if (r > 1.5f) puffs += Offset(cx + side * (w + 8 + ph * 34), TW_TOP + 2 - ph * 18) to (r to SandDust)
    }
    cloud(puffs)
}

/** 바닥 그림자 */
private fun DrawScope.twShadow(t: Float) {
    val (cx, _, _) = twLevel(t, 1f)
    drawOval(Ink.copy(alpha = 0.15f), Offset(cx - 64, 257f), androidx.compose.ui.geometry.Size(128f, 12f))
}

/** 바닥: 닿은 자리에서 모래 구름이 양옆으로 굴러 나가며 작아짐 */
private fun DrawScope.twSkirt(t: Float) {
    val (cx, _, _) = twLevel(t, 1f)
    val puffs = mutableListOf(Offset(cx, 254f) to (13f to SandLight))
    for (k in 0..7) {
        val ph = (t * 0.028f + k / 8f) % 1f
        val side = if (k % 2 == 1) 1 else -1
        puffs += Offset(cx + side * (10 + ph * 66), 258 - sin(ph * PI.toFloat()) * 9 + (1 - ph) * 2) to
            ((13 * (1 - ph) + 3) to if (k % 4 < 2) SandDust else SandLight)
    }
    cloud(puffs)
}

/** 돌멩이·회전초: 몸통 둘레를 돌며 뒤([near] false)는 작고 흐리게 */
private fun DrawScope.twDebris(t: Float, near: Boolean) {
    for (k in 0..4) {
        val (cx, y, w) = twLevel(t, 0.15f + k * 0.17f)
        val a = t * 0.12f + k * 1.3f
        if ((sin(a) > 0) != near) continue
        val x = cx + cos(a) * (w + 14)
        val yy = y + sin(a) * (w * 0.22f + 3)
        val alpha = if (near) 1f else 0.5f
        if (k == 2) {
            val r = if (near) 10f else 7f
            drawCircle(Tumble, r, Offset(x, yy), alpha = alpha)
            drawCircle(Ink, r, Offset(x, yy), alpha = alpha, style = Stroke(2f))
            val twig = Path().apply {
                moveTo(x - r * 0.6f, yy - r * 0.2f)
                quadraticTo(x, yy - r * 0.9f, x + r * 0.5f, yy)
                quadraticTo(x, yy + r * 0.7f, x - r * 0.4f, yy + r * 0.3f)
            }
            ink(twig, TumbleLine, 1.6f, alpha)
        } else {
            val r = if (near) 4.5f else 3f
            drawCircle(if (k % 2 == 1) SandDark else Pebble, r, Offset(x, yy), alpha = if (near) 1f else 0.45f)
            drawCircle(Ink, r, Offset(x, yy), alpha = if (near) 1f else 0.45f, style = Stroke(1.6f))
        }
    }
}

/** 양옆 바람 줄 ) ( */
private fun DrawScope.twWind(t: Float) {
    for (k in 0..3) {
        val (cx, y, w) = twLevel(t, 0.2f + k * 0.2f)
        val side = if (k % 2 == 1) 1 else -1
        val ph = (t * 0.05f + k * 0.3f) % 1f
        val x = cx + side * (w + 22 + ph * 8)
        ink(Path().apply { moveTo(x, y - 12); quadraticTo(x + side * 9, y, x, y + 12) }, Ink, 2.4f, 0.55f * sin(ph * PI.toFloat()))
    }
}

/** 토끼를 감싸며 지나가는 모래 띠 두 줄 */
private fun DrawScope.twWrap(t: Float, cx: Float, cy: Float) {
    for (k in 0..1) {
        val y = cy - 10 + k * 22
        val w = 48f - k * 6
        val ph = (t * 0.07f + k * 0.5f) % 1f
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(40f, 200f), ph * 240)
        val band = halfRing(cx, y, w, 9f, front = true)
        ink(band, Ink, 9f, 0.9f, dash)
        ink(band, if (k == 1) SandLight else SandDust, 5f, 1f, dash)
    }
}

/** 시안 좌표로 그리기 */
private fun DrawScope.tw(block: DrawScope.() -> Unit) = withTransform({
    translate(TW_OX - TW_AX * TW_K, TW_OY - TW_AY * TW_K)
    scale(TW_K, TW_K, Offset.Zero)
}) { block() }

private fun desertTrip(t: Float): FxFrame {
    // 토끼는 몸통 가운데쯤(높이 0.55)에서 작아져 빙글빙글, 몸통과 함께 흔들림
    val (mx, my, _) = twLevel(t, 0.55f)
    val rx = mx + cos(t / 9) * 8
    val ry = my - 3.5f + sin(t / 5) * 5
    return FxFrame(
        // 말풍선은 회오리 오른쪽 위로 비켜서 (토끼 얼굴을 가리지 않게)
        front = listOf(hurt("휘잉~").copy(x = 236f, y = -96f)),
        rot = (t * 24) % 360, scale = 0.66f, pivotY = 0.5f,
        dx = TW_OX + (rx - TW_AX) * TW_K - 100f, dy = TW_OY + (ry - TW_AY) * TW_K - 85f, pose = 0,
        under = {
            tw {
                twShadow(t)
                twDebris(t, near = false)
                twFunnel(t)
                twRim(t)
            }
        },
        over = {
            tw {
                twWrap(t, rx, ry)
                twSkirt(t)
                twDebris(t, near = true)
                twWind(t)
            }
        },
    )
}
