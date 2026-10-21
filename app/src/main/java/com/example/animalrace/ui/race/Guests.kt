package com.example.animalrace.ui.race

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.example.animalrace.ui.theme.Pal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/*
 * 움직이는 배경 손님: 설원 눈사람(팔 흔들기), 사막 모래 회오리.
 * 시안: 캔버스 "배경 손님 개선" 보드의 개선 B (Guests.dc.html). 시안의 60ms 단위 시간을 그대로 쓴다.
 */

private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

private fun DrawScope.fillLine(path: Path, fill: Color?, line: Float, lineColor: Color = Pal.Ink, alpha: Float = 1f) {
    if (fill != null) drawPath(path, fill, alpha = alpha)
    if (line > 0f) drawPath(path, lineColor, alpha = alpha, style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ---------------------------------------------------------------- 눈사람 (viewBox 40×56)

private val ArmLeft = p("M9 35 L2 28 M5 31 L2 33")
private val ArmRight = p("M31 35 L38 27 M35 31 L38 33")
private val Shade = p("M10 48 Q20 52 30 48")
private val Scarf = p("M11 29 Q20 34 29 29 L29.5 33 Q20 38 10.5 33 Z")
private val ScarfTail = p("M23 33 L27 45 L22.5 45 L20 34 Z")
private val ScarfStripes = p("M15 30.5 L15 34.5 M20 31.5 L20 36 M25 30.5 L25 34.5 M22.5 39 L25.5 39 M23.5 42 L26.5 42")
private val Mouth = p("M16 26 Q20 28.5 24 26")
private val Carrot = p("M19.5 22.5 L30 24.5 Q30.6 25.3 30 25.8 L19.5 25.6 Z")
private val CarrotLines = p("M23 23.3 L24 24.6 M26 24 L26.8 25.1")
private val Hat = p("M10.5 17 Q10.5 6 20 6 Q29.5 6 29.5 17 Z")
private val HatBand = p("M9.5 15 L30.5 15 Q31 17 30.5 19 L9.5 19 Q9 17 9.5 15 Z")
private val HatRibs = p("M14 9 L14 14 M18 7.5 L18 14 M22 7.5 L22 14 M26 9 L26 14")
private val Twig = Color(0xFF6B4A2E)
private val ScarfRed = Color(0xFFE5484D)

/** 털모자·당근 코·목도리 눈사람. ([x],[y])는 왼쪽 위, 1단위 = [unit]px (40×56). 오른팔을 흔든다. */
internal fun DrawScope.snowmanGuest(x: Float, y: Float, unit: Float, time: Double) {
    val t = (time / 0.06).toFloat()
    val wave = sin(t / 4) * 14
    val ink = Pal.Ink
    withTransform({ translate(x, y); scale(unit, unit, Offset.Zero) }) {
        fillLine(ArmLeft, null, 2.2f, Twig)
        rotate(wave, Offset(30.8f, 34.7f)) { fillLine(ArmRight, null, 2.2f, Twig) }
        for ((cy, r) in listOf(42f to 13f, 22f to 9.5f)) {
            drawCircle(Color.White, r, Offset(20f, cy))
            drawCircle(ink, r, Offset(20f, cy), style = Stroke(2.5f))
        }
        fillLine(Shade, null, 2f, Color(0xFFBFD9EE))
        drawCircle(ink, 1.4f, Offset(20f, 38f)); drawCircle(ink, 1.4f, Offset(20f, 44f))
        fillLine(Scarf, ScarfRed, 2f); fillLine(ScarfTail, ScarfRed, 2f)
        fillLine(ScarfStripes, null, 1.4f, Color.White)
        drawCircle(ink, 1.4f, Offset(16.6f, 20.5f)); drawCircle(ink, 1.4f, Offset(23.4f, 20.5f))
        fillLine(Mouth, null, 1.4f)
        fillLine(Carrot, Color(0xFFFF8A3D), 1.6f)
        fillLine(CarrotLines, null, 1f, Color(0xFFC9601E))
        drawCircle(Color(0xFFFF9DB5), 1.8f, Offset(14f, 24.5f), alpha = 0.85f)
        drawCircle(Color(0xFFFF9DB5), 1.8f, Offset(26.5f, 24f), alpha = 0.85f)
        fillLine(Hat, Color(0xFF5B7FB8), 2f)
        fillLine(HatBand, Color.White, 2f)
        fillLine(HatRibs, null, 1.2f, Color(0xFF456A9E))
        drawCircle(Color.White, 3f, Offset(20f, 5f)); drawCircle(ink, 3f, Offset(20f, 5f), style = Stroke(2f))
    }
}

// ---------------------------------------------------------------- 모래 회오리 (viewBox 60×80)

private val DevilCloud = p("M4 12 Q0 6 7 5 Q10 0 18 2 Q24 -2 30 1 Q37 -2 43 2 Q51 0 53 5 Q60 6 56 12 Q52 16 43 14 Q37 18 30 15 Q22 18 15 14 Q7 17 4 12 Z")
private val StreakColors = listOf(Color(0xFF8E5428), Color(0xFFF0C48E), Color(0xFFA86A35), Color(0xFFF4D2A2))

private fun Path.smoothThrough(pts: List<Offset>, move: Boolean) {
    if (move) moveTo(pts[0].x, pts[0].y) else lineTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size - 1) {
        val m = (pts[i] + pts[i + 1]) / 2f
        quadraticTo(pts[i].x, pts[i].y, m.x, m.y)
    }
    lineTo(pts.last().x, pts.last().y)
}

/** 멀리 지나가는 작은 모래 토네이도. ([x],[y])는 왼쪽 위, 1단위 = [unit]px (60×80). */
internal fun DrawScope.devilGuest(x: Float, y: Float, unit: Float, time: Double, alpha: Float = 0.92f) {
    val t = (time / 0.06).toFloat()
    val n = 12
    val lv = List(n) { i ->
        val f = i / (n - 1f)
        val w = (22 * (1 - f).pow(1.3f) + 2) * (1 + 0.1f * sin(i * 1.7f + t / 2))
        Triple(30 + sin(t / 5 + f * 2.4f) * (1 + f * 7), 10 + f * 62, w)
    }
    val funnel = Path().apply {
        smoothThrough(lv.map { Offset(it.first - it.third, it.second) }, move = true)
        smoothThrough(lv.map { Offset(it.first + it.third, it.second) }.reversed(), move = false)
        close()
    }
    val g = sin(t / 3) * 1.5f
    val base = p("M2 79 Q0 72 8 71 Q11 ${64 + g} 20 66 Q27 ${60 - g} 34 64 Q42 ${61 + g} 49 67 Q58 67 58 74 Q62 76 59 79 Z")
    withTransform({ translate(x, y); scale(unit, unit, Offset.Zero) }) {
        fillLine(funnel, Color(0xFFD69A5C), 1.6f, alpha = alpha)
        fillLine(DevilCloud, Color(0xFFE2AE72), 1.6f, alpha = alpha)
        // 소용돌이 줄: 깔때기를 비스듬히 감싸며 돈다
        for (j in 0 until 7) {
            val fy = 0.06f + j * 0.13f
            val k = min(n - 1.001f, fy * (n - 1))
            val i = floor(k).toInt()
            val r = k - i
            val a = lv[i]; val b = lv[i + 1]
            val cx = a.first + (b.first - a.first) * r
            val cy = a.second + (b.second - a.second) * r
            val w = (a.third + (b.third - a.third) * r) * 1.1f
            val s0 = (t * 0.06f + j * 0.37f) % 1f
            val len = 0.45f + (j % 3) * 0.18f
            val pts = mutableListOf<Offset>()
            for (q in 0..6) {
                val u = s0 + q / 6f * len
                if (u > 1f) break
                pts += Offset(cx - w + 2 * w * u, cy + sin(u * PI.toFloat()) * 4 - u * 4)
            }
            if (pts.size >= 3) fillLine(Path().apply { smoothThrough(pts, move = true) }, null, 2f, StreakColors[j % 4], alpha)
        }
        fillLine(base, Color(0xFFE8BC88), 1.6f, alpha = alpha)
        // 휘말린 모래알
        for (m in 0 until 5) {
            val ang = t / 3 + m * 1.2f
            val l = lv[2 + m * 2]
            drawCircle(Color(0xFF8E5428), 1.4f, Offset(l.first + cos(ang) * l.third * 1.35f, l.second),
                alpha = alpha * if (sin(ang) > 0) 1f else 0.35f)
        }
    }
}
