package com.example.animalrace.ui.race

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.example.animalrace.model.Venue
import com.example.animalrace.ui.theme.Pal
import kotlin.math.floor
import kotlin.math.sin

/**
 * 경기장 테마. 경주를 시작할 때 무작위로 하나를 고르며, 순위와는 무관한 배경 연출이다.
 * 시안: 캔버스 "경기장 테마 (무작위)" 보드 (Stadiums.dc.html)
 */
enum class Stadium(
    val venue: Venue,
    val skyTop: Color,
    val skyBottom: Color,
    val rail: Color,
    val post: Color,
    val verge: Color,
    val dirt: Color,
    val dirtAlt: Color,
    val line: Color,
    val dust: Color,
    /** 멀리 보이는 언덕(밝은·어두운). null이면 언덕 대신 바다 */
    val hills: Pair<Color, Color>?,
    val sun: Color? = null,
    /** 울타리 뒤 소품과 한 주기(470dp) 안의 위치 */
    val props: List<Pair<Deco, Float>>,
    val specks: Pair<Color, Color>,
    val pebbles: Pair<Color, Color>,
    /** 바닥 소품과 레인마다 나올 확률 */
    val extras: List<Pair<Deco, Float>>,
    val snowy: Boolean = false,
    val prints: Boolean = false,
) {
    MEADOW(
        Venue.MEADOW, Color(0xFF8ED6FF), Color(0xFF8ED6FF), Color.White, Color.White, Color(0xFF7ED37A),
        Color(0xFFF2C98A), Color(0xFFEDC283), Color(0xFFE0AE6C), Color(0xFFE8D3B0),
        hills = Color(0xFF6CC46B) to Color(0xFF5DB65E),
        props = listOf(Deco.BUSH to 40f, Deco.BUSH to 260f),
        specks = Color(0xFFD9A866) to Color(0xFFE7BD7E), pebbles = Color(0xFFC9B79C) to Color(0xFFB9A88E),
        extras = listOf(Deco.TUFT to 0.6f), prints = true,
    ),
    BEACH(
        Venue.BEACH, Color(0xFF7FD3F7), Color(0xFFBDEBFF), Color(0xFFF4E2C4), Color(0xFFB07A45), Color(0xFFE9C88E),
        Color(0xFFF8E3AC), Color(0xFFF2D99A), Color(0xFFE4C27E), Color(0xFFFFF2CF),
        hills = null, sun = Color(0xFFFFD84D),
        props = listOf(Deco.PALM to 30f, Deco.PALM to 250f),
        specks = Color(0xFFE9CB86) to Color(0xFFFFF3D0), pebbles = Color(0xFFE8D9C2) to Color(0xFFD8C4A6),
        extras = listOf(Deco.SHELL to 0.9f, Deco.STAR to 0.5f),
    ),
    SNOW(
        Venue.SNOW, Color(0xFFA9D8F5), Color(0xFFE4F4FF), Color.White, Color(0xFF5B7FB8), Color.White,
        Color(0xFFF2F7FC), Color(0xFFE6EFF8), Color(0xFFC6D6EA), Color.White,
        hills = Color(0xFF9FB8D6) to Color(0xFF8AA8CC),
        props = listOf(Deco.PINE to 50f, Deco.PINE to 84f, Deco.SNOWMAN to 190f, Deco.PINE to 290f),
        specks = Color(0xFFD5E3F2) to Color.White, pebbles = Color.White to Color(0xFFF4F9FF),
        extras = listOf(Deco.TWIG to 0.5f), snowy = true,
    ),
    DESERT(
        Venue.DESERT, Color(0xFFFFB46B), Color(0xFFFFD9A0), Color(0xFFD99A5B), Color(0xFF8A5A2B), Color(0xFFE2A563),
        Color(0xFFEBAA72), Color(0xFFE29E66), Color(0xFFCB8150), Color(0xFFF6CFA2),
        hills = Color(0xFFE8955A) to Color(0xFFD98450), sun = Color(0xFFFFE38A),
        props = listOf(Deco.CACTUS to 120f, Deco.CACTUS to 320f),
        specks = Color(0xFFD88E58) to Color(0xFFF3C08C), pebbles = Color(0xFFB8875E) to Color(0xFFA9774F),
        extras = listOf(Deco.CRACK to 0.8f, Deco.WEED to 0.45f),
    );

    /** 레인별 바닥 무늬 (최대 10레인) */
    internal val ground: List<List<GroundItem>> by lazy { List(10) { laneGround(it, this) } }
}

/**
 * 소품 그림 (SVG 경로, 단위는 viewBox = [w]×[h]).
 * [d1]은 [fill]로 채우고 잉크 테두리, [d2]는 [detailFill]이 있으면 채우고 잉크 테두리, 없으면 [detail] 색 선으로 그린다.
 */
enum class Deco(
    val w: Float, val h: Float,
    d1: String, val fill: Color?,
    d2: String = "", val detailFill: Color? = null, val detail: Color = Pal.Ink,
) {
    PALM(44f, 76f, "M19 76 Q16 44 22 20 L27 20 Q23 44 26 76 Z", Color(0xFFB07A45),
        "M24 20 Q10 6 0 16 Q12 12 22 22 Q10 24 4 38 Q16 26 25 22 Q32 28 38 38 Q34 24 26 20 Q38 10 44 16 Q34 2 24 18 Z", Color(0xFF3FAE5A)),
    PINE(36f, 60f, "M18 0 L33 22 L25 22 L36 42 L21 42 L21 60 L15 60 L15 42 L0 42 L11 22 L3 22 Z", Color(0xFF3E8E5E),
        "M18 0 L25 10 Q18 14 11 10 Z", Color.White),
    CACTUS(32f, 56f, "M13 56 L13 7 Q16 0 19 7 L19 56 Z M13 30 L7 30 Q3 30 3 24 L3 14 Q6 9 9 14 L9 24 L13 24 Z M19 34 L25 34 L25 18 Q28 13 31 18 L31 34 Q31 40 25 40 L19 40 Z", Color(0xFF4FA35A)),
    BUSH(50f, 30f, "M2 30 Q0 16 12 14 Q14 2 26 6 Q36 0 42 12 Q52 14 48 30 Z", Color(0xFF5DB65E)),
    TUFT(22f, 16f, "M3 15 Q4 7 2 2 Q7 7 8 15 Q9 5 11 1 Q13 6 13 15 Q15 8 20 4 Q18 10 18 15 Z", Color(0xFF5DB65E)),
    SHELL(16f, 14f, "M8 13 L1 6 Q8 -2 15 6 Z", Color(0xFFFFB3A7), "M8 12 L5 4 M8 12 L8 2 M8 12 L11 4", detail = Color(0xFFC9707A)),
    STAR(16f, 16f, "M8 0 L10 6 L16 6 L11 10 L13 16 L8 12 L3 16 L5 10 L0 6 L6 6 Z", Color(0xFFFF9A5C)),
    TWIG(22f, 10f, "M1 7 L21 4", null, "M9 6 L13 1 M15 5 L18 9", detail = Color(0xFF6B4A2E)),
    CRACK(24f, 10f, "", null, "M0 5 L6 3 L10 7 L16 3 L24 6 M10 7 L12 10", detail = Color(0xFFB76A3E)),
    WEED(18f, 18f, "M9 1 A8 8 0 1 1 8.9 1 Z", Color(0xFFD9A35F), "M3 9 Q9 3 15 9 Q9 15 3 9 M9 2 Q12 9 9 16", detail = Color(0xFF8A5A2B)),

    // 배경 손님
    BUTTERFLY(22f, 16f, "M11 8 Q4 -2 1 4 Q0 10 11 9 Q22 10 21 4 Q18 -2 11 8 Z", Color(0xFFFFD84D), "M11 4 L11 14"),
    GULL(34f, 14f, "M1 10 Q9 0 17 9 Q25 0 33 10 Q25 5 17 12 Q9 5 1 10 Z", Color.White),
    /** 자리·크기만 쓰고 그림은 [snowmanGuest] (팔을 흔들어서 따로 그린다) */
    SNOWMAN(40f, 56f, "", null);

    val main: Path? = d1.takeIf { it.isNotEmpty() }?.let { PathParser().parsePathString(it).toPath() }
    val sub: Path? = d2.takeIf { it.isNotEmpty() }?.let { PathParser().parsePathString(it).toPath() }
}

/** [x],[y]를 왼쪽 위로, viewBox 1단위 = [unit]px 로 소품을 그린다. [stroke]는 viewBox 단위 선 굵기. */
private fun DrawScope.deco(d: Deco, x: Float, y: Float, unit: Float, stroke: Float = 2f) {
    withTransform({ translate(x, y); scale(unit, unit, Offset.Zero) }) {
        val line = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        d.main?.let { p ->
            d.fill?.let { drawPath(p, it) }
            drawPath(p, Pal.Ink, style = line)
        }
        d.sub?.let { p ->
            if (d.detailFill != null) {
                drawPath(p, d.detailFill)
                drawPath(p, Pal.Ink, style = line)
            } else {
                drawPath(p, d.detail, style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

// ---------------------------------------------------------------- 배경

/** 하늘(그라데이션)·해·눈·언덕 또는 바다·울타리 뒤 소품. 울타리는 [fence]에서. */
internal fun DrawScope.stadiumBackdrop(st: Stadium, top: Float, camPx: Float, time: Double) {
    val ink = Pal.Ink
    val line = 3.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(st.skyTop, st.skyBottom), endY = top), size = Size(size.width, top))

    st.sun?.let { c ->
        val r = (if (st == Stadium.DESERT) 36.dp else 24.dp).toPx()
        val center = Offset(size.width * (if (st == Stadium.DESERT) 0.16f else 0.8f), top - 72.dp.toPx())
        drawCircle(c, r, center)
        drawCircle(ink, r, center, style = Stroke(line))
    }

    // 구름 (아주 느리게)
    val cloudShift = (camPx * 0.08f) % (size.width + 200f)
    listOf(0.1f to 0.35f, 0.62f to 0.5f).forEach { (fx, fy) ->
        var cx = fx * size.width - cloudShift
        if (cx < -160f) cx += size.width + 200f
        drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(cx, top * fy), Size(120.dp.toPx(), 34.dp.toPx()),
            androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
    }

    // 내리는 눈
    if (st.snowy) {
        for (k in 0 until 28) {
            val fx = (rnd(k * 3.1) * (size.width + 30f) - (time * 25).toFloat()) % (size.width + 30f)
            val x = (if (fx < 0) fx + size.width + 30f else fx) - 15f
            val y = (rnd(k * 7.7) * top + (time * (34 + rnd(k.toDouble()) * 34)).toFloat()) % top
            drawCircle(Color.White, (1.5f + rnd(k * 1.9) * 2f) * density, Offset(x, y))
        }
    }

    val hills = st.hills
    if (hills != null) {
        // 언덕 (중간 속도). 설원은 꼭대기에 눈
        val hillW = 320.dp.toPx()
        val hillShift = (camPx * 0.35f) % hillW
        var hx = -hillShift - hillW
        var i = 0
        while (hx < size.width + hillW) {
            val hTop = top - (if (i % 2 == 0) 96.dp else 80.dp).toPx()
            val sz = Size(hillW, 220.dp.toPx())
            drawOval(if (i % 2 == 0) hills.first else hills.second, Offset(hx, hTop), sz)
            if (st.snowy) clipRect(top = hTop, bottom = hTop + 26.dp.toPx()) { drawOval(Color.White, Offset(hx, hTop), sz) }
            drawOval(ink, Offset(hx, hTop), sz, style = Stroke(line))
            hx += hillW * 0.62f; i++
        }
    } else {
        // 바다: 수평선 흰 물결 + 파란 바다
        val seaTop = top - 64.dp.toPx()
        drawRect(Color(0xFF3BB3E0), Offset(0f, seaTop), Size(size.width, top - seaTop))
        drawRect(Color.White, Offset(0f, seaTop), Size(size.width, 5.dp.toPx()))
    }

    guests(st, top, camPx, time)

    // 울타리 뒤 소품 (언덕보다 빠르고 트랙보다 느리게)
    val period = 470.dp.toPx()
    val shift = (camPx * 0.33f) % period
    val unit = density
    var base = -shift
    while (base < size.width) {
        for ((d, at) in st.props) {
            val x = base + at.dp.toPx()
            if (x + d.w * unit < 0 || x > size.width) continue
            val y = top - 16.dp.toPx() - d.h * unit
            if (d == Deco.SNOWMAN) snowmanGuest(x, y, unit, time) else deco(d, x, y, unit, stroke = 2.5f)
        }
        base += period
    }
}

/** 배경 손님: 순위와 상관없이 하늘·먼 곳을 지나간다 (나비·갈매기·모래 회오리). 설원 눈사람은 울타리 뒤 소품. */
private fun DrawScope.guests(st: Stadium, top: Float, camPx: Float, time: Double) {
    val span = size.width + 80.dp.toPx()
    fun loop(speedDp: Float, off: Float) = ((time * speedDp.dp.toPx() + off * span) % span).toFloat() - 40.dp.toPx()
    val tt = time.toFloat()
    when (st) {
        Stadium.MEADOW -> for (k in 0..1) {
            val x = loop(36f + k * 8, k * 0.47f)
            val y = top - (70 + k * 18).dp.toPx() + sin(tt * 3 + k) * 8.dp.toPx()
            deco(Deco.BUTTERFLY, x, y, density * 1.1f)
        }
        Stadium.BEACH -> for (k in 0..1) {
            val x = size.width - loop(48f, k * 0.12f)
            val y = top - (80 + k * 14).dp.toPx() + sin(tt * 1.6f + k * 2) * 4.dp.toPx()
            deco(Deco.GULL, x, y, density)
        }
        Stadium.DESERT -> {
            // 멀리서 느리게 흘러가는 모래 회오리 (언덕과 같은 원근)
            val far = size.width + 100.dp.toPx()
            val x = size.width + 20.dp.toPx() - ((camPx * 0.2f + tt * 14.dp.toPx() + size.width * 0.3f) % far)
            val wob = sin(tt * 2) * 3.dp.toPx()
            devilGuest(x + wob, top - 14.dp.toPx() - 80 * density, density, time)
        }
        Stadium.SNOW -> Unit
    }
}

/** 울타리(10m마다 기둥 + 가로 레일)와 그 아래 가장자리 띠. */
internal fun DrawScope.fence(st: Stadium, top: Float, camX: Double, pxPerM: Float) {
    val ink = Pal.Ink
    val line = 3.dp.toPx()
    var m = floor(camX / 10) * 10
    while (true) {
        val x = ((m - camX) * pxPerM).toFloat()
        if (x > size.width) break
        drawRect(st.post, Offset(x - 4.dp.toPx(), top - 30.dp.toPx()), Size(8.dp.toPx(), 30.dp.toPx()))
        drawRect(ink, Offset(x - 4.dp.toPx(), top - 30.dp.toPx()), Size(8.dp.toPx(), 30.dp.toPx()), style = Stroke(2.dp.toPx()))
        m += 10
    }
    // 기둥이 박혀 보이게 기둥 다음에
    drawRect(st.verge, Offset(0f, top - 12.dp.toPx()), Size(size.width, 12.dp.toPx()))
    drawRect(st.rail, Offset(0f, top - 22.dp.toPx()), Size(size.width, 10.dp.toPx()))
    drawLine(ink, Offset(0f, top - 22.dp.toPx()), Offset(size.width, top - 22.dp.toPx()), line)
    drawLine(ink, Offset(0f, top - 12.dp.toPx()), Offset(size.width, top - 12.dp.toPx()), line)
}

// ---------------------------------------------------------------- 바닥 디테일

internal enum class Ground { SPECK, PRINT, PEBBLE, ICE, DECO }

/** 바닥 무늬 하나. [x]는 한 주기 안의 위치(m), [y]는 레인 높이 대비 위치, 크기는 레인 높이 79dp 기준. */
internal class GroundItem(
    val kind: Ground, val x: Double, val y: Float,
    val w: Float = 0f, val h: Float = 0f, val color: Color = Color.Unspecified, val deco: Deco? = null,
)

/** 바닥 무늬가 반복되는 간격(m). 기본 화면 폭(130m)보다 약간 길게. */
private const val GROUND_PERIOD = 140.0

/** 결정적인 의사 난수: 같은 레인·같은 자리에는 언제나 같은 무늬가 나온다. */
private fun rnd(seed: Double): Float {
    val x = sin(seed * 127.1 + 311.7) * 43758.5453
    return (x - floor(x)).toFloat()
}

private fun rnd(seed: Int) = rnd(seed.toDouble())

private fun laneGround(lane: Int, st: Stadium): List<GroundItem> {
    val l = lane.toDouble()
    val p = GROUND_PERIOD
    return buildList {
        for (k in 0 until 14) {
            add(GroundItem(Ground.SPECK, rnd(l * 50 + k) * p, (8 + rnd(l * 70 + k) * 62) / 79,
                3 + rnd(k * 3 + l) * 3, 2 + rnd(k * 5 + l) * 2,
                if (rnd(k + l * 9) > 0.5f) st.specks.first else st.specks.second))
        }
        if (st.prints) for (k in 0 until 3) add(GroundItem(Ground.PRINT, rnd(l * 31 + k * 7) * p, (30 + rnd(l * 17 + k) * 30) / 79))
        if (st.snowy) for (k in 0 until 2) add(GroundItem(Ground.ICE, rnd(l * 41 + k * 9) * p, (20 + rnd(l * 19 + k) * 40) / 79, 34f, 10f))
        for (k in 0 until 4) {
            val w = if (k == 0) 18f else 8 + rnd(l * 13 + k) * 6
            add(GroundItem(Ground.PEBBLE, rnd(l * 91 + k * 5) * p, (12 + rnd(l * 23 + k * 3) * 52) / 79,
                w, if (st.snowy) w else w * 0.68f, if (rnd(l + k * 11) > 0.5f) st.pebbles.first else st.pebbles.second))
        }
        st.extras.forEachIndexed { j, (d, chance) ->
            if (rnd(l * 3.3 + j * 7) < chance) {
                val y = if (d == Deco.TUFT) 2f else 14 + rnd(l * 29 + j) * 44
                add(GroundItem(Ground.DECO, rnd(l * 77 + j * 13) * p, y / 79, deco = d))
            }
        }
    }
}

/** 레인 바닥의 알갱이·발자국·돌·소품. 트랙(월드 좌표)에 붙어 있어 동물과 같은 속도로 흘러간다. */
internal fun DrawScope.ground(st: Stadium, lane: Int, laneTop: Float, lh: Float, camX: Double, camW: Double, pxPerM: Float) {
    val s = (lh / 79.dp.toPx()).coerceIn(0.55f, 1.2f) * density
    val ink = Pal.Ink
    val items = st.ground[lane % st.ground.size]
    // 촘촘히(속도감, 4.12): 같은 무늬를 반 간격으로 두 번 깔아 흘러가는 게 잘 보이게
    val period = GROUND_PERIOD / 2
    val k = 0.5
    var base = floor(camX / period) * period
    while (base < camX + camW) {
        for (g in items) {
            val x = ((base + g.x * k - camX) * pxPerM).toFloat()
            if (x < -36 * s || x > size.width) continue
            val y = laneTop + g.y * lh
            when (g.kind) {
                Ground.SPECK -> drawOval(g.color, Offset(x, y), Size(g.w * s, g.h * s))
                Ground.ICE -> {
                    drawOval(Color(0xFFCFE8F8), Offset(x, y), Size(g.w * s, g.h * s))
                    drawLine(Color.White, Offset(x + 8 * s, y + 3 * s), Offset(x + 18 * s, y + 3 * s), 2 * s)
                }
                Ground.PRINT -> for ((dx, dy) in listOf(0f to 0f, 14f to 8f)) {
                    val o = Offset(x + dx * s, y + dy * s)
                    rotate(-12f, o + Offset(4.5f * s, 3f * s)) { drawOval(Color(0xFFDDAE72), o, Size(9 * s, 6 * s)) }
                }
                Ground.PEBBLE -> {
                    val sz = Size(g.w * s, g.h * s)
                    drawOval(g.color, Offset(x, y), sz)
                    drawOval(ink, Offset(x, y), sz, style = Stroke(2 * s))
                    drawOval(Color.White.copy(alpha = 0.55f), Offset(x + sz.width * 0.22f, y + sz.height * 0.18f),
                        Size(sz.width * 0.3f, sz.height * 0.26f))
                }
                Ground.DECO -> deco(g.deco!!, x, y, s)
            }
        }
        base += period
    }
}
