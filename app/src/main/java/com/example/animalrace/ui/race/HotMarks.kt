package com.example.animalrace.ui.race

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.animalrace.ui.theme.Pal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 머리 위 표시: 당첨 자리면 폭탄, 당첨이 절반을 넘는 판은 거꾸로 안전한 자리에 네잎클로버 (5.4) */
internal enum class HotMark { BOMB, CLOVER }

/**
 * 지금 등수([rank], 0부터)에 붙일 표시. 꼴찌 판([picks] null)은 꼴찌 자리에 폭탄.
 * 표시만 하고 경주에는 아무 영향이 없다.
 */
internal fun hotMark(rank: Int, players: Int, picks: List<Int>?): HotMark? {
    val hot = picks ?: listOf(players)
    return if (hot.size * 2 <= players) HotMark.BOMB.takeIf { rank + 1 in hot }
    else HotMark.CLOVER.takeIf { rank + 1 !in hot }
}

private val Fuse = Color(0xFF8A6A3F)
private val Spark = Color(0xFFFFC93C)
private val Flame = Color(0xFFFF8A3C)

/**
 * 만화 폭탄 (가로 [width]). [urgency](0~1)가 오를수록 불붙은 심지가 타들어 가 짧아지고 불꽃이 빨리 튄다.
 * 0.5부터 부르르 떨고, 0.7부터 몸통이 빨갛게 번쩍이며 연기가 피어오른다 (곧 터질 것처럼).
 */
@Composable
internal fun Bomb(modifier: Modifier = Modifier, width: Dp = 30.dp, time: Float, urgency: Float) {
    Canvas(modifier.size(width, width * 1.15f)) {
        val u = size.width / 30f
        val shake = if (urgency > 0.5f) sin(time * 60f) * 6f * (urgency - 0.5f) else 0f
        rotate(shake, pivot = Offset(15f * u, 22f * u)) {
            val c = Offset(14f * u, 22f * u)
            val r = 11f * u
            // 심지: 마개(19,10)에서 끝(26,5)까지의 곡선 중 아직 안 탄 부분만. 급할수록 짧아진다
            val burn = 1f - 0.8f * urgency
            fun fusePt(t: Float): Offset {
                val a = 1 - t
                return Offset((a * a * 19f + 2 * a * t * 22f + t * t * 26f) * u, (a * a * 10f + 2 * a * t * 4f + t * t * 5f) * u)
            }
            val fuse = Path().apply {
                val p0 = fusePt(0f); moveTo(p0.x, p0.y)
                for (k in 1..12) { val p = fusePt(burn * k / 12); lineTo(p.x, p.y) }
            }
            drawPath(fuse, Pal.Ink, style = Stroke(4.2f * u, cap = StrokeCap.Round))
            drawPath(fuse, Fuse, style = Stroke(2f * u, cap = StrokeCap.Round))
            val tip = fusePt(burn)
            // 연기: 불꽃에서 모락모락 (0.7부터)
            if (urgency > 0.7f) for (k in 0 until 3) {
                val ph = (time * 1.6f + k / 3f) % 1f
                drawCircle(Color(0xFF9A93A8), (1.5f + 2.5f * ph) * u, tip + Offset((2f + 3f * ph) * u, -9f * ph * u), alpha = (1f - ph) * 0.7f)
            }
            // 몸통 + 마개: 0.7부터 빨갛게 번쩍 (급할수록 빨리)
            val flash = if (urgency > 0.7f) ((sin(time * (8f + 30f * (urgency - 0.7f))) + 1f) / 2f) * ((urgency - 0.7f) / 0.3f) else 0f
            drawCircle(Pal.Ink, r + 1.4f * u, c)
            drawCircle(lerp(Color(0xFF3A3150), Pal.Tomato, flash), r, c)
            drawRoundRect(Pal.Ink, Offset(14.5f * u, 8.5f * u), Size(7f * u, 5.5f * u), androidx.compose.ui.geometry.CornerRadius(1.5f * u))
            drawRoundRect(Color(0xFF6A5F80), Offset(15.6f * u, 9.6f * u), Size(4.8f * u, 3.3f * u), androidx.compose.ui.geometry.CornerRadius(1f * u))
            // 반짝 하이라이트
            drawCircle(Color.White.copy(alpha = 0.55f), 3f * u, Offset(10f * u, 18f * u))
            drawCircle(Color.White.copy(alpha = 0.35f), 1.4f * u, Offset(8f * u, 22.5f * u))
            // 불꽃: 급할수록 빨리 깜빡
            val beat = ((time * (6f + urgency * 14f)) % 1f)
            val s = (if (beat < 0.5f) 1f else 0.7f) * u
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4 + time * 3f
                val len = (if (k % 2 == 0) 5.5f else 3.5f) * s
                drawLine(if (k % 2 == 0) Flame else Spark, tip, tip + Offset(cos(a) * len, sin(a) * len), 1.8f * u, StrokeCap.Round)
            }
            drawCircle(Spark, 2.2f * s, tip)
            drawCircle(Color.White, 1f * s, tip)
        }
    }
}

/** 골인하며 터지는 "펑!" 폭발 ([t]초 동안 커졌다 사라짐, 0.7초) */
@Composable
internal fun Boom(modifier: Modifier = Modifier, size: Dp = 64.dp, t: Float) {
    if (t !in 0f..0.7f) return
    Canvas(modifier.size(size)) {
        val k = t / 0.7f
        val grow = 0.5f + 0.7f * (1 - (1 - k) * (1 - k))
        val alpha = (1f - k).coerceIn(0f, 1f)
        val c = center
        val r = this.size.minDimension / 2 * grow
        val star = Path()
        for (i in 0 until 20) {
            val a = i * PI.toFloat() / 10 - PI.toFloat() / 2
            val rr = if (i % 2 == 0) r else r * 0.62f
            val p = c + Offset(cos(a) * rr, sin(a) * rr)
            if (i == 0) star.moveTo(p.x, p.y) else star.lineTo(p.x, p.y)
        }
        star.close()
        drawPath(star, Flame, alpha = alpha)
        drawPath(star, Pal.Ink, alpha = alpha, style = Stroke(2.5.dp.toPx()))
        drawCircle(Spark, r * 0.45f, c, alpha = alpha)
        drawCircle(Color.White, r * 0.2f, c, alpha = alpha)
    }
}

/** 네잎클로버 (가로 [width]) */
@Composable
internal fun Clover(modifier: Modifier = Modifier, width: Dp = 28.dp, time: Float) {
    Canvas(modifier.size(width, width * 1.1f)) {
        val u = size.width / 28f
        rotate(sin(time * 3f) * 8f, pivot = Offset(14f * u, 14f * u)) {
            val c = Offset(14f * u, 13f * u)
            drawLine(Pal.Ink, c, Offset(17f * u, 29f * u), 3.6f * u, StrokeCap.Round)
            drawLine(Color(0xFF3E8E4A), c, Offset(17f * u, 29f * u), 1.8f * u, StrokeCap.Round)
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2 + PI.toFloat() / 4
                val leaf = c + Offset(cos(a) * 6f * u, sin(a) * 6f * u)
                drawCircle(Pal.Ink, 6.6f * u, leaf)
            }
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2 + PI.toFloat() / 4
                val leaf = c + Offset(cos(a) * 6f * u, sin(a) * 6f * u)
                drawCircle(Color(0xFF6BCB77), 5.2f * u, leaf)
                drawCircle(Color.White.copy(alpha = 0.35f), 1.8f * u, leaf + Offset(-1.5f * u, -1.5f * u))
            }
            drawCircle(Color(0xFF3E8E4A), 2f * u, c)
        }
    }
}
