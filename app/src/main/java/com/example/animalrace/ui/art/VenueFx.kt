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
import kotlin.math.floor
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
    FxPart(30f, -14f, text.length * 17f + 30f, 30f, fill = BubbleHurtBg, border = Ink, borderW = 3f, top = 15f, bottom = 15f, text = text, fontSize = 16f)

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

private const val TW_X = -14f
private const val TW_Y = -64f
private const val TW_W = 128f
private const val TW_H = 160f
private const val CLOUD = "M2 16 Q-2 8 8 6 Q12 -1 22 2 Q30 -3 40 1 Q50 -3 58 2 Q68 -1 72 6 Q82 8 78 16 Q72 22 60 19 Q50 24 40 20 Q30 24 20 19 Q8 22 2 16 Z"
private const val CLOUD_LINES = "M10 8 Q22 4 32 9 M44 6 Q56 3 66 9 M18 14 Q30 17 40 13"
private const val BASE_LINES = "M22 20 Q34 15 46 19 M64 16 Q78 12 92 17"
private val Sand = Color(0xFFC68749)
private val SandCloud = Color(0xFFD49A5E)
private val SandDark = Color(0xFF8E5428)
private val SandLight = Color(0xFFF0C48E)
private val SandDust = Color(0xFFE3B37E)

/** 점들을 부드러운 곡선으로 (중점 사이를 Q로) */
private fun Path.smoothThrough(pts: List<Offset>, move: Boolean) {
    if (move) moveTo(pts[0].x, pts[0].y) else lineTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size - 1) {
        val m = (pts[i] + pts[i + 1]) / 2f
        quadraticTo(pts[i].x, pts[i].y, m.x, m.y)
    }
    lineTo(pts.last().x, pts.last().y)
}

private fun desertTrip(t: Float): FxFrame {
    // 깔때기 단면: (중심 x, y, 반폭) — viewBox 80×100
    val n = 16
    val lv = List(n) { i ->
        val f = i / (n - 1f)
        val w = (30 * (1 - f).pow(1.35f) + 2.5f) * (1 + 0.1f * sin(i * 1.7f + t / 2))
        Triple(40 + sin(t / 5 + f * 2.2f) * (1 + f * 9), 12 + f * 84, w)
    }
    fun at(fy: Float): Triple<Float, Float, Float> {
        val k = min(n - 1.001f, fy * (n - 1))
        val i = floor(k).toInt()
        val r = k - i
        val a = lv[i]; val b = lv[i + 1]
        return Triple(a.first + (b.first - a.first) * r, a.second + (b.second - a.second) * r, a.third + (b.third - a.third) * r)
    }
    val funnel = Path().apply {
        smoothThrough(lv.map { Offset(it.first - it.third, it.second) }, move = true)
        smoothThrough(lv.map { Offset(it.first + it.third, it.second) }.reversed(), move = false)
        close()
    }
    // 소용돌이 줄 하나: 깔때기를 비스듬히 감싸는 곡선의 일부(시작·길이가 돌면서 바뀜)
    fun streak(fy: Float, len: Float, off: Float): Path? {
        val (cx, y, w0) = at(fy)
        val w = w0 * 1.12f
        val s0 = (t * 0.06f + off) % 1f
        val pts = mutableListOf<Offset>()
        for (k in 0..8) {
            val u = s0 + k / 8f * len
            if (u > 1f) break
            pts += Offset(cx - w + 2 * w * u, y + sin(u * PI.toFloat()) * 5 - u * 5)
        }
        return if (pts.size < 3) null else Path().apply { smoothThrough(pts, move = true) }
    }
    val sway = sin(t / 6) * 3
    val streakColors = listOf(SandDark, Color(0xFFEDBE85), Color(0xFFA86A35), Color(0xFFF4D2A2))
    val g = sin(t / 3) * 2
    val base = PathParser().parsePathString(
        "M4 28 Q0 18 12 17 Q14 ${7 + g} 28 10 Q36 ${2 - g} 50 7 Q62 ${1 + g} 74 7 Q90 ${2 - g} 98 11 Q114 9 116 18 Q128 20 124 28 Z",
    ).toPath()
    return FxFrame(
        front = listOf(hurt("휘잉~")),
        // 토끼 등은 작아져서 토네이도 가운데에 떠올라 빙글빙글
        rot = (t * 24) % 360, scale = 0.69f, pivotY = 0.5f,
        dx = 3 * F, dy = (-22.15f + sin(t / 3) * 6) * F, pose = 0,
        under = {
            part(funnel, 80f, 100f, TW_X, TW_Y, TW_W, TW_H, Sand, sw = 2f)
            part(svg(CLOUD), 80f, 100f, TW_X + sway, TW_Y, TW_W, TW_H, SandCloud, sw = 2f)
            for (j in 0 until 9) {
                streak(0.04f + j * 0.105f, 0.45f + (j % 3) * 0.18f, j * 0.37f)?.let {
                    part(it, 80f, 100f, TW_X, TW_Y, TW_W, TW_H, null, streakColors[j % 4], 2.4f)
                }
            }
            part(svg(CLOUD_LINES), 80f, 100f, TW_X + sway, TW_Y, TW_W, TW_H, null, SandLight, 2.2f)
        },
        over = {
            for (q in 0..2) {
                streak(0.3f + q * 0.16f, 0.5f, q * 0.5f + 0.2f)?.let {
                    part(it, 80f, 100f, TW_X, TW_Y, TW_W, TW_H, null, if (q % 2 == 1) Color(0xFFF4D2A2) else SandDark, 2.2f, alpha = 0.85f)
                }
            }
            // 휘말린 모래알
            for (k in 0..6) {
                val ang = t / 3 + k * 0.9f
                val (cx, y, w) = at(0.15f + (k % 7) * 0.11f)
                dot(TW_X + (cx + cos(ang) * w * 1.3f) * 1.6f, TW_Y + y * 1.6f, 6f, SandDark, alpha = if (sin(ang) > 0) 1f else 0.35f)
            }
            // 바닥 모래 구름 (넓게 퍼지며 출렁)
            part(base, 128f, 30f, -14f, 62f, 128f, 30f, SandDust, sw = 2f)
            part(svg(BASE_LINES), 128f, 30f, -14f, 62f, 128f, 30f, null, Sand, 2f)
        },
    )
}
