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

// ---------------------------------------------------------------- 해변: 파도가 덮쳐 철썩 (시안 "해변 파도 다듬기" ②)

private val WaveLight = Color(0xFF42C0E8)
private val WaveDeep = Color(0xFF0A6FA6)
private val Spray = Color(0xFF8FD6F2)

// 새 파도 (viewBox 100×70, 왼쪽=토끼 쪽으로 말림). 몸통 → 말린 안쪽(관) → 거품 마루 → 물결 줄 → 거품 알갱이
private const val WV_BODY = "M104 70 Q99 50 88 30 Q76 9 55 4 Q35 1 22 11 Q12 19 11 31 Q19 23 29 25 Q40 28 40 40 Q40 53 27 61 Q14 68 0 70 Z"
private const val WV_TUBE = "M12 30 Q20 23 30 25 Q41 29 40 41 Q39 53 27 61 Q35 49 31 39 Q27 31 12 30 Z"
private const val WV_MID = "M104 70 Q99 54 90 41 Q78 34 66 40 Q55 50 56 70 Z"
private const val WV_FOAM = "M89 33 Q81 12 58 5 Q38 1 24 11 Q14 19 12 30 Q17 26 21 29 Q22 23 28 22 Q30 16 37 17 Q40 12 47 14 Q52 10 59 13 Q66 11 70 16 Q77 17 80 23 Q87 25 89 33 Z"
private const val WV_LINES = "M60 22 Q70 30 72 44 M74 30 Q82 40 84 54 M50 30 Q58 40 58 54"
private const val WV_SHINE = "M36 7 Q48 3 60 6"
private const val DROP2 = "M6 0 Q12 9 6 14 Q0 9 6 0 Z"
private const val PUDDLE = "M2 12 Q2 4 22 4 Q40 1 62 4 Q86 2 98 8 Q104 13 96 16 Q74 20 50 18 Q24 20 8 18 Q0 17 2 12 Z"
private const val PUDDLE_FOAM = "M10 8 Q18 5 26 8 M44 5 Q54 3 62 6 M76 6 Q86 5 92 9"
private val WaveMid = Color(0xFF2A9FD6)
private val Foam = Color(0xFFF4FBFF)
private val FoamShade = Color(0xFFCDEBF7)

private fun smoothstep(a: Float, b: Float, x: Float): Float { val k = ((x - a) / (b - a)).coerceIn(0f, 1f); return k * k * (3 - 2 * k) }

/** 새 파도 한 덩어리 */
private fun DrawScope.wave2(x: Float, y: Float, w: Float, h: Float, rot: Float, alpha: Float = 1f) {
    part(svg(WV_BODY), 100f, 70f, x, y, w, h, WaveLight, sw = 3f, rot = rot, alpha = alpha)
    part(svg(WV_MID), 100f, 70f, x, y, w, h, WaveMid, null, rot = rot, alpha = alpha)
    part(svg(WV_TUBE), 100f, 70f, x, y, w, h, WaveDeep, sw = 2f, rot = rot, alpha = alpha)
    part(svg(WV_LINES), 100f, 70f, x, y, w, h, null, Color(0xFF8FDAF5), 2.6f, rot = rot, alpha = alpha * 0.9f)
    part(svg(WV_FOAM), 100f, 70f, x, y, w, h, Foam, sw = 2.5f, rot = rot, alpha = alpha)
    part(svg(WV_SHINE), 100f, 70f, x, y, w, h, null, FoamShade, 2.4f, rot = rot, alpha = alpha)
}

/** 물방울: 하늘색 + 잉크 + 흰 점 */
private fun DrawScope.drop2(cx: Float, cy: Float, size: Float, alpha: Float, rot: Float = 0f) {
    part(svg(DROP2), 12f, 14f, cx - size / 2, cy - size * 0.6f, size, size * 1.17f, Spray, sw = 1.8f, alpha = alpha, rot = rot)
    dot(cx - size * 0.12f, cy + size * 0.12f, size * 0.22f, White, alpha = alpha)
}

/** 거품 뭉치(동그라미들을 한 덩어리로: 잉크 먼저, 채우기 나중) */
private fun DrawScope.foamCloud(puffs: List<Triple<Float, Float, Float>>, alpha: Float = 1f) {
    puffs.forEach { (x, y, d) -> dot(x, y, d + 4f, Ink, alpha = alpha) }
    puffs.forEach { (x, y, d) -> dot(x, y, d, Foam, alpha = alpha) }
}

/** 해변 (시안 "해변 파도 다듬기" ②): 뒤에서 솟음 → 앞으로 말려 토끼를 덮침 → 하얗게 부서짐 → 흠뻑 젖어 물이 뚝뚝 */
private fun beachTrip(t: Float): FxFrame {
    val ph = (t % 30f) / 30f
    val rise = smoothstep(0f, 0.32f, ph)
    val crash = smoothstep(0.32f, 0.5f, ph)
    val splash = smoothstep(0.45f, 0.8f, ph)
    val after = smoothstep(0.55f, 1f, ph)
    val hit = if (ph in 0.45f..0.75f) sin((ph - 0.45f) / 0.3f * PI.toFloat()) else 0f
    return FxFrame(
        front = listOf(hurt("철썩!").copy(x = -48f, y = -40f)),
        // 맞는 순간 뒤로 밀리며 젖혀졌다가 돌아옴
        dx = -9f * hit * F, rot = -12f * hit, dy = 2f * hit * F, pose = 0,
        under = {
            // 물웅덩이: 덮친 뒤 넓어졌다가 줄어듦
            val pw = 70f + 50f * splash - 20f * after
            val pa = splash * (1 - after * 0.5f)
            part(svg(PUDDLE), 104f, 20f, 50 - pw / 2, 70f, pw, 20f, Spray, sw = 2.5f, alpha = pa)
            part(svg(PUDDLE_FOAM), 104f, 20f, 50 - pw / 2, 70f, pw, 20f, null, Foam, 2.6f, alpha = pa)
            // 솟아오르는 동안은 토끼 뒤
            if (crash < 0.01f) wave2(62 - rise * 34, 30 - rise * 76, 124f * (0.6f + rise * 0.4f), 87f * (0.6f + rise * 0.4f), 6 - rise * 6)
        },
        over = {
            // 앞으로 말려 덮칠 때는 토끼 앞 (아래로 꺼지며 사라짐)
            if (crash > 0.01f && splash < 0.6f) {
                val a = 1 - smoothstep(0.42f, 0.55f, ph)
                wave2(28 - crash * 30, -46 + crash * 40, 124f, 87f * (1 - crash * 0.35f), -crash * 18, alpha = a)
            }
            // 하얗게 부서지는 거품: 위·옆으로 솟구쳐 퍼지며 작아져 사라짐 (흐려지지 않게 크기로)
            if (splash > 0f && splash < 1f) {
                val puffs = (0..8).map { k ->
                    val ang = -PI.toFloat() * (0.12f + k * 0.095f)
                    val r = 10 + splash * (30 + (k % 3) * 8)
                    val d = (24 + (k % 2) * 6) * (1 - splash) * (1 - splash * 0.3f)
                    Triple(48 + cos(ang) * r * 1.3f, 40 + sin(ang) * r - splash * 8, d)
                }.filter { it.third > 1.5f } + listOf(Triple(48f, 42f, 30 * (1 - splash) * (1 - splash)))
                foamCloud(puffs.filter { it.third > 1.5f })
                for (k in 0..6) {
                    val ang = -PI.toFloat() * (0.1f + k * 0.13f)
                    val r = 30 + splash * 40
                    drop2(48 + cos(ang) * r * 1.2f, 36 + sin(ang) * r + splash * splash * 40, 10f, 1 - splash * splash, rot = ang * 57f + 90f)
                }
            }
            // 흠뻑 젖음: 몸에서 물이 뚝뚝
            if (after > 0f) {
                for (k in 0..3) {
                    val p = (ph * 3 + k * 0.27f) % 1f
                    drop2(24f + k * 16, 30f + p * 34, 7f, (1 - p) * after)
                }
            }
        },
    )
}

// ---------------------------------------------------------------- 설원: 빙판에서 미끄덩 → 눈더미에 푹 (시안 "설원 연출 다듬기" ②)

private const val ICE = "M4 14 Q4 2 40 3 Q72 0 104 3 Q140 2 140 14 Q140 26 104 25 Q72 28 40 25 Q4 26 4 14 Z"
private const val ICE_CRACKS = "M30 8 L44 14 L40 22 M44 14 L58 12 M96 6 L104 14 L118 12 M104 14 L100 22"
private const val ICE_SHINE = "M18 9 L34 7 M66 6 L86 5 M114 18 L126 17"

private const val ICE_RIM = "M8 18 Q40 27 72 25 Q104 28 136 18"
private const val SKID = "M0 0 L26 0 M4 5 L22 5"
private const val BANK = "M0 52 Q0 38 10 32 Q10 18 24 16 Q30 4 44 7 Q56 2 64 14 Q74 20 72 34 Q78 42 76 52 Z"
private const val BANK_SHADE = "M8 46 Q40 52 70 46 M20 26 Q28 22 36 25 M50 16 Q58 15 62 21"
private val Snow = Color(0xFFFDFEFF)
private val SnowShade = Color(0xFFBFD9EE)
private val IceBlue = Color(0xFFCFE8F8)
private val IceEdge = Color(0xFF9CC8EA)

/** 하얀 눈 뭉치 덩어리 (동그라미들을 한 덩어리로: 잉크 먼저, 채우기 나중) */
private fun DrawScope.poof(puffs: List<Triple<Float, Float, Float>>) {
    val ps = puffs.filter { it.third > 1.5f }
    ps.forEach { (x, y, d) -> dot(x, y, d + 4f, Ink) }
    ps.forEach { (x, y, d) -> dot(x, y, d, Snow) }
}

/** 반짝이는 빙판 + 미끄러진 자국 */
private fun DrawScope.ice2(skid: Float) {
    part(svg(ICE), 144f, 28f, -22f, 56f, 144f, 28f, IceBlue, sw = 2.5f)
    part(svg(ICE_RIM), 144f, 28f, -22f, 56f, 144f, 28f, null, IceEdge, 2.4f)
    part(svg(ICE_CRACKS), 144f, 28f, -22f, 56f, 144f, 28f, null, Color(0xFF7FAED6), 1.8f)
    part(svg(ICE_SHINE), 144f, 28f, -22f, 56f, 144f, 28f, null, White, 3.2f)
    if (skid > 0f) part(svg(SKID), 26f, 6f, 10f - skid * 6, 70f, 26f + skid * 20, 6f, null, White, 2.6f, alpha = 0.9f)
}

/** 반짝 (✦ 모양 4갈래) */
private fun DrawScope.twinkle(cx: Float, cy: Float, r: Float, alpha: Float) {
    val d = "M0 -1 L0.25 -0.25 L1 0 L0.25 0.25 L0 1 L-0.25 0.25 L-1 0 L-0.25 -0.25 Z"
    part(svg(d), 2f, 2f, cx - r, cy - r, r * 2, r * 2, White, IceEdge, 0.12f, alpha = alpha)
}

/** 빙판에서 미끄러져 빙글 → 앞 눈더미에 머리부터 쿵 → 엉덩이만 나와 버둥 */
private fun snowTrip(t: Float, loop: Boolean): FxFrame {
    val ph = if (loop) (t % 30f) / 30f else (t / 30f).coerceAtMost(1f)
    val slide = (ph / 0.45f).coerceIn(0f, 1f)
    val stuck = ph >= 0.45f
    val burst = ((ph - 0.45f) / 0.35f).coerceIn(0f, 1f)
    return FxFrame(
        front = listOf(hurt(if (!stuck) "미끄덩~" else "푹!").copy(x = -48f, y = -40f)),
        // 미끄러지며 앞으로 쭉 → 처박히면 앞으로 고꾸라져 버둥
        rot = if (!stuck) sin(t * 0.9f) * 18 * (1 - slide) + slide * 20 else 38f + sin(t * 1.4f) * 6,
        dx = if (!stuck) slide * slide * 16 * F else 16 * F + sin(t * 1.4f) * 1.5f * F,
        dy = if (!stuck) 0f else 6 * F,
        pose = 0, pivotY = 0.8f,
        under = { ice2(slide) },
        over = {
            if (!stuck) for (c in 0..3) {
                val q = (ph * 3 + c * 0.25f) % 1f
                twinkle(40 - q * 40, 66 - sin(q * PI.toFloat()) * 20, 6f * (1 - q) + 2, 1 - q)
            }
            // 눈더미 (토끼 앞): 다가오다가 처박히면 머리를 덮음
            val bx = if (!stuck) 112 - slide * 32 else 80f
            if (stuck) {
                // 처박힌 순간 눈더미 위로 눈 뭉치가 솟구쳐 작아지며 사라짐 (눈더미 뒤)
                val q = (burst * 1.6f).coerceAtMost(1f)
                poof(listOf(Triple(98f - q * 14, 22 - q * 26, 20 * (1 - q)), Triple(112f + q * 12, 20 - q * 20, 16 * (1 - q)), Triple(88f - q * 24, 30 - q * 10, 13 * (1 - q))))
            }
            part(svg(BANK), 76f, 52f, bx, 26f, 70f, 54f, Snow, sw = 2.6f)
            part(svg(BANK_SHADE), 76f, 52f, bx, 26f, 70f, 54f, null, SnowShade, 2.2f)
            if (stuck) {
                // 위에서 떨어지는 눈 조각
                for (k in 0..2) {
                    val q = (ph * 2.5f + k * 0.33f) % 1f
                    dot(86f + k * 12, -10f + q * 40, 7f * (1 - q), Snow, 1.6f)
                }
                twinkle(68f, 4f, 5f, 0.5f + 0.5f * abs(sin(t / 2)))
                twinkle(118f, 10f, 4f, 1 - 0.5f * abs(sin(t / 2)))
            }
        },
    )
}

// ---------------------------------------------------------------- 사막: 모래 토네이도에 휘말려 빙글빙글

private const val TW_X = -14f
private const val TW_Y = -64f
private const val TW_W = 128f
private const val TW_H = 160f
private const val BASE_LINES = "M22 20 Q34 15 46 19 M64 16 Q78 12 92 17"
private val Sand = Color(0xFFC68749)
private val HoleDeep = Color(0xFF6B3D1C)
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

private fun oval(cx: Float, cy: Float, rx: Float, ry: Float) =
    Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }

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
    val streakColors = listOf(SandDark, Color(0xFFEDBE85), Color(0xFFA86A35), Color(0xFFF4D2A2))
    val g = sin(t / 3) * 2
    val base = PathParser().parsePathString(
        "M4 28 Q0 18 12 17 Q14 ${7 + g} 28 10 Q36 ${2 - g} 50 7 Q62 ${1 + g} 74 7 Q90 ${2 - g} 98 11 Q114 9 116 18 Q128 20 124 28 Z",
    ).toPath()
    return FxFrame(
        // 말풍선은 회오리 오른쪽으로 비켜서 (토끼 얼굴을 가리지 않게)
        front = listOf(hurt("휘잉~").copy(x = 196f, y = -70f)),
        // 토끼 등은 작아져서 토네이도 가운데에 떠올라 빙글빙글
        rot = (t * 24) % 360, scale = 0.69f, pivotY = 0.5f,
        dx = 3 * F, dy = (-22.15f + sin(t / 3) * 6) * F, pose = 0,
        under = {
            part(funnel, 80f, 100f, TW_X, TW_Y, TW_W, TW_H, Sand, sw = 2f)
            for (j in 0 until 9) {
                streak(0.04f + j * 0.105f, 0.45f + (j % 3) * 0.18f, j * 0.37f)?.let {
                    part(it, 80f, 100f, TW_X, TW_Y, TW_W, TW_H, null, streakColors[j % 4], 2.4f)
                }
            }
            // 꼭대기 구멍 (시안 "회오리 꼭대기 · 뚜껑 대신 구멍" ②): 밝은 테두리 안쪽이 두 단계로 어두워짐.
            // 소용돌이 줄보다 나중에, 몸통 위 양끝을 덮게 조금 넓게
            val (tx, ty, tw) = lv[0]
            part(oval(tx, ty, tw * 1.06f, tw * 0.26f), 80f, 100f, TW_X, TW_Y, TW_W, TW_H, SandLight, sw = 2f)
            part(oval(tx, ty + tw * 0.04f, tw * 0.8f, tw * 0.17f), 80f, 100f, TW_X, TW_Y, TW_W, TW_H, SandDark, null)
            part(oval(tx, ty + tw * 0.09f, tw * 0.5f, tw * 0.09f), 80f, 100f, TW_X, TW_Y, TW_W, TW_H, HoleDeep, null)
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
